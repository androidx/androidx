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

import android.annotation.SuppressLint
import android.app.NotificationManager
import androidx.annotation.UiThread

/** An observer for changes in system state which are relevant to watch faces. */
class WatchState {
    /** The current user interruption settings. See {@link NotificationManager}. */
    var interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
        private set

    /** Whether or not the watch is in ambient mode. */
    var isAmbient = false
        private set

    /**
     * Whether or not the watch is in airplane mode. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    @get:JvmName("inAirplaneMode")
    var inAirplaneMode = false
        private set

    /**
     * Whether or not we should conserve power due to low battery. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isBatteryLowAndNotCharging = false
        private set

    /**
     * Whether or not the watch is charging. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isCharging = false
        private set

    /**
     * Whether or not the watch is connected to the companion phone. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isConnectedToCompanion = false
        private set

    /**
     * Whether or not GPS is active on the watch. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isGpsActive = false
        private set

    /**
     * Whether or not the watch's keyguard (lock screen) is locked. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isKeyguardLocked = false
        private set

    /**
     * Whether or not the watch is in theater mode. Only valid if
     * {@link android.support.wearable.watchface.WatchFaceStyle#hideNotificationIndicator} is true.
     */
    var isInTheaterMode = false
        private set

    /** Whether or not the watch face is visible. */
    var isVisible = false
        private set

    /** The total number of notification cards in the stream. */
    var notificationCount = 0
        private set

    /** The total number of unread notification cards in the stream. */
    var unreadNotificationCount = 0
        private set

    /** Whether or not the watch has low bit ambient support. */
    @get:JvmName("hasLowBitAmbient")
    var hasLowBitAmbient = false
        private set

    /** Whether or not the watch has burn in protection support. */
    @get:JvmName("hasBurnInProtection")
    var hasBurnInProtection = false
        private set

    private val mListeners = HashSet<Listener>()

    interface Listener {
        /**
         * Called when the device enters or exits ambient mode. The watch face should switch to a
         * simplified low intensity display when in ambient mode. E.g. if the watch face displays
         * seconds, it should hide them in ambient mode.
         *
         * @param isAmbient Whether or not the device is in ambient mode
         */
        @UiThread
        fun onAmbientModeChanged(isAmbient: Boolean) {}

        /**
         * Called when the device enters or exits airplane mode.
         *
         * @param inAirplaneMode Whether or not the device is in airplane mode
         */
        @UiThread
        fun onInAirplaneModeChanged(inAirplaneMode: Boolean) {}

        /**
         * Called when the devices enters or exits a condition where the battery is low and not
         * charging.
         *
         * @param isBatteryLowAndNotCharging Whether or not the battery is low and not charging
         */
        @UiThread
        fun onIsBatteryLowAndNotCharging(isBatteryLowAndNotCharging: Boolean) {}

        /**
         * Called when the device starts or stops charging.
         *
         * @param isCharging Whether or not the device is charging
         */
        @UiThread
        fun onIsChargingChanged(isCharging: Boolean) {}

        /**
         * Called when the device connects or disconnects with the companion phone.
         *
         * @param isConnectedToCompanion Whether or not the device is connected to the phone
         */
        @UiThread
        fun onIsConnectedToCompanionChanged(isConnectedToCompanion: Boolean) {}

        /**
         * Called when GPS is enabled or disabled.
         *
         * @param isGpsActive Whether or not GPS is enabled
         */
        @UiThread
        fun onIsGpsActiveChanged(isGpsActive: Boolean) {}

        /**
         * Called when the keyguard (lock screen) is locked or unlocked.
         *
         * @param isKeyguardLocked Whether or not the keyguard (lock screen) is locked
         */
        @UiThread
        fun onIsKeyguardLockedChanged(isKeyguardLocked: Boolean) {}

        /**
         * Called when the user changes the interruption filter. The watch face should adjust the amount
         * of information it displays. For example, if it displays the number of pending emails,
         * it should hide it if interruptionFilter is equal to
         * {@link NotificationManager.INTERRUPTION_FILTER_NONE}. {@code interruptionFilter} can be
         * {@link NotificationManager.INTERRUPTION_FILTER_NONE}, {@link
         * NotificationManager.INTERRUPTION_FILTER_PRIORITY},
         * {@link NotificationManager.INTERRUPTION_FILTER_ALL},
         * {@link NotificationManagerINTERRUPTION_FILTER_ALARMS}, or
         * {@link NotificationManager.INTERRUPTION_FILTER_UNKNOWN}.
         *
         * @param interruptionFilter The current interruption filter set by the user
         */
        @UiThread
        fun onInterruptionFilterChanged(interruptionFilter: Int) {}

        /**
         * Called when the device enters or exits theater mode.
         *
         * @param inTheaterMode Whether or not the device is in theater mode
         */
        @UiThread
        fun onInTheaterModeChanged(inTheaterMode: Boolean) {}

        /**
         * Called when the total number of notification cards in the stream has changed.
         *
         * @param notificationCount total number of the notification cards in the stream
         */
        @UiThread
        fun onNotificationCountChanged(notificationCount: Int) {}

