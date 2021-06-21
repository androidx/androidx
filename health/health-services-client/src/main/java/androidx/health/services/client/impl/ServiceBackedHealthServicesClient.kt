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
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.ipc.internal.ConnectionManager

/**
 * A [HealthServicesClient] implementation.
 *
 * @hide
 */
public class ServiceBackedHealthServicesClient
internal constructor(context: Context, connectionManager: ConnectionManager) :
    HealthServicesClient {

    private val applicationContext: Context = context.applicationContext
    private val ipcClient: HealthServicesIpcClient = HealthServicesIpcClient(connectionManager)

    public constructor(context: Context) : this(context, HsConnectionManager.getInstance(context))

    override val exerciseClient: ExerciseClient
        get() = ServiceBackedExerciseClient.getClient(applicationContext)
    override val passiveMonitoringClient: PassiveMonitoringClient
        get() = ServiceBackedPassiveMonitoringClient(applicationContext)
    override val measureClient: MeasureClient
        get() = ServiceBackedMeasureClient.getClient(applicationContext)
}
