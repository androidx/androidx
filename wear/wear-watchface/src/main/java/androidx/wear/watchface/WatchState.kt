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

import androidx.annotation.RestrictTo

class WatchState(
    /**
     * The current user interruption settings. See {@link NotificationManager}. Based on the value
     * the watch face should adjust the amount of information it displays. For example, if it
     * displays the number of pending emails, it should hide it if interruptionFilter is equal to
     * {@link NotificationManager.INTERRUPTION_FILTER_NONE}. {@code interruptionFilter} can be
     * {@link NotificationManager.INTERRUPTION_FILTER_NONE}, {@link
     * NotificationManager.INTERRUPTION_FILTER_PRIORITY},
     * {@link NotificationManager.INTERRUPTION_FILTER_ALL},
     * {@link NotificationManagerINTERRUPTION_FILTER_ALARMS}, or
     * {@link NotificationManager.INTERRUPTION_FILTER_UNKNOWN}.
     */
    val interruptionFilter: WatchData<Int>,

    /**
     * Whether or not the watch is in ambient mode. The watch face should switch to a simplified low
     * intensity display when in ambient mode. E.g. if the watch face displays seconds, it should
     * hide them in ambient mode.
     */
    val isAmbient: WatchData<Boolean>,

    /**
     * Whether or not the watch is in airplane mode. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val inAirplaneMode: WatchData<Boolean>,

    /**
     * Whether or not we should conserve power due to a low battery which isn't charging. Only
     * valid if {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator}
     * is true.
     *
     * @hide
     */
    val isBatteryLowAndNotCharging: WatchData<Boolean>,

    /**
     * Whether or not the watch is charging. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val isCharging: WatchData<Boolean>,

    /**
     * Whether or not the watch is connected to the companion phone. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val isConnectedToCompanion: WatchData<Boolean>,

    /**
     * Whether or not GPS is active on the watch. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val isGpsActive: WatchData<Boolean>,

    /**
     * Whether or not the watch's keyguard (lock screen) is locked. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val isKeyguardLocked: WatchData<Boolean>,

    /**
     * Whether or not the watch is in theater mode. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     *
     * @hide
     */
    val isInTheaterMode: WatchData<Boolean>,

    /** Whether or not the watch face is visible. */
    val isVisible: WatchData<Boolean>,

    /** The total number of notification cards in the stream. */
    val notificationCount: WatchData<Int>,

    /** The total number of unread notification cards in the stream. */
    val unreadNotificationCount: WatchData<Int>,

    /** Whether or not the watch has low bit ambient support. */
    val hasLowBitAmbient: WatchData<Boolean>,

    /** Whether or not the watch has burn in protection support. */
    val hasBurnInProtection: WatchData<Boolean>
)

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MutableWatchState {
    var interruptionFilter = MutableWatchData<Int>()
    val isAmbient = MutableWatchData<Boolean>()
    val inAirplaneMode = MutableWatchData<Boolean>()
    val isBatteryLowAndNotCharging = MutableWatchData<Boolean>()
    val isCharging = MutableWatchData<Boolean>()
    val isConnectedToCompanion = MutableWatchData<Boolean>()
    val isGpsActive = MutableWatchData<Boolean>()
    val isKeyguardLocked = MutableWatchData<Boolean>()
    val isInTheaterMode = MutableWatchData<Boolean>()
    val isVisible = MutableWatchData<Boolean>()
    val notificationCount = MutableWatchData<Int>()
    val unreadNotificationCount = MutableWatchData<Int>()
    val hasLowBitAmbient = MutableWatchData<Boolean>()
    val hasBurnInProtection = MutableWatchData<Boolean>()

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