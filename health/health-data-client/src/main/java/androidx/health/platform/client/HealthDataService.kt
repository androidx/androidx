/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.health.platform.client

import android.content.Context
import androidx.health.platform.client.impl.ServiceBackedHealthDataClient
import androidx.health.platform.client.impl.ipc.ClientConfiguration

/**
 * Entry point for connecting to Health Data Provider on the device and creating instances of
 * [HealthDataAsyncClient].
 */
object HealthDataService {

    private const val ANDROID_HEALTH_PLATFORM_CLIENT_NAME = "HealthData"
    private const val ANDROID_HEALTH_PLATFORM_PROVIDER_PACKAGE =
        "com.google.android.apps.healthdata"
    private const val ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION =
        "androidx.health.ACTION_BIND_HEALTH_DATA_SERVICE"

    /**
     * Creates an IPC-backed [HealthDataAsyncClient] instance binding to the default implementation.
     */
    @SuppressWarnings("RestrictedApi")
    fun getClient(context: Context): HealthDataAsyncClient {
        val configuration =
            ClientConfiguration(
                ANDROID_HEALTH_PLATFORM_CLIENT_NAME,
                ANDROID_HEALTH_PLATFORM_PROVIDER_PACKAGE,
                ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION
            )
        return ServiceBackedHealthDataClient(context, configuration)
    }

    /**
     * Creates an IPC-backed [HealthDataAsyncClient] instance binding to the Health Data Provider
     * with the given (`servicePackageName`), using the specified (`bindAction`).
     *
     * ```
     * HealthDataClient client =
     *         HealthDataService.getClient(
     *             context,
     *             "HealthDataProvider",
     *             "com.google.android.apps.healthdata",
     *             "com.google.android.apps.healthdata.service.HealthCoreService")
     * ```
     */
    @SuppressWarnings("RestrictedApi")
    fun getClient(
        context: Context,
        clientName: String,
        servicePackageName: String,
        bindAction: String,
    ): HealthDataAsyncClient {
        require(!servicePackageName.isEmpty()) { "Service package name must not be empty." }
        val configuration = ClientConfiguration(clientName, servicePackageName, bindAction)
        return ServiceBackedHealthDataClient(context, configuration)
    }

    fun getClient(context: Context, enabledPackage: String): HealthDataAsyncClient {
        return getClient(
            context,
            clientName = ANDROID_HEALTH_PLATFORM_CLIENT_NAME,
            servicePackageName = enabledPackage,
            bindAction = ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION
        )
    }
}
