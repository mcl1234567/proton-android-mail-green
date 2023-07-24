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

package ch.protonmail.android.mailnotifications.data.remote.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailnotifications.data.remote.DeviceServiceApi
import ch.protonmail.android.mailnotifications.data.remote.fcm.model.KEY_PM_REGISTRATION_WORKER_ERROR
import ch.protonmail.android.mailnotifications.data.remote.resource.RegisterDeviceRequest
import ch.protonmail.android.mailnotifications.domain.FcmTokenPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiManager
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.takeIfNotBlank

/**
 * A CoroutineWorker that handles device registration on Proton servers.
 */
@HiltWorker
class RegisterDeviceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val fcmTokenPreferences: FcmTokenPreferences,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
            ?.let(::UserId)
            ?: return Result.failure(workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "User id not provided"))
        val fcmToken = fcmTokenPreferences.getToken().ifBlank {
            return Result.failure(workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "FCM token not found"))
        }
        val api: ApiManager<out DeviceServiceApi> = apiProvider.get(userId)
        val registerDeviceRequest = RegisterDeviceRequest(
            deviceToken = fcmToken,
            environment = DeviceEnvironmentAndroid
        )

        return when (val result = api { registerDevice(registerDeviceRequest) }) {
            is ApiResult.Success -> Result.success()

            is ApiResult.Error -> if (result.isRetryable()) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {

        private const val DeviceEnvironmentAndroid = 4
        internal const val RawUserIdKey = "pmRegistrationWorkerUserId"

        fun params(userId: UserId) = mapOf(
            RawUserIdKey to userId.id
        )
    }
}