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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.MessageDetailViewModel
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Suppress("UseComposableActions")
fun MessageDetailScreen(
    modifier: Modifier = Modifier,
    onExit: (message: String?) -> Unit,
    openMessageBodyLink: (uri: Uri) -> Unit,
    viewModel: MessageDetailViewModel = hiltViewModel(),
    showFeatureMissingSnackbar: () -> Unit = {},
) {
    val state by rememberAsState(flow = viewModel.state, initial = MessageDetailViewModel.initialState)
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    state.bottomSheetState?.let {
        // Avoids a "jumping" of the bottom sheet
        if (it.isShowEffectWithoutContent()) return@let

        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) { onDispose { viewModel.submit(MessageViewAction.DismissBottomSheet) } }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(
            topStart = ProtonDimens.LargeCornerRadius,
            topEnd = ProtonDimens.LargeCornerRadius
        ),
        sheetBackgroundColor = ProtonTheme.colors.backgroundNorm,
        sheetContent = {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    onFolderSelected = { viewModel.submit(MessageViewAction.MoveToDestinationSelected(it)) },
                    onDoneClick = { viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(it)) }
                )
                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    onLabelAsSelected = { viewModel.submit(MessageViewAction.LabelAsToggleAction(it)) },
                    onDoneClick = { viewModel.submit(MessageViewAction.LabelAsConfirmed(it)) }
                )
                null -> ProtonCenteredProgress()
            }.exhaustive
        }
    ) {
        MessageDetailScreen(
            modifier = modifier,
            state = state,
            actions = MessageDetailScreen.Actions(
                onExit = onExit,
                onReload = { viewModel.submit(MessageViewAction.Reload) },
                onStarClick = { viewModel.submit(MessageViewAction.Star) },
                onTrashClick = { viewModel.submit(MessageViewAction.Trash) },
                onUnStarClick = { viewModel.submit(MessageViewAction.UnStar) },
                onUnreadClick = { viewModel.submit(MessageViewAction.MarkUnread) },
                onMoveClick = { viewModel.submit(MessageViewAction.RequestMoveToBottomSheet) },
                onLabelAsClick = { viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet) },
                onMessageBodyLinkClicked = { viewModel.submit(MessageViewAction.MessageBodyLinkClicked(it)) },
                onOpenMessageBodyLink = openMessageBodyLink,
                onReplyClick = { showFeatureMissingSnackbar() },
                onReplyAllClick = { showFeatureMissingSnackbar() },
                onDeleteClick = { showFeatureMissingSnackbar() }
            ),
            showFeatureMissingSnackbar = showFeatureMissingSnackbar
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MessageDetailScreen(
    state: MessageDetailState,
    actions: MessageDetailScreen.Actions,
    modifier: Modifier = Modifier,
    showFeatureMissingSnackbar: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = ProtonSnackbarHostState()

    ConsumableLaunchedEffect(state.exitScreenEffect) { actions.onExit(null) }
    ConsumableTextEffect(state.exitScreenWithMessageEffect) { string ->
        actions.onExit(string)
    }
    ConsumableTextEffect(state.error) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = string)
    }
    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) {
        actions.onOpenMessageBodyLink(it)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            ProtonSnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            val uiModel = (state.messageMetadataState as? MessageMetadataState.Data)?.messageDetailActionBar
            DetailScreenTopBar(
                title = uiModel?.subject ?: DetailScreenTopBar.NoTitle,
                isStarred = uiModel?.isStarred,
                messageCount = null,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = { actions.onExit(null) },
                    onStarClick = actions.onStarClick,
                    onUnStarClick = actions.onUnStarClick
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomActionBar(
                state = state.bottomBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onReply = {
                        actions.onReplyClick()
                    },
                    onReplyAll = {
                        actions.onReplyClick()
                    },
                    onForward = { Timber.d("message onForward clicked") },
                    onMarkRead = { Timber.d("message onMarkRead clicked") },
                    onMarkUnread = actions.onUnreadClick,
                    onStar = { Timber.d("message onStar clicked") },
                    onUnstar = { Timber.d("message onUnstar clicked") },
                    onMove = actions.onMoveClick,
                    onLabel = actions.onLabelAsClick,
                    onTrash = actions.onTrashClick,
                    onDelete = {
                        actions.onDeleteClick()
                    },
                    onArchive = { Timber.d("message onArchive clicked") },
                    onSpam = { Timber.d("message onSpam clicked") },
                    onViewInLightMode = { Timber.d("message onViewInLightMode clicked") },
                    onViewInDarkMode = { Timber.d("message onViewInDarkMode clicked") },
                    onPrint = { Timber.d("message onPrint clicked") },
                    onViewHeaders = { Timber.d("message onViewHeaders clicked") },
                    onViewHtml = { Timber.d("message onViewHtml clicked") },
                    onReportPhishing = { Timber.d("message onReportPhishing clicked") },
                    onRemind = { Timber.d("message onRemind clicked") },
                    onSavePdf = { Timber.d("message onSavePdf clicked") },
                    onSenderEmail = { Timber.d("message onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("message onSaveAttachments clicked") }
                )
            )
        }
    ) { innerPadding ->
        when (state.messageMetadataState) {
            is MessageMetadataState.Data -> MessageDetailContent(
                modifier = Modifier.padding(innerPadding),
                messageMetadataState = state.messageMetadataState,
                messageBodyState = state.messageBodyState,
                onReload = actions.onReload,
                onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
            is MessageMetadataState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )
        }.exhaustive
    }
}

