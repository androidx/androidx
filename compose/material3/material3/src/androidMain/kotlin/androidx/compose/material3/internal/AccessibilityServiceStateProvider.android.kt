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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityServicesStateChangeListener
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal actual fun rememberAccessibilityServiceState(
    listenToTouchExplorationState: Boolean,
    listenToSwitchAccessState: Boolean,
): State<Boolean> {
    val context = LocalContext.current
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    val listener =
        remember(listenToTouchExplorationState, listenToSwitchAccessState) {
            Listener(listenToTouchExplorationState, listenToSwitchAccessState)
        }

    ObserveState(
        lifecycleOwner = LocalLifecycleOwner.current,
        handleEvent = { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listener.register(accessibilityManager)
            }
        },
        onDispose = { listener.unregister(accessibilityManager) }
    )

    return listener
}

@Composable
private fun ObserveState(
    lifecycleOwner: LifecycleOwner,
    handleEvent: (Lifecycle.Event) -> Unit = {},
    onDispose: () -> Unit = {}
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> handleEvent(event) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            onDispose()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Stable
private class Listener(
    listenToTouchExplorationState: Boolean,
    listenToSwitchAccessState: Boolean,
) : AccessibilityStateChangeListener, State<Boolean> {
    private var accessibilityEnabled by mutableStateOf(false)

    private val touchExplorationListener =
        if (listenToTouchExplorationState) {
            object : TouchExplorationStateChangeListener {
                var enabled by mutableStateOf(false)

                override fun onTouchExplorationStateChanged(enabled: Boolean) {
                    this.enabled = enabled
                }
            }
        } else {
            null
        }

    private val switchAccessListener =
        if (listenToSwitchAccessState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            object : AccessibilityServicesStateChangeListener {
                var enabled by mutableStateOf(false)

                override fun onAccessibilityServicesStateChanged(am: AccessibilityManager) {
                    enabled = am.switchAccessEnabled
                }
            }
        } else {
            null
        }

    private val AccessibilityManager.switchAccessEnabled
        get() =
            getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC).fastAny {
                it.settingsActivityName?.contains(SwitchAccessActivityName) == true
            }

    override val value: Boolean
        get() =
            accessibilityEnabled &&
                ((touchExplorationListener?.enabled ?: false) ||
                    (switchAccessListener?.enabled ?: false))

    override fun onAccessibilityStateChanged(enabled: Boolean) {
        accessibilityEnabled = enabled
    }

    fun register(am: AccessibilityManager) {
        accessibilityEnabled = am.isEnabled
        am.addAccessibilityStateChangeListener(this)
        touchExplorationListener?.let {
            it.enabled = am.isTouchExplorationEnabled
            am.addTouchExplorationStateChangeListener(it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switchAccessListener?.let {
                it.enabled = am.switchAccessEnabled
                Api33Impl.addAccessibilityServicesStateChangeListener(am, it)
            }
        }
    }

    fun unregister(am: AccessibilityManager) {
        am.removeAccessibilityStateChangeListener(this)
        touchExplorationListener?.let { am.removeTouchExplorationStateChangeListener(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switchAccessListener?.let {
                Api33Impl.removeAccessibilityServicesStateChangeListener(am, it)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33Impl {
        @JvmStatic
        @DoNotInline
        fun addAccessibilityServicesStateChangeListener(
            am: AccessibilityManager,
            listener: AccessibilityServicesStateChangeListener
        ) {
            am.addAccessibilityServicesStateChangeListener(listener)
        }

        @JvmStatic
        @DoNotInline
        fun removeAccessibilityServicesStateChangeListener(
            am: AccessibilityManager,
            listener: AccessibilityServicesStateChangeListener
        ) {
            am.removeAccessibilityServicesStateChangeListener(listener)
        }
    }
}

private const val SwitchAccessActivityName = "SwitchAccess"
