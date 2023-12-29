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

package ch.protonmail.android.navigation.model

import ch.protonmail.android.feature.account.SignOutAccountDialog.USER_ID_KEY
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.DraftMessageIdKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.SerializedDraftActionKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ConversationIdKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ScrollToMessageIdKey
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen.MESSAGE_ID_KEY
import ch.protonmail.android.maillabel.presentation.folderform.FolderFormScreen.FolderFormLabelIdKey
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen.ParentFolderListLabelIdKey
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen.ParentFolderListParentLabelIdKey
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormScreen.LabelFormLabelIdKey
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin.AutoLockPinScreen.AutoLockPinModeKey
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.serialize

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object Conversation : Destination(
            "mailbox/conversation/${ConversationIdKey.wrap()}/${ScrollToMessageIdKey.wrap()}"
        ) {
            operator fun invoke(conversationId: ConversationId, scrollToMessageId: MessageId? = null) =
                route.replace(ConversationIdKey.wrap(), conversationId.id).replace(
                    ScrollToMessageIdKey.wrap(), scrollToMessageId?.id ?: "null"
                )
        }

        object Message : Destination("mailbox/message/${MESSAGE_ID_KEY.wrap()}") {

            operator fun invoke(messageId: MessageId) = route.replace(MESSAGE_ID_KEY.wrap(), messageId.id)
        }

        object Composer : Destination("composer")

        object EditDraftComposer : Destination("composer/${DraftMessageIdKey.wrap()}") {

            operator fun invoke(messageId: MessageId) = route.replace(DraftMessageIdKey.wrap(), messageId.id)
        }

        object MessageActionComposer : Destination("composer/action/${SerializedDraftActionKey.wrap()}") {

            operator fun invoke(action: DraftAction) =
                route.replace(SerializedDraftActionKey.wrap(), action.serialize())
        }

        object Settings : Destination("settings")
        object AccountSettings : Destination("settings/account")
        object AlternativeRoutingSettings : Destination("settings/alternativeRouting")
        object AutoLockSettings : Destination("settings/autolock")
        object AutoLockPinSettings : Destination("settings/autolock/pin/${AutoLockPinModeKey.wrap()}") {

            operator fun invoke(mode: AutoLockInsertionMode) =
                route.replace(AutoLockPinModeKey.wrap(), mode.serialize())
        }

        object CombinedContactsSettings : Destination("settings/combinedContacts")
        object ConversationModeSettings : Destination("settings/account/conversationMode")
        object DefaultEmailSettings : Destination("settings/account/defaultEmail")
        object DisplayNameSettings : Destination("settings/account/displayName")
        object PrivacySettings : Destination("settings/account/privacy")
        object LanguageSettings : Destination("settings/appLanguage")
        object SwipeActionsSettings : Destination("settings/swipeActions")
        object EditSwipeActionSettings : Destination("settings/swipeActions/edit/${SWIPE_DIRECTION_KEY.wrap()}") {

            operator fun invoke(direction: SwipeActionDirection) =
                route.replace(SWIPE_DIRECTION_KEY.wrap(), direction.name)
        }

        object ThemeSettings : Destination("settings/theme")
        object Notifications : Destination("settings/notifications")
        object DeepLinksHandler : Destination("deepLinksHandler")
        object LabelList : Destination("labelList")
        object CreateLabel : Destination("labelForm")
        object EditLabel : Destination("labelForm/${LabelFormLabelIdKey.wrap()}") {

            operator fun invoke(labelId: LabelId) = route.replace(LabelFormLabelIdKey.wrap(), labelId.id)
        }

        object FolderList : Destination("folderList")
        object CreateFolder : Destination("folderForm")
        object EditFolder : Destination("folderForm/${FolderFormLabelIdKey.wrap()}") {

            operator fun invoke(labelId: LabelId) = route.replace(FolderFormLabelIdKey.wrap(), labelId.id)
        }

        object ParentFolderList : Destination(
            "parentFolderList/${ParentFolderListLabelIdKey.wrap()}/${ParentFolderListParentLabelIdKey.wrap()}"
        ) {

            operator fun invoke(labelId: LabelId?, parentLabelId: LabelId?) = run {
                route.replace(
                    ParentFolderListLabelIdKey.wrap(), labelId?.id ?: "null"
                ).replace(
                    ParentFolderListParentLabelIdKey.wrap(), parentLabelId?.id ?: "null"
                )
            }
        }

        object Contacts : Destination("contacts")
    }

    object Dialog {
        object SignOut : Destination("signout/${USER_ID_KEY.wrap()}") {

            operator fun invoke(userId: UserId?) = route.replace(USER_ID_KEY.wrap(), userId?.id ?: " ")
        }

        object RemoveAccount : Destination("remove/${USER_ID_KEY.wrap()}") {

            operator fun invoke(userId: UserId?) = route.replace(USER_ID_KEY.wrap(), userId?.id ?: " ")
        }
    }
}

/**
 * Wrap a key in the format required by the Navigation framework: `{key_name}`
 */
private fun String.wrap() = "{$this}"
