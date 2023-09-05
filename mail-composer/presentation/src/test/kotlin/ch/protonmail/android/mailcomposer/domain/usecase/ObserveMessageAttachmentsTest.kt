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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test

class ObserveMessageAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice

    private val messageRepository = mockk<MessageRepository> {
        every { observeMessageAttachments(userId, messageId) } returns flowOf(listOf(MessageAttachmentTestData.invoice))
    }

    private val observeMessageAttachments = ObserveMessageAttachments(messageRepository)

    @Test
    fun `should call the repository method to observe message attachments`() {
        // When
        observeMessageAttachments(userId, messageId)

        // Then
        verify { messageRepository.observeMessageAttachments(userId, messageId) }
    }
}
