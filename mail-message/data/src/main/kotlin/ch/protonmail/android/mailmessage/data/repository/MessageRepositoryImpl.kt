/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val remoteDataSource: MessageRemoteDataSource,
    private val localDataSource: MessageLocalDataSource,
    coroutineScopeProvider: CoroutineScopeProvider
) : MessageRepository {

    private data class MessageKey(val userId: UserId, val messageId: MessageId)

    private val messageWithBodyStore: ProtonStore<MessageKey, MessageWithBody> = StoreBuilder.from(
        fetcher = Fetcher.of { key: MessageKey ->
            remoteDataSource.getMessage(key.userId, key.messageId)
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: MessageKey ->
                localDataSource.observeMessageWithBody(key.userId, key.messageId)
            },
            writer = { key, messageWithBody ->
                localDataSource.upsertMessageWithBody(key.userId, messageWithBody)
            }
        )
    ).scope(coroutineScopeProvider.GlobalIOSupervisedScope).buildProtonStore()

    override suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError, List<Message>> = localDataSource.getMessages(
        userId = userId,
        pageKey = pageKey
    ).let { cachedMessages ->
        val isLocalPageValid = localDataSource.isLocalPageValid(userId, pageKey, cachedMessages)
        if (isLocalPageValid) cachedMessages.right()
        else fetchMessages(userId, pageKey).fold(
            ifLeft = { dataError ->
                Timber.w("Failed to fetch messages from remote, returning cached messages. $dataError")
                cachedMessages.right()
            },
            ifRight = { it.right() }
        )
    }

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    ) = localDataSource.markAsStale(userId, labelId)

    override fun observeCachedMessage(
        userId: UserId,
        messageId: MessageId
    ): Flow<Either<DataError.Local, Message>> = localDataSource.observeMessage(userId, messageId).mapLatest {
        it?.right() ?: DataError.Local.NoDataCached.left()
    }

    override fun observeCachedMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Flow<Either<DataError.Local, NonEmptyList<Message>>> =
        localDataSource.observeMessages(userId, conversationId).mapLatest { list ->
            list.toNonEmptyListOrNull()?.right() ?: DataError.Local.NoDataCached.left()
        }

    override fun observeMessageWithBody(
        userId: UserId,
        messageId: MessageId
    ): Flow<Either<DataError, MessageWithBody>> =
        messageWithBodyStore.stream(
            StoreRequest.cached(MessageKey(userId, messageId), false)
        ).mapLatest { it.toDataResult() }
            .mapToEither()
            .distinctUntilChanged()

    override suspend fun getMessageWithBody(
        userId: UserId,
        messageId: MessageId
    ): Either<DataError, MessageWithBody> = observeMessageWithBody(userId, messageId).first()

    override suspend fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> {
        val messageEither = localDataSource.addLabel(userId, messageId, labelId)
        return messageEither.tap {
            remoteDataSource.addLabel(userId, messageId, labelId)
        }
    }

    override suspend fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> {
        val messageEither = localDataSource.removeLabel(userId, messageId, labelId)

        return messageEither.tap {
            remoteDataSource.removeLabel(userId, messageId, labelId)
        }
    }

    override suspend fun moveTo(
        userId: UserId,
        messageId: MessageId,
        fromLabel: LabelId?,
        toLabel: LabelId
    ): Either<DataError.Local, Message> {
        if (toLabel == SystemLabelId.Trash.labelId) {
            return moveToTrash(userId, messageId)
        }

        val message = localDataSource.observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()

        val updatedLabels = message.labelIds.toMutableList().apply {
            fromLabel?.let { this.remove(it) }
            this.add(toLabel)
        }

        val updatedMessage = message.copy(labelIds = updatedLabels)

        localDataSource.upsertMessage(updatedMessage)
        remoteDataSource.addLabel(userId, messageId, toLabel)
        return updatedMessage.right()
    }

    override suspend fun markUnread(userId: UserId, messageId: MessageId): Either<DataError.Local, Message> =
        localDataSource.markUnread(userId, messageId)

    private suspend fun moveToTrash(
        userId: UserId,
        messageId: MessageId
    ): Either<DataError.Local, Message> {
        val message = localDataSource.observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessage = run {
            val persistentLabels = listOf(
                SystemLabelId.AllDrafts.labelId,
                SystemLabelId.AllMail.labelId,
                SystemLabelId.AllSent.labelId
            )
            message.copy(labelIds = message.labelIds.filter { labelId -> labelId in persistentLabels })
        }
        localDataSource.upsertMessage(updatedMessage)
        return addLabel(userId = userId, messageId = messageId, labelId = SystemLabelId.Trash.labelId)
    }

    private suspend fun fetchMessages(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<Message>> = localDataSource.getClippedPageKey(
        userId = userId,
        pageKey = pageKey.copy(size = min(MessageApi.maxPageSize, pageKey.size))
    ).let { adaptedPageKey ->
        remoteDataSource.getMessages(
            userId = userId,
            pageKey = adaptedPageKey
        ).tap { messages -> insertMessages(userId, adaptedPageKey, messages) }
    }

    private suspend fun insertMessages(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>
    ) = localDataSource.upsertMessages(
        userId = userId,
        pageKey = pageKey,
        items = messages
    )
}
