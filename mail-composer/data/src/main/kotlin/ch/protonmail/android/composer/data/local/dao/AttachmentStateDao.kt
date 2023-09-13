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

package ch.protonmail.android.composer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.composer.data.local.entity.AttachmentStateEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class AttachmentStateDao : BaseDao<AttachmentStateEntity>() {

    @Query(
        """
        SELECT * FROM AttachmentStateEntity
        WHERE userId = :userId 
        AND messageId = :messageId
        AND attachmentId = :attachmentId
        OR apiAttachmentId = :attachmentId
    """
    )
    abstract suspend fun getAttachmentState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): AttachmentStateEntity?
}
