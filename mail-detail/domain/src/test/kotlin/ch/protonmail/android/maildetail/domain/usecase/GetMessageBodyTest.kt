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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.DecryptedData
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.user.domain.UserAddressManager
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMessageBodyTest {

    private val messageId = MessageId("messageId")
    private val decryptedMessageBody = "Decrypted message body."

    private val pgpCryptoMock = mockk<PGPCrypto>()
    private val cryptoContext = mockk<CryptoContext>(relaxed = true) {
        every { pgpCrypto } returns pgpCryptoMock
    }
    private val messageRepository = mockk<MessageRepository> {
        coEvery { getMessageWithBody(UserIdTestData.userId, messageId) } returns
            MessageWithBody(
                MessageTestData.message,
                MessageBodyTestData.messageBody
            ).right()
    }
    private val userAddressManager = mockk<UserAddressManager> {
        coEvery { getAddress(UserIdTestData.userId, MessageTestData.message.addressId) } returns mockk {
            every { keys } returns emptyList()
        }
    }

    private val getMessageBody = GetMessageBody(cryptoContext, messageRepository, userAddressManager)

    @Test
    fun `when repository gets message body and decryption is successful then the decrypted message body is returned`() =
        runTest {
            // Given
            val expected = DecryptedMessageBody(decryptedMessageBody).right()
            every {
                pgpCryptoMock.decryptAndVerifyData(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    publicKeys = any(),
                    unlockedKeys = any(),
                    time = any()
                )
            } returns DecryptedData(decryptedMessageBody.toByteArray(), VerificationStatus.Success)

            // When
            val actual = getMessageBody(UserIdTestData.userId, messageId)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `when repository gets message body and decryption has failed then a decryption error is returned`() =
        runTest {
            // Given
            val expected = DataError.Local.DecryptionError(MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY).left()
            every {
                pgpCryptoMock.decryptAndVerifyData(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    publicKeys = any(),
                    unlockedKeys = any(),
                    time = any()
                )
            } throws CryptoException()

            // When
            val actual = getMessageBody(UserIdTestData.userId, messageId)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `when repository gets message body and user address is null then a decryption error is returned`() =
        runTest {
            // Given
            val expected = DataError.Local.DecryptionError(MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY).left()
            coEvery {
                userAddressManager.getAddress(UserIdTestData.userId, MessageTestData.message.addressId)
            } returns null

            // When
            val actual = getMessageBody(UserIdTestData.userId, messageId)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `when repository method returns an error then the use case returns the error`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        coEvery {
            messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual)
    }
}
