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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.model.BottomBarState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import javax.inject.Inject

class BottomBarStateReducer @Inject constructor() {

    fun reduce(currentState: BottomBarState, event: ConversationDetailEvent): BottomBarState {
        return when (event) {
            is ConversationDetailEvent.ConversationActionsData -> BottomBarState.Data(event.actionUiModels)
            is ConversationDetailEvent.ErrorLoadingActions -> BottomBarState.Error.FailedLoadingActions
            is ConversationDetailEvent.ConversationData,
            is ConversationDetailEvent.ErrorLoadingConversation,
            is ConversationDetailEvent.NoPrimaryUser -> currentState
        }
    }

    fun reduce(currentState: BottomBarState, event: MessageDetailEvent): BottomBarState {
        return when (event) {
            is MessageDetailEvent.MessageActionsData -> BottomBarState.Data(event.actions)
            is MessageDetailEvent.ErrorLoadingActions -> BottomBarState.Error.FailedLoadingActions
            is MessageDetailEvent.NoPrimaryUser,
            is MessageDetailEvent.MessageBody,
            is MessageDetailEvent.MessageMetadata,
            MessageDetailEvent.NoCachedMetadata -> currentState
        }
    }
}
