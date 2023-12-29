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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.pin

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPinContinuationAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinErrorUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinStepUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.InsertedPin
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import javax.inject.Inject

class AutoLockPinReducer @Inject constructor(
    private val stepUiMapper: AutoLockPinStepUiMapper,
    private val errorsUiMapper: AutoLockPinErrorUiMapper
) {

    fun newStateFrom(currentState: AutoLockPinState, operation: AutoLockPinEvent) =
        currentState.toNewStateFromEvent(operation)

    private fun AutoLockPinState.toNewStateFromEvent(event: AutoLockPinEvent): AutoLockPinState {
        return when (this) {
            is AutoLockPinState.Loading -> when (event) {
                is AutoLockPinEvent.Data.Loaded -> event.toDataState()
                else -> this
            }

            is AutoLockPinState.DataLoaded -> when (event) {
                is AutoLockPinEvent.Update.PinValueChanged -> updatePinValue(this, event)
                is AutoLockPinEvent.Update.MovedToStep -> moveToStep(this, event.step)
                is AutoLockPinEvent.Update.OperationAborted,
                is AutoLockPinEvent.Update.OperationCompleted -> completeOperation(this)
                is AutoLockPinEvent.Update.VerificationCompleted -> completeVerification(this, event)
                is AutoLockPinEvent.Update.Error -> handleError(this, event)

                else -> this
            }
        }
    }

    private fun handleError(
        state: AutoLockPinState.DataLoaded,
        event: AutoLockPinEvent.Update.Error
    ): AutoLockPinState.DataLoaded = when (event) {
        is AutoLockPinEvent.Update.Error.WrongPinCode -> {
            state.copy(
                pinInsertionState = state.pinInsertionState.copy(remainingAttempts = event.remainingAttempts),
                pinInsertionErrorEffect = Effect.of(errorsUiMapper.toUiModel(event))
            )
        }

        else -> state.copy(pinInsertionErrorEffect = Effect.of(errorsUiMapper.toUiModel(event)))
    }

    private fun completeOperation(state: AutoLockPinState.DataLoaded) =
        state.copy(closeScreenEffect = Effect.of(Unit))

    private fun completeVerification(
        state: AutoLockPinState.DataLoaded,
        event: AutoLockPinEvent.Update.VerificationCompleted
    ): AutoLockPinState.DataLoaded {
        return when (event.action) {
            is AutoLockPinContinuationAction.NavigateToDeepLink ->
                state.copy(navigateEffect = Effect.of(event.action.destination.toDecodedValue()))

            else -> state.copy(closeScreenEffect = Effect.of(Unit))
        }
    }

    private fun moveToStep(state: AutoLockPinState.DataLoaded, step: PinInsertionStep): AutoLockPinState {
        val confirmButtonUiModel = stepUiMapper.toConfirmButtonUiModel(isEnabled = false, step)
        val topBarUiModel = stepUiMapper.toTopBarUiModel(step)

        val newTopBarState = AutoLockPinState.TopBarState(topBarUiModel)
        val newConfirmButtonState = AutoLockPinState.ConfirmButtonState(confirmButtonUiModel)
        val newPinInsertionState = AutoLockPinState.PinInsertionState(
            step, PinVerificationRemainingAttempts.Default, PinInsertionUiModel(InsertedPin.Empty)
        )

        return state.copy(
            pinInsertionState = newPinInsertionState,
            topBarState = newTopBarState,
            confirmButtonState = newConfirmButtonState,
            pinInsertionErrorEffect = Effect.empty()
        )
    }

    private fun updatePinValue(
        state: AutoLockPinState.DataLoaded,
        event: AutoLockPinEvent.Update.PinValueChanged
    ): AutoLockPinState.DataLoaded {
        val newInsertedPin = event.newPin
        val pinInsertionState = state.pinInsertionState.copy(
            step = state.pinInsertionState.step, pinInsertionUiModel = PinInsertionUiModel(newInsertedPin)
        )

        val confirmButtonUiModel =
            state.confirmButtonState.confirmButtonUiModel.copy(isEnabled = newInsertedPin.hasValidLength())
        val confirmButtonState = state.confirmButtonState.copy(confirmButtonUiModel = confirmButtonUiModel)
        return state.copy(pinInsertionState = pinInsertionState, confirmButtonState = confirmButtonState)
    }

    private fun AutoLockPinEvent.Data.Loaded.toDataState(): AutoLockPinState.DataLoaded {
        val pinInsertionUiModel = PinInsertionUiModel(InsertedPin.Empty)
        val topBarUiModel = stepUiMapper.toTopBarUiModel(step)
        val confirmButtonUiModel = stepUiMapper.toConfirmButtonUiModel(isEnabled = false, step)
        val errorEffect = errorsUiMapper.toUiModel(remainingAttempts)?.let { Effect.of(it) } ?: Effect.empty()

        return AutoLockPinState.DataLoaded(
            topBarState = AutoLockPinState.TopBarState(topBarUiModel),
            pinInsertionState = AutoLockPinState.PinInsertionState(
                step,
                remainingAttempts,
                pinInsertionUiModel
            ),
            confirmButtonState = AutoLockPinState.ConfirmButtonState(confirmButtonUiModel),
            Effect.empty(),
            Effect.empty(),
            errorEffect
        )
    }
}