@Composable
@Suppress("UseComposableActions")
private fun MessageDetailContent(
    modifier: Modifier = Modifier,
    messageMetadataState: MessageMetadataState.Data,
    messageBodyState: MessageBodyState,
    onReload: () -> Unit,
    onMessageBodyLinkClicked: (uri: Uri) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        item {
            MessageDetailHeader(
                uiModel = messageMetadataState.messageDetailHeader,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar,
            )
            Divider(thickness = MailDimens.SeparatorHeight, color = ProtonTheme.colors.separatorNorm)
            when (messageBodyState) {
                is MessageBodyState.Loading -> ProtonCenteredProgress()
                is MessageBodyState.Data -> MessageBody(
                    messageBodyUiModel = messageBodyState.messageBodyUiModel,
                    onMessageBodyLinkClicked = onMessageBodyLinkClicked
                )
                is MessageBodyState.Error.Data -> MessageBodyLoadingError(
                    messageBodyState = messageBodyState,
                    onReload = onReload
                )
                is MessageBodyState.Error.Decryption -> {
                    ProtonErrorMessage(errorMessage = stringResource(id = R.string.decryption_error))
                    MessageBody(
                        messageBodyUiModel = messageBodyState.encryptedMessageBody,
                        onMessageBodyLinkClicked = onMessageBodyLinkClicked
                    )
                }
            }
        }
    }
}

object MessageDetailScreen {

    const val MESSAGE_ID_KEY = "message id"

    data class Actions(
        val onExit: (notifyUserMessage: String?) -> Unit,
        val onReload: () -> Unit,
        val onStarClick: () -> Unit,
        val onTrashClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onUnreadClick: () -> Unit,
        val onMoveClick: () -> Unit,
        val onLabelAsClick: () -> Unit,
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onOpenMessageBodyLink: (uri: Uri) -> Unit,
        val onReplyClick: () -> Unit,
        val onReplyAllClick: () -> Unit,
        val onDeleteClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExit = {},
                onReload = {},
                onStarClick = {},
                onTrashClick = {},
                onUnStarClick = {},
                onUnreadClick = {},
                onMoveClick = {},
                onLabelAsClick = {},
                onMessageBodyLinkClicked = {},
                onOpenMessageBodyLink = {},
                onReplyClick = {},
                onReplyAllClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview(
    @PreviewParameter(MessageDetailsPreviewProvider::class) state: MessageDetailState
) {
    ProtonTheme3 {
        MessageDetailScreen(state = state, actions = MessageDetailScreen.Actions.Empty)
    }
}
