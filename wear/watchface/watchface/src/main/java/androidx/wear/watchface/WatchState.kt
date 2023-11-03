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
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Describes the current state of the wearable including some hardware details such as whether or
 * not it supports burn in prevention and low-bit ambient.
 *
 * @param interruptionFilter The current user interruption settings. See [NotificationManager]. This
 *   is initially `null` because the watch face is created before the system has sent the state.
 *   Based on the value the watch face should adjust the amount of information it displays. For
 *   example, if it displays the number of pending emails, it should hide it if interruptionFilter
 *   is equal to [NotificationManager.INTERRUPTION_FILTER_NONE]. `interruptionFilter` can be
 *   [NotificationManager.INTERRUPTION_FILTER_NONE],
 *   [NotificationManager.INTERRUPTION_FILTER_PRIORITY],
 *   [NotificationManager.INTERRUPTION_FILTER_ALL],
 *   [NotificationManager.INTERRUPTION_FILTER_ALARMS], or
 *   [NotificationManager.INTERRUPTION_FILTER_UNKNOWN].
 * @param isAmbient Whether or not the watch is in ambient mode. This is initially `null` because
 *   the watch face is created before the system has sent the state. The order in which ambient vs
 *   style changes are reported is not guaranteed. Likewise the order of isAmbient flow callbacks
 *   and [Renderer.CanvasRenderer.render] or [Renderer.GlesRenderer.render] calls is not defined.
 *   For rendering please refer to [RenderParameters.drawMode] instead of isAmbient because you
 *   might receive requests for rendering non-ambient frames while the watch is ambient (e.g.
 *   editing from the companion phone).
 * @param isBatteryLowAndNotCharging Whether or not we should conserve power due to a low battery
 *   which isn't charging. This is initially `null` because the watch face is created before the
 *   system has sent the state. Only valid if
 *   [android.support.wearable.watchface.WatchFaceStyle.hideNotificationIndicator] is true.
 * @param isVisible Whether or not the watch face is visible. This is initially `null` because the
 *   watch face is created before the system has sent the state.
 * @param hasLowBitAmbient Whether or not the watch hardware supports low bit ambient support.
 * @param hasBurnInProtection Whether or not the watch hardware supports burn in protection.
 * @param analogPreviewReferenceTimeMillis UTC reference time for previews of analog watch faces in
 *   milliseconds since the epoch.
 * @param digitalPreviewReferenceTimeMillis UTC reference time for previews of digital watch faces
 *   in milliseconds since the epoch.
 * @param chinHeight the size, in pixels, of the chin or zero if the device does not have a chin. A
 *   chin is a section at the bottom of a circular display that is visible due to hardware
 *   limitations.
 * @param isHeadless Whether or not this is a headless watchface.
 * @param watchFaceInstanceId The ID associated with the watch face instance. Note there may be more
 *   than one instance associated with a [WatchFaceService]. See
 *   [androidx.wear.watchface.client.WatchFaceId] for more details.
 */
