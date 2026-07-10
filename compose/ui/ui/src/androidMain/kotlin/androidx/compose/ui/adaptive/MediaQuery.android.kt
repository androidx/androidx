/*
 * Copyright 2026 The Android Open Source Project
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
@file:OptIn(ExperimentalMediaQueryApi::class)

package androidx.compose.ui.adaptive

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.Posture
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

@Stable
internal class UiMediaScopeImpl(
    context: Context,
    internal val inputManager: InputManager,
    windowInfo: WindowInfo,
    imeVisibility: Boolean,
) : UiMediaScope {
    private val packageManager = context.packageManager
    var _windowInfo by mutableStateOf(windowInfo)
    var _windowPosture by mutableStateOf(Posture.Flat)
    var _anyPointer by mutableStateOf(resolvePointerPrecision(inputManager))
    var isDocked by mutableStateOf(false)
    var isImeVisible by mutableStateOf(imeVisibility)
    var hasPhysicalKeyboard by mutableStateOf(hasPhysicalKeyboard(inputManager))

    override val hasMicrophone: Boolean
        get() = packageManager.isMicAvailable()

    override val hasCamera: Boolean
        get() = packageManager.isCameraAvailable()

    @get:FrequentlyChangingValue
    override val windowWidth: Dp
        get() = _windowInfo.containerDpSize.width

    @get:FrequentlyChangingValue
    override val windowHeight: Dp
        get() = _windowInfo.containerDpSize.height

    override val windowPosture: Posture
        get() = _windowPosture

    override val pointerPrecision: PointerPrecision
        get() = _anyPointer

    override val keyboardKind: KeyboardKind
        get() =
            when {
                hasPhysicalKeyboard -> KeyboardKind.Physical
                isImeVisible -> KeyboardKind.Virtual
                else -> KeyboardKind.None
            }

    override val viewingDistance: ViewingDistance
        get() =
            when {
                packageManager.isTvDevice() -> ViewingDistance.Far
                packageManager.isAutomotiveDevice() || isDocked -> ViewingDistance.Medium
                else -> ViewingDistance.Near
            }
}

/** Resolves the device [Posture] from the given [WindowLayoutInfo]. */
internal fun resolvePosture(layoutInfo: WindowLayoutInfo): Posture {
    @Suppress("ListIterator")
    val fold =
        layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull {
            it.state == FoldingFeature.State.HALF_OPENED
        } ?: return Posture.Flat

    return if (fold.orientation == FoldingFeature.Orientation.HORIZONTAL) {
        Posture.Tabletop
    } else {
        Posture.Book
    }
}

/** Checks if a physical, alphabetic keyboard is currently connected to the device. */
internal fun hasPhysicalKeyboard(inputManager: InputManager?): Boolean {
    if (inputManager == null) return false

    return inputManager.inputDeviceIds?.any { id ->
        val device = inputManager.getInputDevice(id) ?: return@any false
        device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC && !device.isVirtual
    } ?: false
}

/**
 * Resolves the highest precision pointer type available based on all connected input devices.
 *
 * Priority: Fine > Coarse > Blunt > None.
 *
 * The resolution is evaluated using a two-tiered approach:
 * 1. Valid Hardware Sources: Pointer input devices reporting valid X/Y axes motion ranges.
 * 2. Emulator fallback heuristics: Emulated input devices reporting composite sources with
 *    different precision classes are further evaluated on secondary axes (touch, scroll).
 *
 * A valid hardware source is always preferred over the fallback heuristic to avoid false positives.
 */
internal fun resolvePointerPrecision(inputManager: InputManager?): PointerPrecision {
    if (inputManager == null) return PointerPrecision.None

    var pointerPrecision = PointerPrecision.None
    var fallbackPrecision = PointerPrecision.None

    for (id in inputManager.inputDeviceIds) {
        val device = inputManager.getInputDevice(id) ?: continue

        // Relies on input devices reporting primary X/Y motion ranges.
        if (
            device.hasValidPointerSource(InputDevice.SOURCE_MOUSE) ||
                device.hasValidPointerSource(InputDevice.SOURCE_STYLUS) ||
                device.hasValidPointerSource(InputDevice.SOURCE_TOUCHPAD)
        ) {
            return PointerPrecision.Fine
        }

        if (device.hasValidPointerSource(InputDevice.SOURCE_TOUCHSCREEN)) {
            pointerPrecision = PointerPrecision.Coarse
        } else if (
            pointerPrecision == PointerPrecision.None &&
                (device.hasValidPointerSource(InputDevice.SOURCE_JOYSTICK) ||
                    device.hasValidPointerSource(InputDevice.SOURCE_GAMEPAD))
        ) {
            pointerPrecision = PointerPrecision.Blunt
        }

        // Fallback heuristic for emulators: Tracked separately to ensure precision evaluated from
        // a valid hardware source is not overridden by this fallback heuristic.
        if (device.hasFallbackFinePointer()) {
            fallbackPrecision = PointerPrecision.Fine
        } else if (
            fallbackPrecision != PointerPrecision.Fine && device.hasFallbackCoarsePointer()
        ) {
            fallbackPrecision = PointerPrecision.Coarse
        }
    }

    if (pointerPrecision != PointerPrecision.None) {
        return pointerPrecision
    }

    return fallbackPrecision
}

/**
 * Verify if a pointer input source is actually functional by checking both the bitmask and the
 * existence of a valid Motion Range.
 */
private fun InputDevice.hasValidPointerSource(
    source: Int,
    axis: Int = MotionEvent.AXIS_X,
): Boolean {
    return (sources and source == source) && getMotionRange(axis, source) != null
}

/**
 * Checks for a fine pointer precision using a fallback mechanism for emulated devices.
 *
 * Some environments, such as Desktop or XR emulators, may not correctly report standard primary
 * motion ranges for X/Y axes but do expose vertical scroll axes with composite sources.
 */
private fun InputDevice.hasFallbackFinePointer(): Boolean {
    return getMotionRange(MotionEvent.AXIS_X)?.isFromSource(InputDevice.SOURCE_MOUSE) == true &&
        getMotionRange(MotionEvent.AXIS_VSCROLL) != null
}

/**
 * Checks for a coarse pointer precision using a fallback mechanism for emulated devices.
 *
 * Some environments, such as Phone & Tablet emulators, may not correctly report standard primary
 * motion ranges for X/Y axes but do expose touch axes with composite sources.
 */
private fun InputDevice.hasFallbackCoarsePointer(): Boolean {
    return getMotionRange(MotionEvent.AXIS_X)?.isFromSource(InputDevice.SOURCE_TOUCHSCREEN) ==
        true &&
        (getMotionRange(MotionEvent.AXIS_TOUCH_MAJOR) != null ||
            getMotionRange(MotionEvent.AXIS_TOUCH_MINOR) != null)
}

internal val WindowInsetsCompat?.isImeVisible: Boolean
    get() = this?.isVisible(WindowInsetsCompat.Type.ime()) == true

internal fun isDocked(intent: Intent?): Boolean {
    if (intent == null) return false
    val dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED)
    return dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED
}

private fun PackageManager.isCameraAvailable(): Boolean =
    hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

private fun PackageManager.isMicAvailable(): Boolean =
    hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

private fun PackageManager.isAutomotiveDevice(): Boolean =
    hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)

private fun PackageManager.isTvDevice(): Boolean = hasSystemFeature(PackageManager.FEATURE_LEANBACK)

/** Checks if the source bitmask contains the specific target source flag. */
private fun Int.hasSource(source: Int): Boolean {
    return (this and source) == source
}
