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

package androidx.camera.compose

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface
import android.view.View
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView

/**
 * Returns the display rotation used by [CameraXViewfinder] to orient the camera preview upright, as
 * one of [Surface.ROTATION_0], [Surface.ROTATION_90], [Surface.ROTATION_180], or
 * [Surface.ROTATION_270].
 *
 * This follows the same rules as the display rotation used by
 * [androidx.camera.viewfinder.compose.Viewfinder] to correct its content, so that both stay
 * consistent with the window layout of the same frame:
 * - Any rotation is accepted with a configuration change, a change of the window size, or when the
 *   view attaches, since a quarter turn relayouts the window.
 * - Otherwise, only a half turn is accepted, such as from [DisplayManager.DisplayListener] or a
 *   same-size relayout, since a half turn does not change the configuration or window size.
 *
 * When the view is on an external display, [ImplementationMode.EXTERNAL] and
 * [TransformationMode.PRE_APPLIED] streams follow [Display.DEFAULT_DISPLAY] instead, matching
 * `PreviewView`, since the camera rotates with the device and `SurfaceView` buffer transforms are
 * relative to the default display. Its rotation does not relayout the window on the external
 * display, so any change is accepted.
 *
 * @param implementationMode The resolved [ImplementationMode] of the viewfinder.
 * @param transformationMode The [TransformationMode] of the stream.
 * @return The display rotation as a [Surface] rotation constant.
 */
@Composable
internal fun rememberCameraDisplayRotation(
    implementationMode: ImplementationMode,
    transformationMode: Int,
): Int {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isInInspectionMode = LocalInspectionMode.current
    val useDefaultDisplay =
        implementationMode == ImplementationMode.EXTERNAL ||
            transformationMode == TransformationMode.PRE_APPLIED
    // Not keyed by useDefaultDisplay: a new state would accept the current display rotation at
    // once,
    // ahead of the configuration update that the viewfinder waits for after a quarter turn.
    val state = remember(view) { CameraDisplayRotationState(view, useDefaultDisplay) }
    SideEffect { state.useDefaultDisplay = useDefaultDisplay }
    if (!isInInspectionMode) {
        DisposableEffect(state) {
            state.startListening()
            onDispose { state.stopListening() }
        }
    }
    return state.resolve(configuration, useDefaultDisplay)
}

private class CameraDisplayRotationState(
    private val view: View,
    /** Whether to follow [Display.DEFAULT_DISPLAY] when the view is on an external display. */
    var useDefaultDisplay: Boolean,
) {
    /** Incremented to re-run [resolve] when a new rotation should be accepted. */
    private var invalidations by mutableIntStateOf(0)
    private var acceptedRotation = Surface.ROTATION_0
    private var acceptedConfiguration: Configuration? = null
    private var acceptedDisplayId: Int? = null
    private var isUpdatePending = false

    // Use the view's context rather than the application context, which may omit the default
    // display when an external display is connected.
    private val displayManager =
        view.context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != targetDisplay()?.displayId) return
                requestUpdate(acceptAnyRotation = false)
            }
        }

    private val attachListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                requestUpdate(acceptAnyRotation = true)
            }

            override fun onViewDetachedFromWindow(v: View) {}
        }

    private val layoutListener =
        View.OnLayoutChangeListener {
            _,
            left,
            top,
            right,
            bottom,
            oldLeft,
            oldTop,
            oldRight,
            oldBottom ->
            val isSizeChanged =
                right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop
            requestUpdate(acceptAnyRotation = isSizeChanged)
        }

    /**
     * Returns the rotation to use for the given [configuration] in the current composition.
     *
     * A change of the followed display, such as between an external display and
     * [Display.DEFAULT_DISPLAY], is accepted immediately, while a change of [useDefaultDisplay]
     * that keeps following the same display preserves the accepted rotation.
     */
    fun resolve(configuration: Configuration, useDefaultDisplay: Boolean): Int {
        observeInvalidations()
        val display = targetDisplay(useDefaultDisplay) ?: return acceptedRotation
        if (
            isUpdatePending ||
                configuration !== acceptedConfiguration ||
                display.displayId != acceptedDisplayId
        ) {
            isUpdatePending = false
            acceptedConfiguration = configuration
            acceptedDisplayId = display.displayId
            acceptedRotation = display.rotation
        }
        return acceptedRotation
    }

    fun startListening() {
        view.addOnAttachStateChangeListener(attachListener)
        view.addOnLayoutChangeListener(layoutListener)
        displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        // The view may have attached or rotated between composition and registration.
        requestUpdate(acceptAnyRotation = true)
    }

    fun stopListening() {
        view.removeOnAttachStateChangeListener(attachListener)
        view.removeOnLayoutChangeListener(layoutListener)
        displayManager?.unregisterDisplayListener(displayListener)
    }

    /**
     * Requests [resolve] to accept the current rotation if it differs from the accepted one and,
     * unless [acceptAnyRotation] is set or another display is followed, it is a half turn away.
     */
    private fun requestUpdate(acceptAnyRotation: Boolean) {
        val display = targetDisplay() ?: return
        val delta = (display.rotation - acceptedRotation).mod(4)
        val isOtherDisplay = display.displayId != view.display?.displayId
        val shouldUpdate = if (acceptAnyRotation || isOtherDisplay) delta != 0 else delta == 2
        if (shouldUpdate) {
            isUpdatePending = true
            invalidations++
        }
    }

    /** Returns the display to follow, or `null` if the view is not attached to a display. */
    private fun targetDisplay(useDefaultDisplay: Boolean = this.useDefaultDisplay): Display? {
        val viewDisplay = view.display ?: return null
        if (!useDefaultDisplay || viewDisplay.displayId == Display.DEFAULT_DISPLAY) {
            return viewDisplay
        }
        return displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: viewDisplay
    }

    /** Reads [invalidations] so that the caller re-runs when it changes. */
    private fun observeInvalidations(): Int = invalidations
}
