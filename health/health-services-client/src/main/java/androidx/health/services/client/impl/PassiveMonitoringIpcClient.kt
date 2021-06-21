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

import android.os.IBinder
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.Client.VersionGetter
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.ServiceOperation
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import com.google.common.util.concurrent.ListenableFuture

/**
 * An IPC Client that connects to and communicates with Health Services to make Passive Monitoring
 * API calls.
 *
 * @hide
 */
public class PassiveMonitoringIpcClient(connectionManager: ConnectionManager) :
    Client(
        CLIENT_CONFIGURATION,
        connectionManager,
        VersionGetter { binder -> getServiceInterface(binder).apiVersion }
    ) {

    public override fun <T> execute(operation: ServiceOperation<T>): ListenableFuture<T> {
        return super.execute(operation)
    }

    public companion object {
        public const val SERVICE_BIND_ACTION: String =
            "com.google.android.wearable.healthservices.PassiveMonitoringClient"
        private const val CLIENT = "HealthServicesPassiveMonitoringClient"
        private const val SERVICE_PACKAGE_NAME = "com.google.android.wearable.healthservices"
        private val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, SERVICE_BIND_ACTION)

        @JvmStatic
        internal fun getServiceInterface(binder: IBinder): IPassiveMonitoringApiService {
            return IPassiveMonitoringApiService.Stub.asInterface(binder)
        }
    }
}
