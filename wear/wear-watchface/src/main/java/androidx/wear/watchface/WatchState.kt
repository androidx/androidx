/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface

import android.app.NotificationManager
import androidx.annotation.RestrictTo

class WatchState(
    /**
     * The current user interruption settings. See [NotificationManager]. Based on the value
     * the watch face should adjust the amount of information it displays. For example, if it
     * displays the number of pending emails, it should hide it if interruptionFilter is equal to
     * [NotificationManager.INTERRUPTION_FILTER_NONE]. `interruptionFilter` can be
     * [NotificationManager.INTERRUPTION_FILTER_NONE],
     * [NotificationManager.INTERRUPTION_FILTER_PRIORITY],
     * [NotificationManager.INTERRUPTION_FILTER_ALL],
     * [NotificationManager.INTERRUPTION_FILTER_ALARMS], or
     * [NotificationManager.INTERRUPTION_FILTER_UNKNOWN].
     */
    val interruptionFilter: ObservableWatchData<Int>,

    /**
     * Whether or not the watch is in ambient mode. The watch face should switch to a simplified low
     * intensity display when in ambient mode. E.g. if the watch face displays seconds, it should
     * hide them in ambient mode.
     */
    val isAmbient: ObservableWatchData<Boolean>,

    /**
     * Whether or not the watch is in airplane mode. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val inAirplaneMode: ObservableWatchData<Boolean>,

    /**
     * Whether or not we should conserve power due to a low battery which isn't charging. Only
     * valid if [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is
     * true.
     *
     * @hide
     */
    val isBatteryLowAndNotCharging: ObservableWatchData<Boolean>,

    /**
     * Whether or not the watch is charging. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val isCharging: ObservableWatchData<Boolean>,

    /**
     * Whether or not the watch is connected to the companion phone. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val isConnectedToCompanion: ObservableWatchData<Boolean>,

    /**
     * Whether or not GPS is active on the watch. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val isGpsActive: ObservableWatchData<Boolean>,

    /**
     * Whether or not the watch's keyguard (lock screen) is locked. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val isKeyguardLocked: ObservableWatchData<Boolean>,

    /**
     * Whether or not the watch is in theater mode. Only valid if
     * [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
     *
     * @hide
     */
    val isInTheaterMode: ObservableWatchData<Boolean>,

    /** Whether or not the watch face is visible. */
    val isVisible: ObservableWatchData<Boolean>,

    /** The total number of notification cards in the stream. */
    val notificationCount: ObservableWatchData<Int>,

    /** The total number of unread notification cards in the stream. */
    val unreadNotificationCount: ObservableWatchData<Int>,

    /** Whether or not the watch hardware supports low bit ambient support. */
    val hasLowBitAmbient: Boolean,

    /** Whether or not the watch hardware supports burn in protection. */
    val hasBurnInProtection: Boolean
)

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MutableWatchState {
    var interruptionFilter = MutableObservableWatchData<Int>()
    val isAmbient = MutableObservableWatchData<Boolean>()
    val inAirplaneMode = MutableObservableWatchData<Boolean>()
    val isBatteryLowAndNotCharging = MutableObservableWatchData<Boolean>()
    val isCharging = MutableObservableWatchData<Boolean>()
    val isConnectedToCompanion = MutableObservableWatchData<Boolean>()
    val isGpsActive = MutableObservableWatchData<Boolean>()
    val isKeyguardLocked = MutableObservableWatchData<Boolean>()
    val isInTheaterMode = MutableObservableWatchData<Boolean>()
    val isVisible = MutableObservableWatchData<Boolean>()
    val notificationCount = MutableObservableWatchData<Int>()
    val unreadNotificationCount = MutableObservableWatchData<Int>()
    var hasLowBitAmbient = false
    var hasBurnInProtection = false

    fun asWatchState() = WatchState(
        interruptionFilter = interruptionFilter,
        isAmbient = isAmbient,
        inAirplaneMode = inAirplaneMode,
        isBatteryLowAndNotCharging = isBatteryLowAndNotCharging,
        isCharging = isCharging,
        isConnectedToCompanion = isConnectedToCompanion,
        isGpsActive = isGpsActive,
        isKeyguardLocked = isKeyguardLocked,
        isInTheaterMode = isInTheaterMode,
        isVisible = isVisible,
        notificationCount = notificationCount,
        unreadNotificationCount = unreadNotificationCount,
        hasLowBitAmbient = hasLowBitAmbient,
        hasBurnInProtection = hasBurnInProtection
    )
}