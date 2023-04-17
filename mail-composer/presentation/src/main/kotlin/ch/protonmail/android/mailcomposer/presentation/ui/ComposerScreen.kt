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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.default

@Composable
fun ComposerScreen() {
    val maxWidthModifier = Modifier.fillMaxWidth()
    Column {
        ComposerTopBar()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
        ) {
            PrefixedTextField(prefixStringResource = R.string.from_prefix, maxWidthModifier)
            MailDivider()
            PrefixedTextField(prefixStringResource = R.string.to_prefix, maxWidthModifier)
            MailDivider()
            SubjectTextField(maxWidthModifier)
            MailDivider()
            BodyTextField()
        }
    }
}

@Composable
private fun ComposerTopBar() {
    ProtonTopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.close_composer_content_description)
                )
            }
        },
        actions = {
            IconButton(onClick = {}, enabled = false) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_paper_plane),
                    tint = ProtonTheme.colors.iconDisabled,
                    contentDescription = stringResource(R.string.send_message_content_description)
                )
            }
        }
    )
}

@Composable
private fun PrefixedTextField(@StringRes prefixStringResource: Int, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.default,
        prefix = {
            Row {
                Text(
                    text = stringResource(prefixStringResource),
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.default
                )
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
            }
        },
        colors = TextFieldDefaults.composerTextFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun SubjectTextField(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.default,
        colors = TextFieldDefaults.composerTextFieldColors(),
        maxLines = 3,
        placeholder = {
            Text(
                text = stringResource(R.string.subject_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.default
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun BodyTextField(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier.fillMaxSize(),
        textStyle = ProtonTheme.typography.default,
        minLines = 6,
        colors = TextFieldDefaults.composerTextFieldColors(),
        placeholder = {
            Text(
                text = stringResource(R.string.compose_message_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.default
            )
        }
    )
}

@Composable
private fun TextFieldDefaults.composerTextFieldColors(): TextFieldColors =
    colors(
        focusedTextColor = ProtonTheme.colors.textNorm,
        focusedContainerColor = ProtonTheme.colors.backgroundNorm,
        unfocusedContainerColor = ProtonTheme.colors.backgroundNorm,
        focusedLabelColor = ProtonTheme.colors.textNorm,
        unfocusedLabelColor = ProtonTheme.colors.textHint,
        disabledLabelColor = ProtonTheme.colors.textDisabled,
        errorLabelColor = ProtonTheme.colors.notificationError,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen()
    }
}
