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

package ch.protonmail.android.mailmessage.data.local

import java.io.File
import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AttachmentFileStorage @Inject constructor(
    private val internalFileStorage: InternalFileStorage
) {

    suspend fun saveAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String,
        content: ByteArray
    ): File? {
        return internalFileStorage.writeFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId),
            content
        )
    }

    @Throws(AttachmentFileReadException::class)
    suspend fun readAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String
    ): File {
        return internalFileStorage.getFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId)
        ) ?: throw AttachmentFileReadException
    }

    suspend fun deleteAttachmentsOfMessage(userId: UserId, messageId: String): Boolean =
        internalFileStorage.deleteFolder(userId, InternalFileStorage.Folder.MessageAttachments(messageId))
}

object AttachmentFileReadException : RuntimeException()