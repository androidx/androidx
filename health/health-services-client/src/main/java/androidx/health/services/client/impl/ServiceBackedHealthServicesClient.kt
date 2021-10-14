/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl

import android.content.Context
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.impl.IpcConstants.HEALTH_SERVICES_BIND_ACTION
import androidx.health.services.client.impl.IpcConstants.SERVICE_PACKAGE_NAME
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.ClientConfiguration

/**
 * A [HealthServicesClient] implementation.
 *
 * @hide
 */
public class ServiceBackedHealthServicesClient public constructor(context: Context) :
    HealthServicesClient,
    Client<IHealthServicesApiService>(
        CLIENT_CONFIGURATION,
        HsConnectionManager.getInstance(context),
        { binder -> IHealthServicesApiService.Stub.asInterface(binder) },
        { service -> service.apiVersion }
    ) {

    private val applicationContext = context.applicationContext

    override val exerciseClient: ExerciseClient
        get() = ServiceBackedExerciseClient.getClient(applicationContext)
    override val passiveMonitoringClient: PassiveMonitoringClient
        get() = ServiceBackedPassiveMonitoringClient(applicationContext)
    override val measureClient: MeasureClient
        get() = ServiceBackedMeasureClient.getClient(applicationContext)

    private companion object {
        private const val CLIENT = "HealthServicesClient"
        private val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, HEALTH_SERVICES_BIND_ACTION)
    }
}
