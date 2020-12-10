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

/**
 * Describes the current state of the wearable including some hardware details such as whether or
 * not it supports burn in prevention and low-bit ambient.
 */
public class WatchState(
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
    public val interruptionFilter: ObservableWatchData<Int>,

    /**
     * Whether or not the watch is in ambient mode. The watch face should switch to a simplified low
     * intensity display when in ambient mode. E.g. if the watch face displays seconds, it should
     * hide them in ambient mode.
     */
    public val isAmbient: ObservableWatchData<Boolean>,

    /**
     * Whether or not we should conserve power due to a low battery which isn't charging. Only
     * valid if [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is
     * true.
     *
     * @hide
     */
    public val isBatteryLowAndNotCharging: ObservableWatchData<Boolean>,

    /** Whether or not the watch face is visible. */
    public val isVisible: ObservableWatchData<Boolean>,

    /** Whether or not the watch hardware supports low bit ambient support. */
    @get:JvmName("hasLowBitAmbient")
    public val hasLowBitAmbient: Boolean,

    /** Whether or not the watch hardware supports burn in protection. */
    @get:JvmName("hasBurnInProtection")
    public val hasBurnInProtection: Boolean,

    /** UTC reference time for previews of analog watch faces in milliseconds since the epoch. */
    public val analogPreviewReferenceTimeMillis: Long,

    /** UTC reference time for previews of digital watch faces in milliseconds since the epoch. */
    public val digitalPreviewReferenceTimeMillis: Long
)

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableWatchState {
    public var interruptionFilter: MutableObservableWatchData<Int> = MutableObservableWatchData()
    public val isAmbient: MutableObservableWatchData<Boolean> = MutableObservableWatchData()
    public val isBatteryLowAndNotCharging: MutableObservableWatchData<Boolean> =
        MutableObservableWatchData()
    public val isVisible: MutableObservableWatchData<Boolean> = MutableObservableWatchData()
    public var hasLowBitAmbient: Boolean = false
    public var hasBurnInProtection: Boolean = false
    public var analogPreviewReferenceTimeMillis: Long = 0
    public var digitalPreviewReferenceTimeMillis: Long = 0

    public fun asWatchState(): WatchState = WatchState(
        interruptionFilter = interruptionFilter,
        isAmbient = isAmbient,
        isBatteryLowAndNotCharging = isBatteryLowAndNotCharging,
        isVisible = isVisible,
        hasLowBitAmbient = hasLowBitAmbient,
        hasBurnInProtection = hasBurnInProtection,
        analogPreviewReferenceTimeMillis = analogPreviewReferenceTimeMillis,
        digitalPreviewReferenceTimeMillis = digitalPreviewReferenceTimeMillis
    )
}