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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ComposerBottomBar(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Divider(color = ProtonTheme.colors.separatorNorm, thickness = MailDimens.SeparatorHeight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.ExtraLargeSpacing)
                .padding(horizontal = ProtonDimens.ExtraSmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {}
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_lock),
                    contentDescription = stringResource(id = R.string.composer_button_add_password),
                    tint = ProtonTheme.colors.iconNorm
                )
            }
        }
    }
}