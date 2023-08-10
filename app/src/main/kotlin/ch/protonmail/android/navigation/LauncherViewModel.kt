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

package ch.protonmail.android.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailnotifications.data.repository.NotificationTokenRepository
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.navigation.model.LauncherState.AccountNeeded
import ch.protonmail.android.navigation.model.LauncherState.PrimaryExist
import ch.protonmail.android.navigation.model.LauncherState.Processing
import ch.protonmail.android.navigation.model.LauncherState.StepNeeded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionForceLogout
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.entity.SecondFactorResult
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.auth.presentation.onLoginResult
import me.proton.core.auth.presentation.onLoginSsoResult
import me.proton.core.auth.presentation.onOnSignUpResult
import me.proton.core.auth.presentation.onSecondFactorResult
import me.proton.core.auth.presentation.onTwoPassModeResult
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.report.presentation.ReportOrchestrator
import me.proton.core.user.domain.UserManager
import me.proton.core.usersettings.presentation.UserSettingsOrchestrator
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val product: Product,
    private val requiredAccountType: AccountType,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val authOrchestrator: AuthOrchestrator,
    private val plansOrchestrator: PlansOrchestrator,
    private val reportOrchestrator: ReportOrchestrator,
    private val userSettingsOrchestrator: UserSettingsOrchestrator,
    private val notificationsPermissionsOrchestrator: NotificationsPermissionsOrchestrator,
    private val notificationsTokenRepository: NotificationTokenRepository
) : ViewModel() {

    val state: StateFlow<LauncherState> = accountManager.getAccounts().combine(
        notificationsPermissionsOrchestrator.permissionResult()
    ) { accounts, permissionResult ->
        when {
            accounts.isEmpty() || accounts.all { it.isDisabled() } -> AccountNeeded
            accounts.any { it.isReady() } -> when (permissionResult) {
                PermissionResult.CHECKING -> {
                    notificationsPermissionsOrchestrator.requestPermissionIfRequired()
                    StepNeeded
                }

                PermissionResult.SHOW_RATIONALE,
                PermissionResult.GRANTED,
                PermissionResult.DENIED -> PrimaryExist
            }

            accounts.any { it.isStepNeeded() } -> StepNeeded
            else -> Processing
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = Processing
    )

    fun register(context: AppCompatActivity) {
        authOrchestrator.register(context)
        authOrchestrator.registerAdditionalCallbacks(context)
        plansOrchestrator.register(context)
        reportOrchestrator.register(context)
        userSettingsOrchestrator.register(context)
        notificationsPermissionsOrchestrator.register(context)

        accountManager.observe(context.lifecycle, Lifecycle.State.CREATED)
            .onSessionForceLogout { userManager.lock(it.userId) }
            .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
            .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
            .onSessionSecondFactorNeeded { authOrchestrator.startSecondFactorWorkflow(it) }
            .onAccountTwoPassModeNeeded { authOrchestrator.startTwoPassModeWorkflow(it) }
            .onAccountCreateAddressNeeded { authOrchestrator.startChooseAddressWorkflow(it) }
    }

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                Action.AddAccount -> onAddAccount()
                Action.OpenPasswordManagement -> onOpenPasswordManagement()
                Action.OpenRecoveryEmail -> onOpenRecoveryEmail()
                Action.OpenReport -> onOpenReport()
                Action.OpenSubscription -> onOpenSubscription()
                is Action.Remove -> onRemove(action.userId)
                is Action.SignIn -> onSignIn(action.userId)
                is Action.SignOut -> onSignOut(action.userId)
                is Action.Switch -> onSwitch(action.userId)
            }.exhaustive
        }
    }

    @SuppressWarnings("ComplexMethod")
    private fun AuthOrchestrator.registerAdditionalCallbacks(context: AppCompatActivity) {
        onAddAccountResult { result ->
            viewModelScope.launch {
                if (result == null && getPrimaryUserIdOrNull() == null) {
                    context.finish()
                }
            }

            val userId = result?.userId?.let { UserId(it) } ?: return@onAddAccountResult
            registerNotificationTokenForUserId(userId)
        }

        onOnSignUpResult { result ->
            val userId = result?.let { UserId(it.userId) } ?: return@onOnSignUpResult
            registerNotificationTokenForUserId(userId)
        }

        onLoginResult {
            val userId = it?.let { UserId(it.userId) } ?: return@onLoginResult
            registerNotificationTokenForUserId(userId)
        }

        onLoginSsoResult { result ->
            val userId = result?.let { UserId(it.userId) } ?: return@onLoginSsoResult
            registerNotificationTokenForUserId(userId)
        }

        onSecondFactorResult { result ->
            val userId =
                (result as? SecondFactorResult.Success)?.let { UserId(it.userId) } ?: return@onSecondFactorResult
            registerNotificationTokenForUserId(userId)
        }

        onTwoPassModeResult { result ->
            val userId = result?.let { UserId(it.userId) } ?: return@onTwoPassModeResult
            registerNotificationTokenForUserId(userId)
        }
    }

    private fun registerNotificationTokenForUserId(userId: UserId) {
        viewModelScope.launch {
            val account = accountManager.getAccount(userId).firstOrNull() ?: return@launch
            if (account.isReady()) notificationsTokenRepository.synchronizeTokenForUser(userId)
        }
    }

    private fun onAddAccount() {
        authOrchestrator.startAddAccountWorkflow(
            requiredAccountType = AccountType.Internal,
            creatableAccountType = AccountType.Internal,
            product = product
        )
    }

    private suspend fun onOpenPasswordManagement() {
        getPrimaryUserIdOrNull()?.let {
            userSettingsOrchestrator.startPasswordManagementWorkflow(it)
        }
    }

    private suspend fun onOpenRecoveryEmail() {
        getPrimaryUserIdOrNull()?.let {
            userSettingsOrchestrator.startUpdateRecoveryEmailWorkflow(it)
        }
    }

    private suspend fun onOpenReport() = viewModelScope.launch {
        reportOrchestrator.startBugReport()
    }

    private suspend fun onOpenSubscription() {
        getPrimaryUserIdOrNull()?.let {
            plansOrchestrator.showCurrentPlanWorkflow(it)
        }
    }

    private suspend fun onRemove(userId: UserId) {
        accountManager.removeAccount(userId)
    }

    private suspend fun onSignIn(userId: UserId?) {
        val account = userId?.let { getAccountOrNull(it) }
        authOrchestrator.startLoginWorkflow(requiredAccountType, username = account?.username)
    }

    private suspend fun onSignOut(userId: UserId?) {
        accountManager.disableAccount(requireNotNull(userId ?: getPrimaryUserIdOrNull()))
    }

    private suspend fun onSwitch(userId: UserId) {
        val account = getAccountOrNull(userId) ?: return
        when {
            account.isDisabled() -> onSignIn(userId)
            account.isReady() -> accountManager.setAsPrimary(userId)
        }
    }

    private suspend fun getAccountOrNull(it: UserId) = accountManager.getAccount(it).firstOrNull()
    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    sealed interface Action {

        object AddAccount : Action
        object OpenPasswordManagement : Action
        object OpenRecoveryEmail : Action
        object OpenReport : Action
        object OpenSubscription : Action
        data class Remove(val userId: UserId) : Action
        data class SignIn(val userId: UserId?) : Action
        data class SignOut(val userId: UserId?) : Action
        data class Switch(val userId: UserId) : Action
    }
}
