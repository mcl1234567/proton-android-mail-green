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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.AvatarUiModel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAvatarUiModelTest {

    private val getAvatarUiModel = GetAvatarUiModel()

    @Test
    fun `avatar should show draft icon for all drafts in message mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllDrafts.labelId)
        )
        val expectedResult = AvatarUiModel(
            participantInitial = null,
            shouldShowDraftIcon = true
        )

        // When
        val result = getAvatarUiModel(mailboxItem)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show first letter of recipient for all sent messages in message mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllSent.labelId),
            recipients = listOf(Recipient(address = "test@protonmail.com", name = ""))
        )
        val expectedResult = AvatarUiModel(
            participantInitial = 'T',
            shouldShowDraftIcon = false
        )

        // When
        val result = getAvatarUiModel(mailboxItem)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show first letter of sender for mailbox items in conversation mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Conversation,
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = listOf(Recipient(address = "test@protonmail.com", name = "Name"))
        )
        val expectedResult = AvatarUiModel(
            participantInitial = 'N',
            shouldShowDraftIcon = false
        )

        // When
        val result = getAvatarUiModel(mailboxItem)

        // Then
        assertEquals(expectedResult, result)
    }
}
