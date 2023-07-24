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

package ch.protonmail.android.composer.data.remote

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftRepositoryImplTest {

    private val messageRepository = mockk<MessageRepository>()
    private val draftRemoteDataSource = mockk<DraftRemoteDataSourceImpl>()

    private val draftRepository = DraftRepositoryImpl(
        messageRepository,
        draftRemoteDataSource
    )

    @Test
    fun `returns success when remote data source succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val userId = UserIdSample.Primary
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedResponse = MessageWithBodySample.EmptyDraft
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceSuccess(userId, expectedDraft, expectedAction, expectedResponse)

        // When
        val actual = draftRepository.create(userId, messageId)

        // Then
        assertEquals(expectedResponse.right(), actual)
    }

    @Test
    fun `returns local failure when reading the message from DB fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val userId = UserIdSample.Primary
        val expectedError = DataError.Local.NoDataCached
        expectGetLocalMessageFails(userId, messageId, expectedError)

        // When
        val actual = draftRepository.create(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `returns remote failure when remote data source fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val userId = UserIdSample.Primary
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedError = DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceFailure(userId, expectedDraft, expectedAction, expectedError)

        // When
        val actual = draftRepository.create(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    private fun expectRemoteDataSourceSuccess(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        response: MessageWithBody
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns response.right()
    }

    private fun expectRemoteDataSourceFailure(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        error: DataError.Remote
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns error.left()
    }

    private fun expectGetLocalMessageSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedMessage: MessageWithBody
    ) {
        every { messageRepository.observeMessageWithBody(userId, messageId) } returns flowOf(expectedMessage.right())
    }

    private fun expectGetLocalMessageFails(
        userId: UserId,
        messageId: MessageId,
        error: DataError.Local
    ) {
        every { messageRepository.observeMessageWithBody(userId, messageId) } returns flowOf(error.left())
    }
}
