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

package ch.protonmail.android.mailcontact.presentation.contactform

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactFormViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testContactId = ContactFormPreviewData.contactFormSampleData.id!!

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactFormUiModelMapperMock = mockk<ContactFormUiModelMapper>()
    private val observeDecryptedContactMock = mockk<ObserveDecryptedContact>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactFormReducer()

    private val contactFormViewModel by lazy {
        ContactFormViewModel(
            observeDecryptedContactMock,
            reducer,
            contactFormUiModelMapperMock,
            observePrimaryUserId,
            savedStateHandleMock
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given empty Contact ID in SavedState, when init, then emits create state`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Contact ID in SavedState, when error in decrypt, then emits loading error state`() = runTest {
        // Given
        expectSavedStateContactId(testContactId)
        expectDecryptedContact(testUserId, testContactId, null)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.contact_form_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Contact ID in SavedState, when init, then emits update state`() = runTest {
        // Given
        expectSavedStateContactId(testContactId)
        val expectedDecryptedContact = DecryptedContact(testContactId)
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        val expectedContactFormUiModel = ContactFormPreviewData.contactFormSampleData
        expectContactFormUiModel(expectedDecryptedContact, expectedContactFormUiModel)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = expectedContactFormUiModel
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectSavedStateContactId(contactId: ContactId?) {
        every {
            savedStateHandleMock.get<String>(ContactFormScreen.ContactFormContactIdKey)
        } returns contactId?.id
    }

    private fun expectDecryptedContact(
        userId: UserId,
        contactId: ContactId,
        decryptedContact: DecryptedContact?
    ) {
        every {
            observeDecryptedContactMock.invoke(userId, contactId)
        } returns flowOf(decryptedContact?.right() ?: DataError.Local.NoDataCached.left())
    }

    private fun expectContactFormUiModel(
        decryptedContact: DecryptedContact,
        expectedContactFormUiModel: ContactFormUiModel
    ) {
        every {
            contactFormUiModelMapperMock.toContactFormUiModel(decryptedContact)
        } returns expectedContactFormUiModel
    }

}
