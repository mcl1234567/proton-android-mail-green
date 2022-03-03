/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.domain

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.model.CustomAppLanguagePreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import ch.protonmail.android.mailsettings.domain.repository.CustomAppLanguageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveAppSettingsTest {

    private val combinedContactsRepository = mockk<CombinedContactsRepository> {
        every { this@mockk.observe() } returns flowOf(CombinedContactsPreference(true))
    }
    private val customAppLanguageRepository = mockk<CustomAppLanguageRepository> {
        every { this@mockk.observe() } returns flowOf(null)
    }
    private val alternativeRoutingRepository = mockk<AlternativeRoutingRepository> {
        every { this@mockk.observe() } returns flowOf(AlternativeRoutingPreference(true))
    }
    private val autoLockRepository = mockk<AutoLockRepository> {
        every { this@mockk.observe() } returns flowOf(AutoLockPreference(false))
    }

    private lateinit var observeAppSettings: ObserveAppSettings

    @Before
    fun setUp() {
        observeAppSettings = ObserveAppSettings(
            autoLockRepository,
            alternativeRoutingRepository,
            customAppLanguageRepository,
            combinedContactsRepository
        )
    }

    @Test
    fun `auto lock value is returned from auto lock repository`() = runTest {
        // Given
        every { autoLockRepository.observe() } returns flowOf(AutoLockPreference(true))

        // When
        observeAppSettings().test {
            // Then
            val expected = AppSettings(
                hasAutoLock = true,
                hasAlternativeRouting = true,
                customAppLanguage = null,
                hasCombinedContacts = true
            )
            assertEquals(expected, awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `has alternative routing value is returned from alternative routing repository`() =
        runTest {
            // Given
            every { alternativeRoutingRepository.observe() } returns flowOf(
                AlternativeRoutingPreference(false)
            )

            // When
            observeAppSettings().test {
                // Then
                val expected = AppSettings(
                    hasAutoLock = false,
                    hasAlternativeRouting = false,
                    customAppLanguage = null,
                    hasCombinedContacts = true
                )
                assertEquals(expected, awaitItem())

                awaitComplete()
            }
        }

    @Test
    fun `custom app language value is returned from custom app language repository`() = runTest {
        // Given
        every { customAppLanguageRepository.observe() } returns flowOf(
            CustomAppLanguagePreference("en-EN")
        )

        // When
        observeAppSettings().test {
            // Then
            val expected = AppSettings(
                hasAutoLock = false,
                hasAlternativeRouting = true,
                customAppLanguage = "en-EN",
                hasCombinedContacts = true
            )
            assertEquals(expected, awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `null custom app language is returned when custom app language repository is empty`() =
        runTest {
            // Given
            every { customAppLanguageRepository.observe() } returns flowOf(null)

            // When
            observeAppSettings().test {
                // Then
                val expected = AppSettings(
                    hasAutoLock = false,
                    hasAlternativeRouting = true,
                    customAppLanguage = null,
                    hasCombinedContacts = true
                )
                assertEquals(expected, awaitItem())

                awaitComplete()
            }
        }

    @Test
    fun `has combined contacts is returned from combined contacts repository`() = runTest {
        // Given
        every { combinedContactsRepository.observe() } returns flowOf(
            CombinedContactsPreference(false)
        )

        // When
        observeAppSettings().test {
            // Then
            val expected = AppSettings(
                hasAutoLock = false,
                hasAlternativeRouting = true,
                customAppLanguage = null,
                hasCombinedContacts = false
            )
            assertEquals(expected, awaitItem())

            awaitComplete()
        }
    }
}
