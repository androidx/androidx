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

package androidx.camera.viewfinder.compose

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView

/**
 * Returns the rotation of the window the current view is attached to, as one of
 * [Surface.ROTATION_0], [Surface.ROTATION_90], [Surface.ROTATION_180] or [Surface.ROTATION_270],
 * consistent with the window layout of the same frame.
 *
 * The rotation is read from [View.getDisplay] whenever it may have changed, and a new value is
 * accepted only when it cannot run ahead of the window layout:
 * - Any rotation is accepted with a configuration change, a change of the window size, or when the
 *   view attaches, since a quarter turn relayouts the window.
 * - Otherwise, only a half turn is accepted, such as from [DisplayManager.DisplayListener] or a
 *   same-size relayout, since a half turn does not change the configuration or window size.
 *
 * @return The current rotation as a [Surface] rotation constant.
 */
@Composable
internal fun rememberDisplayRotation(): Int {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isInInspectionMode = LocalInspectionMode.current
    val state = remember(view) { DisplayRotationState(view) }
    if (!isInInspectionMode) {
        DisposableEffect(state) {
            state.startListening()
            onDispose { state.stopListening() }
        }
    }
    return state.resolve(configuration)
}

private class DisplayRotationState(private val view: View) {
    /** Incremented to re-run [resolve] when a new rotation should be accepted. */
    private var invalidations by mutableIntStateOf(0)
    private var acceptedRotation = Surface.ROTATION_0
    private var acceptedConfiguration: Configuration? = null
    private var isUpdatePending = false

    private val displayManager =
        view.context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != view.display?.displayId) return
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

    /** Returns the rotation to use for the given [configuration] in the current composition. */
    fun resolve(configuration: Configuration): Int {
        observeInvalidations()
        val rotation = view.display?.rotation ?: return acceptedRotation
        if (isUpdatePending || configuration !== acceptedConfiguration) {
            isUpdatePending = false
            acceptedConfiguration = configuration
            acceptedRotation = rotation
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
     * unless [acceptAnyRotation] is set, it is a half turn away.
     */
    private fun requestUpdate(acceptAnyRotation: Boolean) {
        val rotation = view.display?.rotation ?: return
        val delta = (rotation - acceptedRotation).mod(4)
        val shouldUpdate = if (acceptAnyRotation) delta != 0 else delta == 2
        if (shouldUpdate) {
            isUpdatePending = true
            invalidations++
        }
    }

    /** Reads [invalidations] so that the caller re-runs when it changes. */
    private fun observeInvalidations(): Int = invalidations
}