        /**
         * Called when the number of unread notification cards in the stream has changed.
         *
         * @param unreadNotificationCount number of the notification cards in the stream that
         *       haven't yet been seen by the user
         */
        @UiThread
        fun onUnreadNotificationCountChanged(unreadNotificationCount: Int) {}

        /**
         * Called to inform you of the watch face becoming visible or hidden. Note the
         * {@link WatchFace} automatically schedules draw calls as necessary.
         *
         * @param visible Whether the watch face is visible or hidden
         */
        @UiThread
        fun onVisibilityChanged(visible: Boolean) {}

        /**
         * Called when we've determined if the device has burn in protection or not.
         *
         * @param hasBurnInProtection Whether or not the device has burn in protection
         */
        @UiThread
        fun onHasBurnInProtectionSet(hasBurnInProtection: Boolean) {}

        /**
         * Called when we've determined if the device has a lower bit depth in ambient mode or not.
         *
         * @param hasLowBitAmbient Whether or not the device has a lower bit depth in ambient mode
         */
        @UiThread
        fun onHasLowBitAmbientSet(hasLowBitAmbient: Boolean) {}
    }

    /**
     * Adds a {@link Listener} which will be called every time the system state changes.
     *
     * @param listener The {@link Listener} to add
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    /**
     * Removes a {@link Listener} previously added by {@link addListener}.
     *
     * @param listener The {@link #Listener} to remove
     */
    @UiThread
    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }

    @UiThread
    internal fun onAmbientModeChanged(isAmbient: Boolean) {
        this.isAmbient = isAmbient
        for (listener in mListeners) {
            listener.onAmbientModeChanged(isAmbient)
        }
    }

    @UiThread
    internal fun onInAirplaneModeChanged(inAirplaneMode: Boolean) {
        this.inAirplaneMode = inAirplaneMode
        for (listener in mListeners) {
            listener.onInAirplaneModeChanged(inAirplaneMode)
        }
    }

    @UiThread
    internal fun onIsBatteryLowAndNotCharging(isBatteryLowAndNotCharging: Boolean) {
        this.isBatteryLowAndNotCharging = isBatteryLowAndNotCharging
        for (listener in mListeners) {
            listener.onIsBatteryLowAndNotCharging(isBatteryLowAndNotCharging)
        }
    }

    @UiThread
    internal fun onIsChargingChanged(isCharging: Boolean) {
        this.isCharging = isCharging
        for (listener in mListeners) {
            listener.onIsChargingChanged(isCharging)
        }
    }

    @UiThread
    internal fun onIsConnectedToCompanionChanged(isConnectedToCompanion: Boolean) {
        this.isConnectedToCompanion = isConnectedToCompanion
        for (listener in mListeners) {
            listener.onIsConnectedToCompanionChanged(isConnectedToCompanion)
        }
    }

    @UiThread
    internal fun onIsGpsActiveChanged(isGpsActive: Boolean) {
        this.isGpsActive = isGpsActive
        for (listener in mListeners) {
            listener.onIsGpsActiveChanged(isGpsActive)
        }
    }

    @UiThread
    internal fun onIsKeyguardLockedChanged(isKeyguardLocked: Boolean) {
        this.isKeyguardLocked = isKeyguardLocked
        for (listener in mListeners) {
            listener.onIsKeyguardLockedChanged(isKeyguardLocked)
        }
    }

    @UiThread
    internal fun onInterruptionFilterChanged(interruptionFilter: Int) {
        this.interruptionFilter = interruptionFilter
        for (listener in mListeners) {
            listener.onInterruptionFilterChanged(interruptionFilter)
        }
    }

    @UiThread
    internal fun onInTheaterModeChanged(inTheaterMode: Boolean) {
        this.isInTheaterMode = inTheaterMode
        for (listener in mListeners) {
            listener.onInTheaterModeChanged(inTheaterMode)
        }
    }

    @UiThread
    internal fun onNotificationCountChanged(notificationCount: Int) {
        this.notificationCount = notificationCount
        for (listener in mListeners) {
            listener.onNotificationCountChanged(notificationCount)
        }
    }

    @UiThread
    internal fun onUnreadNotificationCountChanged(unreadNotificationCount: Int) {
        this.unreadNotificationCount = unreadNotificationCount
        for (listener in mListeners) {
            listener.onUnreadNotificationCountChanged(unreadNotificationCount)
        }
    }

    @UiThread
    internal fun onVisibilityChanged(visible: Boolean) {
        this.isVisible = visible
        for (listener in mListeners) {
            listener.onVisibilityChanged(visible)
        }
    }

    @UiThread
    internal fun setHasBurnInProtection(hasBurnInProtection: Boolean) {
        this.hasBurnInProtection = hasBurnInProtection
        for (listener in mListeners) {
            listener.onHasBurnInProtectionSet(hasBurnInProtection)
        }
    }

    @UiThread
    internal fun setHasLowBitAmbient(hasLowBitAmbient: Boolean) {
        this.hasLowBitAmbient = hasLowBitAmbient
        for (listener in mListeners) {
            listener.onHasLowBitAmbientSet(hasLowBitAmbient)
        }
    }
}
