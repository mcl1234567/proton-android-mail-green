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

package ch.protonmail.android.mailcontact.presentation

import java.time.LocalDate
import ch.protonmail.android.mailcommon.presentation.usecase.DecodeByteArray
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactFormUiModelMapperTest {

    private val decodeByteArray = mockk<DecodeByteArray> {
        every { this@mockk.invoke(any()) } returns mockk()
    }

    private val contactFormUiModelMapper = ContactFormUiModelMapper(
        decodeByteArray = decodeByteArray
    )

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
    fun `maps DecryptedContact to ContactFormUiModel`() {
        val photoByteArray = ContactImagesSample.Photo
        val logoByteArray = ContactImagesSample.Logo
        val decryptedContact = DecryptedContact(
            id = ContactSample.Mario.id,
            contactGroups = listOf(
                ContactGroup(
                    "Group 1",
                    androidx.compose.ui.graphics.Color.Red.getHexStringFromColor()
                )
            ),
            structuredName = ContactProperty.StructuredName(
                family = "Mario Last Name", given = "Mario First Name"
            ),
            formattedName = ContactProperty.FormattedName(value = "Mario@protonmail.com"),
            emails = listOf(
                ContactProperty.Email(type = ContactProperty.Email.Type.Email, value = "Mario@protonmail.com"),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Home,
                    value = "home_email@Mario.protonmail.com"
                ),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Work,
                    value = "work_email@Mario.protonmail.com"
                ),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Other,
                    value = "other_email@Mario.protonmail.com"
                )
            ),
            telephones = listOf(
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Telephone, text = "1231231235"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Home,
                    text = "23233232323"
                ),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Work,
                    text = "45454545"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Other, text = "565656"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Mobile,
                    text = "676767"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Main, text = "787887"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Fax,
                    text = "898989"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Pager, text = "90909090")
            ),
            addresses = listOf(
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Address,
                    streetAddress = "Address Street1",
                    locality = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country"
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Other,
                    streetAddress = "Address Other1",
                    locality = "City",
                    region = "Region",
                    postalCode = "234",
                    country = "Country"
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Home,
                    streetAddress = "Home address the rest is empty",
                    locality = "",
                    region = "",
                    postalCode = "",
                    country = ""
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Work,
                    streetAddress = "City the rest is empty",
                    locality = "",
                    region = "",
                    postalCode = "",
                    country = ""
                )
            ),
            birthday = ContactProperty.Birthday(date = LocalDate.of(2023, 12, 14)),
            notes = listOf(ContactProperty.Note(value = "Note1"), ContactProperty.Note(value = "Note2")),
            photos = listOf(
                ContactProperty.Photo(
                    data = photoByteArray,
                    contentType = "jpeg"
                )
            ),
            organizations = listOf(
                ContactProperty.Organization(value = "Organization1"),
                ContactProperty.Organization(value = "Organization2")
            ),
            titles = listOf(ContactProperty.Title(value = "Title")),
            roles = listOf(ContactProperty.Role(value = "Role")),
            timezones = listOf(ContactProperty.Timezone(text = "Europe/Paris")),
            logos = listOf(
                ContactProperty.Logo(
                    data = logoByteArray,
                    contentType = "jpeg"
                )
            ),
            members = listOf(ContactProperty.Member(value = "Member")),
            languages = listOf(ContactProperty.Language(value = "English")),
            urls = listOf(ContactProperty.Url(value = "http://proton.me")),
            gender = ContactProperty.Gender(gender = "Gender"),
            anniversary = ContactProperty.Anniversary(date = LocalDate.of(2023, 12, 6))
        )

        val actual = contactFormUiModelMapper.toContactFormUiModel(decryptedContact)

        val expected = ContactFormUiModel(
            id = ContactSample.Mario.id,
            avatar = ContactFormAvatar.Photo(bitmap = decodeByteArray(photoByteArray)!!),
            displayName = "Mario@protonmail.com",
            firstName = "Mario First Name",
            lastName = "Mario Last Name",
            emails = listOf(
                InputField.SingleTyped(
                    value = "Mario@protonmail.com",
                    selectedType = FieldType.EmailType.Email
                ),
                InputField.SingleTyped(
                    value = "home_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Home
                ),
                InputField.SingleTyped(
                    value = "work_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Work
                ),
                InputField.SingleTyped(
                    value = "other_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Other
                )
            ),
            phones = listOf(
                InputField.SingleTyped(
                    value = "1231231235",
                    selectedType = FieldType.PhoneType.Telephone
                ),
                InputField.SingleTyped(
                    value = "23233232323",
                    selectedType = FieldType.PhoneType.Home
                ),
                InputField.SingleTyped(
                    value = "45454545",
                    selectedType = FieldType.PhoneType.Work
                ),
                InputField.SingleTyped(
                    value = "565656",
                    selectedType = FieldType.PhoneType.Other
                ),
                InputField.SingleTyped(
                    value = "676767",
                    selectedType = FieldType.PhoneType.Mobile
                ),
                InputField.SingleTyped(
                    value = "787887",
                    selectedType = FieldType.PhoneType.Main
                ),
                InputField.SingleTyped(
                    value = "898989",
                    selectedType = FieldType.PhoneType.Fax
                ),
                InputField.SingleTyped(
                    value = "90909090",
                    selectedType = FieldType.PhoneType.Pager
                )
            ),
            addresses = listOf(
                InputField.Address(
                    streetAddress = "Address Street1",
                    city = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country",
                    selectedType = FieldType.AddressType.Address
                ),
                InputField.Address(
                    streetAddress = "Address Other1",
                    city = "City",
                    region = "Region",
                    postalCode = "234",
                    country = "Country",
                    selectedType = FieldType.AddressType.Other
                ),
                InputField.Address(
                    streetAddress = "Home address the rest is empty",
                    city = "",
                    region = "",
                    postalCode = "",
                    country = "",
                    selectedType = FieldType.AddressType.Home
                ),
                InputField.Address(
                    streetAddress = "City the rest is empty",
                    city = "",
                    region = "",
                    postalCode = "",
                    country = "",
                    selectedType = FieldType.AddressType.Work
                )
            ),
            birthday = InputField.Date(
                value = LocalDate.of(2023, 12, 14)
            ),
            notes = listOf(
                InputField.Note(
                    value = "Note1"
                ),
                InputField.Note(
                    value = "Note2"
                )
            ),
            others = listOf(
                InputField.SingleTyped(
                    value = "Organization1",
                    selectedType = FieldType.OtherType.Organization
                ),
                InputField.SingleTyped(
                    value = "Organization2",
                    selectedType = FieldType.OtherType.Organization
                ),
                InputField.SingleTyped(
                    value = "Title",
                    selectedType = FieldType.OtherType.Title
                ),
                InputField.SingleTyped(
                    value = "Role",
                    selectedType = FieldType.OtherType.Role
                ),
                InputField.SingleTyped(
                    value = "Europe/Paris",
                    selectedType = FieldType.OtherType.TimeZone
                ),
                InputField.ImageTyped(
                    value = decodeByteArray(logoByteArray)!!,
                    selectedType = FieldType.OtherType.Logo
                ),
                InputField.SingleTyped(
                    value = "Member",
                    selectedType = FieldType.OtherType.Member
                ),
                InputField.SingleTyped(
                    value = "English",
                    selectedType = FieldType.OtherType.Language
                ),
                InputField.SingleTyped(
                    value = "http://proton.me",
                    selectedType = FieldType.OtherType.Url
                ),
                InputField.SingleTyped(
                    value = "Gender",
                    selectedType = FieldType.OtherType.Gender
                ),
                InputField.DateTyped(
                    value = LocalDate.of(2023, 12, 6),
                    selectedType = FieldType.OtherType.Anniversary
                )
            ),
            otherTypes = FieldType.OtherType.values().toList()
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `maps empty DecryptedContact to empty ContactFormUiModel`() {
        val decryptedContact = DecryptedContact(
            id = ContactSample.Mario.id
        )

        val actual = contactFormUiModelMapper.toContactFormUiModel(decryptedContact)

        val expected = emptyContactFormUiModel.copy(
            id = ContactSample.Mario.id
        )

        assertEquals(actual, expected)
    }
}