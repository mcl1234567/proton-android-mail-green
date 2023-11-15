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

package ch.protonmail.android.mailsettings.data.repository.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import kotlinx.coroutines.flow.Flow
import me.proton.core.user.domain.entity.AddressId

interface AddressIdentityLocalDataSource {

    fun observeDisplayName(addressId: AddressId): Flow<Either<DataError, DisplayName>>

    fun observeSignatureValue(addressId: AddressId): Flow<Either<DataError, SignatureValue>>

    fun observeSignaturePreference(addressId: AddressId): Flow<Either<PreferencesError, SignaturePreference>>

    suspend fun updateSignatureEnabledState(addressId: AddressId, enabled: Boolean): Either<PreferencesError, Unit>

    suspend fun updateAddressIdentity(
        addressId: AddressId,
        displayName: DisplayName,
        signature: Signature
    ): Either<DataError, Unit>
}