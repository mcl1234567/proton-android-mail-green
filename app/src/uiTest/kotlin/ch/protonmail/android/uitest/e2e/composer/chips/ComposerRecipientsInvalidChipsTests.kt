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

package ch.protonmail.android.uitest.e2e.composer.chips

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.test.annotations.suite.SmokeExtendedTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.chips.ChipsCreationTrigger
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.section.recipients.ComposerRecipientsSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@SmokeExtendedTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerRecipientsInvalidChipsTests :
    MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut),
    ComposerChipsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedRecipientChip = RecipientChipEntry(
        index = 0,
        text = "test",
        state = RecipientChipValidationState.Invalid
    )

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher = composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("190232")
    fun testInvalidToRecipientChip() {
        composerRobot {
            toRecipientSection {
                createAndVerifyInvalidChip()
            }

            snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
        }
    }

    @Test
    @SmokeTest
    @TestId("190233")
    fun testInvalidCcRecipientChip() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            ccRecipientSection {
                createAndVerifyInvalidChip()
            }

            snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
        }
    }

    @Test
    @SmokeTest
    @TestId("190234")
    fun testInvalidBccRecipientChip() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            bccRecipientSection {
                createAndVerifyInvalidChip()
            }

            snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
        }
    }

    @Test
    @TestId("190235")
    fun testInvalidRecipientChipOnFocusChange() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
                typeRecipient("test")
            }

            ccRecipientSection {
                tapRecipientField()
            }

            toRecipientSection {
                verify { hasRecipientChips(expectedRecipientChip) }
            }

            snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
        }
    }

    @Test
    @TestId("190238")
    fun testInvalidRecipientChipOnSpacebarTap() {
        composerRobot {
            toRecipientSection {
                createAndVerifyInvalidChip(trigger = ChipsCreationTrigger.Spacebar)
            }

            snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
        }
    }

    @Test
    @TestId("190239")
    fun testMultipleInvalidRecipientChipsOnSpacebarTap() {
        composerRobot {
            toRecipientSection {
                withRandomRecipients(size = 100, state = RecipientChipValidationState.Invalid) {
                    typeRecipient(it.text, autoConfirm = true)

                    verify { hasRecipientChips(it) }
                    snackbarSection { verify { hasMessage(SnackbarTextEntry.InvalidEmailAddress) } }
                }
            }
        }
    }

    private fun ComposerRecipientsSection.createAndVerifyInvalidChip(
        trigger: ChipsCreationTrigger = ChipsCreationTrigger.ImeAction
    ) = createAndVerifyChip(state = RecipientChipValidationState.Invalid, trigger)
}