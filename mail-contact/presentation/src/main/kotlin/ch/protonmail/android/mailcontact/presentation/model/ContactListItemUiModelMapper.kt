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

package ch.protonmail.android.mailcontact.presentation.model

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import me.proton.core.contact.domain.entity.Contact

fun List<Contact>.toContactListItemUiModel(): List<ContactListItemUiModel> {
    val contacts = arrayListOf<ContactListItemUiModel>()
    this.map {
        it.copy(name = it.name.trim())
    }.sortedBy {
        it.name
    }.groupBy {
        it.name.trim().first().uppercaseChar()
    }.forEach { nameGroup ->
        contacts.add(
            ContactListItemUiModel.Header(value = nameGroup.key.toString())
        )
        nameGroup.value.forEach { contact ->
            contacts.add(
                ContactListItemUiModel.Contact(
                    id = contact.id.id,
                    name = contact.name,
                    emails = contact.contactEmails.sortedBy { it.order }.map { contactEmail -> contactEmail.email },
                    avatar = AvatarUiModel.ParticipantInitial(getInitials(contact.name))
                )
            )
        }
    }
    return contacts
}

private fun getInitials(name: String, takeFirstOnly: Boolean? = false): String {
    if (name.isBlank()) return ""
    if (takeFirstOnly == true) return name.uppercase().take(1)
    val initials = name.uppercase().split(' ')
        .mapNotNull { it.firstOrNull()?.toString() }
        .reduce { acc, s -> acc + s }
    // Keep only the first and last initials
    return if (initials.length > 2) initials[0].toString() + initials[initials.lastIndex] else initials
}
