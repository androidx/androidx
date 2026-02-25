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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.Posture
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.collectLatest

@Stable
internal class UiMediaScopeImpl(
    context: Context,
    inputManager: InputManager,
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

/**
 * A composable function that creates and populates a [UiMediaScope] with information about the
 * current device and window environment.
 *
 * This function is the core implementation that backs the `mediaQuery` composable. It gathers
 * various pieces of context-dependent information and makes them available through the
 * [UiMediaScope] interface.
 */
@Composable
internal fun obtainUiMediaScope(
    context: Context,
    view: View,
    windowInfo: WindowInfo,
): UiMediaScope {
    val inputManager = remember { context.getSystemService(Context.INPUT_SERVICE) as InputManager }
    val initialImeVisibility = remember { ViewCompat.getRootWindowInsets(view).isImeVisible }
    val scope = remember {
        UiMediaScopeImpl(context, inputManager, windowInfo, initialImeVisibility)
    }
    scope._windowInfo = windowInfo

    // Window posture
    LaunchedEffect(context) {
        WindowInfoTracker.getOrCreate(context).windowLayoutInfo(context).collectLatest { layout ->
            scope._windowPosture = resolvePosture(layout)
        }
    }

    // Input Devices (Pointer & Physical Keyboard)
    DisposableEffect(context) {
        val listener =
            object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(id: Int) = update()

                override fun onInputDeviceRemoved(id: Int) = update()

                override fun onInputDeviceChanged(id: Int) = update()

                fun update() {
                    scope._anyPointer = resolvePointerPrecision(inputManager)
                    scope.hasPhysicalKeyboard = hasPhysicalKeyboard(inputManager)
                }
            }

        inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))

        listener.update()

        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    // IME listener (Virtual Keyboard)
    DisposableEffect(view) {
        val listener =
            ViewTreeObserver.OnGlobalLayoutListener {
                scope.isImeVisible = ViewCompat.getRootWindowInsets(view).isImeVisible
            }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }

    // Docked state receiver for reachability
    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_DOCK_EVENT)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    scope.isDocked = isDocked(intent)
                }
            }
        val stickyIntent =
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        scope.isDocked = isDocked(stickyIntent)

        onDispose { context.unregisterReceiver(receiver) }
    }

    return scope
}

/** Resolves the device [Posture] from the given [WindowLayoutInfo]. */
private fun resolvePosture(layoutInfo: WindowLayoutInfo): Posture {
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
private fun hasPhysicalKeyboard(inputManager: InputManager?): Boolean {
    if (inputManager == null) return false

    return inputManager.inputDeviceIds?.any { id ->
        val device = inputManager.getInputDevice(id) ?: return@any false
        device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC && !device.isVirtual
    } ?: false
}

/**
 * Resolves the primary pointer type available on the system based on all connected input devices.
 *
 * Priority: Fine > Coarse > Blunt > None.
 */
private fun resolvePointerPrecision(inputManager: InputManager?): PointerPrecision {
    if (inputManager == null) return PointerPrecision.None

    var pointerPrecision = PointerPrecision.None

    for (id in inputManager.inputDeviceIds) {
        val device = inputManager.getInputDevice(id) ?: continue
        val sources = device.sources

        // High Precision (Fine)
        if (
            sources.hasSource(InputDevice.SOURCE_MOUSE) ||
                sources.hasSource(InputDevice.SOURCE_STYLUS) ||
                sources.hasSource(InputDevice.SOURCE_TOUCHPAD)
        ) {
            return PointerPrecision.Fine
        }

        // Limited Precision (Coarse)
        if (sources.hasSource(InputDevice.SOURCE_TOUCHSCREEN)) {
            pointerPrecision = PointerPrecision.Coarse
            continue
        }

        // Low Precision (Blunt)
        if (
            pointerPrecision == PointerPrecision.None &&
                (sources.hasSource(InputDevice.SOURCE_JOYSTICK) ||
                    sources.hasSource(InputDevice.SOURCE_GAMEPAD))
        ) {
            pointerPrecision = PointerPrecision.Blunt
        }
    }

    return pointerPrecision
}

private val WindowInsetsCompat?.isImeVisible: Boolean
    get() = this?.isVisible(WindowInsetsCompat.Type.ime()) == true

private fun isDocked(intent: Intent?): Boolean {
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
