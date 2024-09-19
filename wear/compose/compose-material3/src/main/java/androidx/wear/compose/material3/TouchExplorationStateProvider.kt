/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A functional interface for providing the state of touch exploration services. It is strongly
 * discouraged to make logic conditional based on state of accessibility services. Please consult
 * with accessibility experts before making such change.
 */
internal fun interface TouchExplorationStateProvider {

    /**
     * Returns the touch exploration service state wrapped in a [State] to allow composables to
     * attach the state to itself. This will allow composables to react to change in service state,
     * if required.
     */
    @Composable fun touchExplorationState(): State<Boolean>
}

/**
 * The default implementation of [TouchExplorationStateProvider]. It depends on the state of
 * accessibility services to determine the current state of touch exploration services.
 */
internal class DefaultTouchExplorationStateProvider : TouchExplorationStateProvider {

    @Composable
    override fun touchExplorationState(): State<Boolean> {
        val context = LocalContext.current
        val accessibilityManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }

        val listener = remember { Listener(accessibilityManager) }

        LocalLifecycleOwner.current.lifecycle.ObserveState(
            handleEvent = { event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    listener.register()
                } else if (event == Lifecycle.Event.ON_PAUSE) {
                    listener.unregister()
                }
            },
            onDispose = {
                // Unregister the listener in case the PAUSE lifecycle event never came through
                // Unregistering multiple times is safe
                listener.unregister()
            }
        )

        return listener
    }

    @Composable
    private fun Lifecycle.ObserveState(
        handleEvent: (Lifecycle.Event) -> Unit = {},
        onDispose: () -> Unit = {}
    ) {
        DisposableEffect(this) {
            val observer = LifecycleEventObserver { _, event -> handleEvent(event) }
            this@ObserveState.addObserver(observer)
            onDispose {
                onDispose()
                this@ObserveState.removeObserver(observer)
            }
        }
    }

    private class Listener(
        private val accessibilityManager: AccessibilityManager,
    ) : AccessibilityStateChangeListener, TouchExplorationStateChangeListener, State<Boolean> {

        private var accessibilityEnabled by mutableStateOf(accessibilityManager.isEnabled)
        private var touchExplorationEnabled by
            mutableStateOf(accessibilityManager.isTouchExplorationEnabled)

        override val value: Boolean
            get() = accessibilityEnabled && touchExplorationEnabled

        override fun onAccessibilityStateChanged(it: Boolean) {
            accessibilityEnabled = it
        }

        override fun onTouchExplorationStateChanged(it: Boolean) {
            touchExplorationEnabled = it
        }

        fun register() {
            accessibilityEnabled = accessibilityManager.isEnabled
            touchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled

            accessibilityManager.addTouchExplorationStateChangeListener(this)
            accessibilityManager.addAccessibilityStateChangeListener(this)
        }

        fun unregister() {
            accessibilityManager.removeTouchExplorationStateChangeListener(this)
            accessibilityManager.removeAccessibilityStateChangeListener(this)
        }
    }
}

/** CompositionLocal to provide a means to override TouchExplorationStateProvider during testing */
internal val LocalTouchExplorationStateProvider:
    ProvidableCompositionLocal<TouchExplorationStateProvider> =
    staticCompositionLocalOf {
        DefaultTouchExplorationStateProvider()
    }
