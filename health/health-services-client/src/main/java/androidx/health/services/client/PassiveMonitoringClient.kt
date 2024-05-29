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

package androidx.health.services.client

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * Client which provides a means to passively monitor data without requiring an ongoing workout.
 *
 * The lifetimes of registrations made through this client are independent of the lifetime of the
 * subscribing app. These registrations are therefore suitable for notifying of ongoing measurements
 * or triggered passive goals, regardless of whether or not the subscribing app is currently
 * running, in the foreground or engaged in a workout.
 */
public interface PassiveMonitoringClient {
    /**
     * Subscribes for updates to be periodically delivered to the app.
     *
     * Data updates will be batched and delivered from the point of initial registration and will
     * continue to be delivered until the [DataType] is unregistered, either by explicitly calling
     * [clearPassiveListenerServiceAsync] or by registering again without that [DataType] included
     * in the request. Higher frequency updates are available through [ExerciseClient] or
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
     * @return a [ListenableFuture] that completes when the registration succeeds in Health Services
     *   or fails when the request is not supported on the current device or the client does not
     *   have the required permissions for the request
     */
    public fun setPassiveListenerServiceAsync(
        service: Class<out PassiveListenerService>,
        config: PassiveListenerConfig
    ): ListenableFuture<Void>

    /**
     * Subscribes for updates to be periodically delivered to the app via bound callback.
     *
     * Data updates are sent on generation (they will not be batched) and will be delivered until
     * the [DataType] is unregistered, either by explicitly calling
     * [clearPassiveListenerCallbackAsync] or by registering again without that [DataType] included
     * in the request. Higher frequency updates are available through [ExerciseClient] or
     * [MeasureClient].
     *
     * The provided [callback] will take priority in receiving updates as long the app is alive and
     * the callback can be successfully notified. Otherwise, the request will automatically be
     * unregistered.
     *
     * This registration is unique per subscribing app and operates independently from
     * [setPassiveListenerServiceAsync] (each channel will receive its own stream of data).
     * Subsequent registrations will replace the previous registration, if one had been made. The
     * client is responsible for ensuring that their requested [PassiveListenerConfig] is supported
     * on this device by checking the [PassiveMonitoringCapabilities]. The returned future will fail
     * if the request is not supported on the current device or the client does not have the
     * required permissions for the request.
     *
     * This call completes when the registration succeeds in Health Services. If the request is not
     * supported on this device or the calling app lacks permissions, then
     * [PassiveListenerCallback.onRegistrationFailed] will be invoked on the provided [callback].
     *
     * @param callback the [PassiveListenerCallback] that will receive updates on the main thread
     * @param config the requested [PassiveListenerConfig] from the client
     */
    public fun setPassiveListenerCallback(
        config: PassiveListenerConfig,
        callback: PassiveListenerCallback
    )

    /**
     * Subscribes for updates to be periodically delivered to the app via bound callback.
     *
     * Data updates are sent on generation (they will not be batched) and will be delivered until
     * the [DataType] is unregistered, either by explicitly calling
     * [clearPassiveListenerCallbackAsync] or by registering again without that [DataType] included
     * in the request. Higher frequency updates are available through [ExerciseClient] or
     * [MeasureClient].
     *
     * The provided [callback] will take priority in receiving updates as long the app is alive and
     * the callback can be successfully notified. Otherwise, the request will automatically be
     * unregistered.
     *
     * This registration is unique per subscribing app and operates independently from
     * [setPassiveListenerServiceAsync] (each channel will receive its own stream of data).
     * Subsequent registrations will replace the previous registration, if one had been made. The
     * client is responsible for ensuring that their requested [PassiveListenerConfig] is supported
     * on this device by checking the [PassiveMonitoringCapabilities]. The returned future will fail
     * if the request is not supported on the current device or the client does not have the
     * required permissions for the request.
     *
     * This call completes when the registration succeeds in Health Services. If the request is not
     * supported on this device or the calling app lacks permissions, then
     * [PassiveListenerCallback.onRegistrationFailed] will be invoked on the provided [callback].
     *
     * @param callback the [PassiveListenerCallback] that will receive updates
     * @param executor the [Executor] on which the [callback] will be invoked
     * @param config the requested [PassiveListenerConfig] from the client
     */
    public fun setPassiveListenerCallback(
        config: PassiveListenerConfig,
        executor: Executor,
        callback: PassiveListenerCallback
    )

    /**
     * Unregisters the subscription made by [setPassiveListenerServiceAsync].
     *
     * Data will not be delivered after this call so if clients care about any pending batched data
     * they should call flush before unregistering.
     *
     * @return a [ListenableFuture] that completes when the un-registration succeeds in Health
     *   Services. This is a no-op if the callback has already been unregistered
     */
    public fun clearPassiveListenerServiceAsync(): ListenableFuture<Void>

    /**
     * Unregisters the subscription made by [setPassiveListenerCallback].
     *
     * Data will not be delivered after this call so if clients care about any pending batched data
     * they should call flush before unregistering.
     *
     * @return a [ListenableFuture] that completes when the un-registration succeeds in Health
     *   Services. This is a no-op if the callback has already been unregistered
     */
    public fun clearPassiveListenerCallbackAsync(): ListenableFuture<Void>

    /**
     * Flushes the sensors for the registered [DataType]s.
     *
     * If no listener has been registered by this client, this will be a no-op. This call should be
     * used sparingly and will be subject to throttling by Health Services.
     *
     * @return a [ListenableFuture] that will complete when the flush is finished
     */
    public fun flushAsync(): ListenableFuture<Void>

    /**
     * Returns the [PassiveMonitoringCapabilities] of this client for this device.
     *
     * This can be used to determine what [DataType]s this device supports for passive monitoring
     * and goals. Clients should use the capabilities to inform their requests since Health Services
     * will typically reject requests made for [DataType]s which are not supported.
     *
     * @return a [ListenableFuture] containing the [PassiveMonitoringCapabilities] for this device
     */
    public fun getCapabilitiesAsync(): ListenableFuture<PassiveMonitoringCapabilities>
}
