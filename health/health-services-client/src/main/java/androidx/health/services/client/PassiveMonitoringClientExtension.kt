/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.health.services.client

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.PassiveMonitoringCapabilities

/**
 * Subscribes for updates to be periodically delivered to the app.
 *
 * Data updates will be batched and delivered from the point of initial registration and will
 * continue to be delivered until the [DataType] is unregistered, either by explicitly calling
 * [clearPassiveListenerService] or by registering again without that [DataType]
 * included in the request. Higher frequency updates are available through [ExerciseClient] or
 * [MeasureClient]. Any requested goal, user activity, or health event updates will not be
 * batched.
 *
 * Health Services will automatically bind to the provided [PassiveListenerService] to send the
 * update. Clients are responsible for defining the service in their app manifest. They should
 * also require the `com.google.android.wearable.healthservices.permission.PASSIVE_DATA_BINDING`
 * permission in their app manifest service definition in order to ensure that Health Services
 * is the source of the binding.
 *
 * This registration is unique per subscribing app. Subsequent registrations will replace the
 * previous registration, if one had been made. The client is responsible for ensuring that
 * their requested [PassiveListenerConfig] is supported on this device by checking the
 * [PassiveMonitoringCapabilities]. The returned future will fail if the request is not
 * supported on the current device or the client does not have the required permissions for the
 * request.
 *
 * @param service the [PassiveListenerService] to bind to
 * @param config the [PassiveListenerConfig] from the client
 * @throws HealthServicesException if Health Service fails to process the call
 * @throws SecurityException if calling app does not have the required permissions
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun PassiveMonitoringClient.setPassiveListenerService(
    service: Class<out PassiveListenerService>,
    config: PassiveListenerConfig
) = setPassiveListenerServiceAsync(service, config).awaitWithException()

/**
 * Unregisters the subscription made by [setPassiveListenerService].
 *
 * Data will not be delivered after this call so if clients care about any pending batched data
 * they should call flush before unregistering.
 *
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun PassiveMonitoringClient.clearPassiveListenerService() =
    clearPassiveListenerServiceAsync().awaitWithException()

/**
 * Unregisters the subscription made by [PassiveMonitoringClient.setPassiveListenerCallback].
 *
 * Data will not be delivered after this call so if clients care about any pending batched data
 * they should call flush before unregistering.
 *
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun PassiveMonitoringClient.clearPassiveListenerCallback() =
    clearPassiveListenerCallbackAsync().awaitWithException()

/**
 * Flushes the sensors for the registered [DataType]s.
 *
 * If no listener has been registered by this client, this will be a no-op. This call should be
 * used sparingly and will be subject to throttling by Health Services.
 *
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun PassiveMonitoringClient.flush() = flushAsync().awaitWithException()

/**
 * Returns the [PassiveMonitoringCapabilities] of this client for this device.
 *
 * This can be used to determine what [DataType]s this device supports for passive monitoring
 * and goals. Clients should use the capabilities to inform their requests since Health Services
 * will typically reject requests made for [DataType]s which are not supported.
 *
 * @return a [PassiveMonitoringCapabilities] for this device
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun PassiveMonitoringClient.getCapabilities() =
    getCapabilitiesAsync().awaitWithException()
