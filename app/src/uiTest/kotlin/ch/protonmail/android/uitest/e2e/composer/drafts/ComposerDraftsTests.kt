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

package ch.protonmail.android.uitest.e2e.composer.drafts

import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.emptyListSection
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot

internal interface ComposerDraftsTests : ComposerTests {

    fun verifyEmptyDrafts() {
        menuRobot {
            swipeOpenSidebarMenu()
            openDrafts()
        }

        mailboxRobot {
            topAppBarSection { verify { isMailbox(MailboxType.Drafts) } }
            emptyListSection { verify { isShown() } }
        }
    }

    fun verifyDraftCreation(
        vararg expectedRecipients: ParticipantEntry,
        subject: String = "",
        body: String = ""
    ) {
        val expectedDraftItem = MailboxListItemEntry(
            index = 0,
            avatarInitial = AvatarInitial.Draft,
            participants = expectedRecipients.toList(),
            date = "Jul 1, 2023",
            subject = subject
        )

        mailboxRobot {
            snackbarSection { verify { hasSuccessMessage(SnackbarTextEntry.DraftSaved) } }
        }

        menuRobot {
            swipeOpenSidebarMenu()
            openDrafts()
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(expectedDraftItem) }
            }
        }
    }
}
