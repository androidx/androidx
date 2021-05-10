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

import android.app.PendingIntent
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.event.Event
import com.google.common.util.concurrent.ListenableFuture

/**
 * Client which provides a means to passively monitor data without requiring an ongoing workout.
 *
 * The lifetimes of registrations made through this client are independent of the lifetime of the
 * subscribing app. These registrations are therefore suitable for notifying of ongoing measurements
 * or triggered events, regardless of whether or not the subscribing app is currently running, in
 * the foreground or engaged in a workout.
 */
public interface PassiveMonitoringClient {
    /**
     * Subscribes for updates on a set of data types to be periodically delivered to the app.
     *
     * Data will be batched. Higher frequency updates are available through [ExerciseClient] or
     * [MeasureClient].
     *
     * The provided [PendingIntent] will be invoked periodically with the collected data.
     *
     * Subscribing apps are responsible for ensuring they can receive the [callbackIntent] by e.g.
     * declaring a suitable [android.content.BroadcastReceiver] in their app manifest.
     *
     * This registration is unique per subscribing app. Subsequent registrations will replace the
     * previous registration, if one had been made.
     */
    public fun registerDataCallback(
        dataTypes: Set<@JvmSuppressWildcards DataType>,
        callbackIntent: PendingIntent
    ): ListenableFuture<Void>

    /**
     * Subscribes an intent callback (the same way as [PassiveMonitoringClient.registerDataCallback]
     * ) and a [PassiveMonitoringCallback] for updates on a set of data types periodically.
     *
     * The provided [callback] will take priority in receiving updates as long the app is alive and
     * the callback can be successfully notified. Otherwise, updates will be delivered to the
     * [callbackIntent].
     *
     * This registration is unique per subscribing app. Subsequent registrations will replace the
     * previous registration, if one had been made.
     */
    public fun registerDataCallback(
        dataTypes: Set<@JvmSuppressWildcards DataType>,
        callbackIntent: PendingIntent,
        callback: PassiveMonitoringCallback
    ): ListenableFuture<Void>

    /**
     * Unregisters the subscription made by [PassiveMonitoringClient.registerDataCallback].
     *
     * The associated [PendingIntent] will be called one last time with any remaining buffered data.
     */
    public fun unregisterDataCallback(): ListenableFuture<Void>

    /**
     * Registers for notification of the [event] being triggered.
     *
     * The provided [PendingIntent] will be sent whenever [event] is triggered.
     *
     * Subscribing apps are responsible for ensuring they can receive the [callbackIntent] by e.g.
     * declaring a suitable [android.content.BroadcastReceiver] in their app manifest.
     *
     * Registration of multiple events is possible except where there already exists an event that
     * is equal, as per the definition of [Event.equals], in which case the existing registration
     * for that event will be replaced.
     */
    public fun registerEventCallback(
        event: Event,
        callbackIntent: PendingIntent
    ): ListenableFuture<Void>

    /** Unregisters the subscription for the given [Event]. */
    public fun unregisterEventCallback(event: Event): ListenableFuture<Void>

    /** Returns the [PassiveMonitoringCapabilities] of this client for the device. */
    public val capabilities: ListenableFuture<PassiveMonitoringCapabilities>
}