public class WatchState(
    public val interruptionFilter: StateFlow<Int?>,
    public val isAmbient: StateFlow<Boolean?>,
    public val isBatteryLowAndNotCharging: StateFlow<Boolean?>,
    public val isVisible: StateFlow<Boolean?>,
    @get:JvmName("hasLowBitAmbient") public val hasLowBitAmbient: Boolean,
    @get:JvmName("hasBurnInProtection") public val hasBurnInProtection: Boolean,
    public val analogPreviewReferenceTimeMillis: Long,
    public val digitalPreviewReferenceTimeMillis: Long,
    @Px @get:Px public val chinHeight: Int,
    public val isHeadless: Boolean,
    public val watchFaceInstanceId: StateFlow<String>
) {
    /** Whether the device is locked or not. */
    internal var isLocked: StateFlow<Boolean> = MutableStateFlow(false)

    internal constructor(
        interruptionFilter: StateFlow<Int?>,
        isAmbient: StateFlow<Boolean?>,
        isBatteryLowAndNotCharging: StateFlow<Boolean?>,
        isVisible: StateFlow<Boolean?>,
        hasLowBitAmbient: Boolean,
        hasBurnInProtection: Boolean,
        analogPreviewReferenceTimeMillis: Long,
        digitalPreviewReferenceTimeMillis: Long,
        @Px chinHeight: Int,
        isHeadless: Boolean,
        watchFaceInstanceId: StateFlow<String>,
        isLocked: StateFlow<Boolean>
    ) : this(
        interruptionFilter,
        isAmbient,
        isBatteryLowAndNotCharging,
        isVisible,
        hasLowBitAmbient,
        hasBurnInProtection,
        analogPreviewReferenceTimeMillis,
        digitalPreviewReferenceTimeMillis,
        chinHeight,
        isHeadless,
        watchFaceInstanceId
    ) {
        this.isLocked = isLocked
    }

    @Deprecated("WatchState constructors without watchFaceInstanceId are deprecated")
    constructor(
        interruptionFilter: StateFlow<Int?>,
        isAmbient: StateFlow<Boolean?>,
        isBatteryLowAndNotCharging: StateFlow<Boolean?>,
        isVisible: StateFlow<Boolean?>,
        hasLowBitAmbient: Boolean,
        hasBurnInProtection: Boolean,
        analogPreviewReferenceTimeMillis: Long,
        digitalPreviewReferenceTimeMillis: Long,
        chinHeight: Int,
        isHeadless: Boolean
    ) : this(
        interruptionFilter,
        isAmbient,
        isBatteryLowAndNotCharging,
        isVisible,
        hasLowBitAmbient,
        hasBurnInProtection,
        analogPreviewReferenceTimeMillis,
        digitalPreviewReferenceTimeMillis,
        chinHeight,
        isHeadless,
        watchFaceInstanceId = MutableStateFlow(DEFAULT_INSTANCE_ID),
        MutableStateFlow(false)
    )

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("WatchState:")
        writer.increaseIndent()
        writer.println("interruptionFilter=${interruptionFilter.value}")
        writer.println("isAmbient=${isAmbient.value}")
        writer.println("isBatteryLowAndNotCharging=${isBatteryLowAndNotCharging.value}")
        writer.println("isVisible=${isVisible.value}")
        writer.println("hasLowBitAmbient=$hasLowBitAmbient")
        writer.println("hasBurnInProtection=$hasBurnInProtection")
        writer.println("analogPreviewReferenceTimeMillis=$analogPreviewReferenceTimeMillis")
        writer.println("digitalPreviewReferenceTimeMillis=$digitalPreviewReferenceTimeMillis")
        writer.println("chinHeight=$chinHeight")
        writer.println("isHeadless=$isHeadless")
        writer.println("watchFaceInstanceId=${watchFaceInstanceId.value}")
        writer.decreaseIndent()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableWatchState() {
    public val interruptionFilter: MutableStateFlow<Int> =
        MutableStateFlow(NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
    public val isAmbient: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    public val isBatteryLowAndNotCharging: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    public val isVisible: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    public var hasLowBitAmbient: Boolean = false
    public var hasBurnInProtection: Boolean = false
    public var analogPreviewReferenceTimeMillis: Long = 0
    public var digitalPreviewReferenceTimeMillis: Long = 0
    public val watchFaceInstanceId: MutableStateFlow<String> = MutableStateFlow(DEFAULT_INSTANCE_ID)
    public val isLocked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Px
    public var chinHeight: Int = 0
        @Px get
        set(@Px value) {
            field = value
        }

    public var isHeadless: Boolean = false

    public fun asWatchState(): WatchState =
        WatchState(
            interruptionFilter = interruptionFilter,
            isAmbient = isAmbient,
            isBatteryLowAndNotCharging = isBatteryLowAndNotCharging,
            isVisible = isVisible,
            hasLowBitAmbient = hasLowBitAmbient,
            hasBurnInProtection = hasBurnInProtection,
            analogPreviewReferenceTimeMillis = analogPreviewReferenceTimeMillis,
            digitalPreviewReferenceTimeMillis = digitalPreviewReferenceTimeMillis,
            chinHeight = chinHeight,
            isHeadless = isHeadless,
            watchFaceInstanceId = watchFaceInstanceId,
            isLocked = isLocked
        )
}
