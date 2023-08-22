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

package ch.protonmail.android.di

import ch.protonmail.android.mailcommon.domain.MailFeatureDefaults
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class FeatureFlagModuleTest(private val testInput: TestInput) {

    @Test
    fun `should provide the correct defaults`() = with(testInput) {
        // When
        val actualDefaults = FeatureFlagModule.provideDefaultMailFeatureFlags(buildFlavor, buildDebug)

        // Then
        assertEquals(expectedDefaults, actualDefaults)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                buildFlavor = "dev",
                buildDebug = false,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to false,
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.ShowSettings to true,
                    MailFeatureId.HideComposer to true,
                    MailFeatureId.SelectionMode to false
                )
            ),
            TestInput(
                buildFlavor = "dev",
                buildDebug = true,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to true,
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.ShowSettings to true,
                    MailFeatureId.HideComposer to false,
                    MailFeatureId.SelectionMode to true
                )
            ),
            TestInput(
                buildFlavor = "alpha",
                buildDebug = false,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to false,
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.ShowSettings to true,
                    MailFeatureId.HideComposer to true,
                    MailFeatureId.SelectionMode to false
                )
            ),
            TestInput(
                buildFlavor = "alpha",
                buildDebug = true,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to true,
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.ShowSettings to true,
                    MailFeatureId.HideComposer to false,
                    MailFeatureId.SelectionMode to true
                )
            ),
            TestInput(
                buildFlavor = "prod",
                buildDebug = false,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to false,
                    MailFeatureId.ConversationMode to false,
                    MailFeatureId.ShowSettings to false,
                    MailFeatureId.HideComposer to true,
                    MailFeatureId.SelectionMode to false
                )
            ),
            TestInput(
                buildFlavor = "prod",
                buildDebug = true,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.AddAttachmentsToDraft to true,
                    MailFeatureId.ConversationMode to false,
                    MailFeatureId.ShowSettings to false,
                    MailFeatureId.HideComposer to false,
                    MailFeatureId.SelectionMode to true
                )
            )
        )
    }

    data class TestInput(
        val buildFlavor: String,
        val buildDebug: Boolean,
        val expectedDefaultsMap: Map<MailFeatureId, Boolean>
    ) {
        val expectedDefaults = MailFeatureDefaults(expectedDefaultsMap)
    }
}
