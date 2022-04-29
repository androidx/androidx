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

import android.content.BroadcastReceiver
import android.content.ComponentName
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.PassiveMonitoringConfig
import androidx.health.services.client.data.PassiveMonitoringUpdate
import com.google.common.util.concurrent.ListenableFuture

/**
 * Client which provides a means to passively monitor data without requiring an ongoing workout.
 *
 * The lifetimes of registrations made through this client are independent of the lifetime of the
 * subscribing app. These registrations are therefore suitable for notifying of ongoing measurements
 * or triggered passive goals, regardless of whether or not the subscribing app is currently
 * running, in the foreground or engaged in a workout.
 */
// TODO(b/227475943): Remove old registration methods, add new registration methods, ensure new
//  registration methods do not return a ListenableFuture
public interface PassiveMonitoringClient {
    /**
     * Subscribes for updates on a set of data types to be periodically delivered to the app.
     *
     * Data will be batched and delivered from the point of initial registration and will continue
     * to be delivered until the [DataType] is unregistered, either by explicitly calling
     * [unregisterDataCallbackAsync] or by registering again without that [DataType] included in the
     * request. Higher frequency updates are available through [ExerciseClient] or [MeasureClient].
     *
     * The data will be broadcast to the provided [ComponentName] periodically to the action:
     * [PassiveMonitoringUpdate.ACTION_DATA]. A [PassiveMonitoringUpdate] can be extracted from the
     * intent using [PassiveMonitoringUpdate.fromIntent].
     *
     * Subscribing apps are responsible for ensuring they can receive the intent by e.g. declaring a
     * suitable [BroadcastReceiver] in their app manifest.
     *
     * This registration is unique per subscribing app. Subsequent registrations will replace the
     * previous registration, if one had been made. The client is responsible for ensuring that
     * their requested [PassiveMonitoringConfig] is supported on this device by checking the
     * [PassiveMonitoringCapabilities]. The returned future will fail if the request is not
     * supported on the current device or the client does not have the required permissions for the
     * request.
     */
    public fun registerDataCallbackAsync(
        configuration: PassiveMonitoringConfig
    ): ListenableFuture<Void>

    /**
     * Subscribes an intent callback (the same way as
     * [PassiveMonitoringClient.registerDataCallbackAsync]) and a [PassiveMonitoringCallback] for
     * updates on a set of data types periodically.
     *
     * The provided [callback] will take priority in receiving updates as long the app is alive and
     * the callback can be successfully notified. Otherwise, updates will be delivered via Intent to
     * the [PassiveMonitoringConfig.componentName] with the [PassiveMonitoringUpdate.ACTION_DATA].
     *
     * This registration is unique per subscribing app. Subsequent registrations will replace the
     * previous registration, if one had been made.
     */
    public fun registerDataCallbackAsync(
        configuration: PassiveMonitoringConfig,
        callback: PassiveMonitoringCallback
    ): ListenableFuture<Void>

    /**
     * Unregisters the subscription made by [PassiveMonitoringClient.registerDataCallbackAsync].
     *
     * The [android.content.Intent] will be broadcast to the [ComponentName] one last time with any
     * remaining buffered data.
     */
    public fun unregisterDataCallbackAsync(): ListenableFuture<Void>

    /**
     * Registers for notification of the [passiveGoal] being triggered.
     *
     * An Intent will be broadcast to the provided [ComponentName] with the action
     * [PassiveGoal.ACTION_GOAL] whenever the [passiveGoal] is triggered.
     *
     * Subscribing apps are responsible for ensuring they can receive the intent by e.g. declaring a
     * suitable [BroadcastReceiver] in their app manifest.
     *
     * Registration of multiple passive goals is possible except where there already exists an
     * passive goal that is equal, as per the definition of [PassiveGoal.equals], in which case the
     * existing registration for that passive goal will be replaced.
     */
    public fun <T : BroadcastReceiver> registerPassiveGoalCallbackAsync(
        passiveGoal: PassiveGoal,
        broadcastReceiver: Class<T>,
    ): ListenableFuture<Void>

    /** Unregisters the subscription for the given [PassiveGoal]. */
    public fun unregisterPassiveGoalCallbackAsync(passiveGoal: PassiveGoal): ListenableFuture<Void>

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
    public val capabilities: ListenableFuture<PassiveMonitoringCapabilities>
}
