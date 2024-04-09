/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.internal

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * It depends on the state of accessibility services to determine the current state of touch
 * exploration services.
 */
@Composable
internal actual fun touchExplorationState(): State<Boolean> {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    val listener = remember { Listener() }

    LocalLifecycleOwner.current.lifecycle.ObserveState(
        handleEvent = { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listener.register(accessibilityManager)
            }
        },
        onDispose = {
            listener.unregister(accessibilityManager)
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
        val observer = LifecycleEventObserver { _, event ->
            handleEvent(event)
        }
        this@ObserveState.addObserver(observer)
        onDispose {
            onDispose()
            this@ObserveState.removeObserver(observer)
        }
    }
}

private class Listener : AccessibilityStateChangeListener, TouchExplorationStateChangeListener,
    State<Boolean> {
    private var accessibilityEnabled by mutableStateOf(false)
    private var touchExplorationEnabled by mutableStateOf(false)

    override val value: Boolean
        get() = accessibilityEnabled && touchExplorationEnabled

    override fun onAccessibilityStateChanged(it: Boolean) {
        accessibilityEnabled = it
    }

    override fun onTouchExplorationStateChanged(it: Boolean) {
        touchExplorationEnabled = it
    }

    fun register(am: AccessibilityManager) {
        accessibilityEnabled = am.isEnabled
        touchExplorationEnabled = am.isTouchExplorationEnabled

        am.addTouchExplorationStateChangeListener(this)
        am.addAccessibilityStateChangeListener(this)
    }

    fun unregister(am: AccessibilityManager) {
        am.removeTouchExplorationStateChangeListener(this)
        am.removeAccessibilityStateChangeListener(this)
    }
}
