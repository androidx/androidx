/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.activity.compose

import androidx.activity.ActivityFlags
import androidx.activity.ExperimentalActivityApi
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.internal.BackHandlerCompat
import androidx.activity.compose.internal.BackHandlerDispatcherCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Provides a [OnBackPressedDispatcher] that can be used by Composables hosted in a
 * [androidx.activity.ComponentActivity].
 */
public object LocalOnBackPressedDispatcherOwner {
    private val LocalOnBackPressedDispatcherOwner =
        compositionLocalOf<OnBackPressedDispatcherOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided,
     * one has not been set via [androidx.activity.setViewTreeOnBackPressedDispatcherOwner], nor is
     * one available by looking at the [LocalContext].
     */
    public val current: OnBackPressedDispatcherOwner?
        @Composable
        get() =
            LocalOnBackPressedDispatcherOwner.current
                ?: LocalView.current.findViewTreeOnBackPressedDispatcherOwner()
                ?: findOwner<OnBackPressedDispatcherOwner>(LocalContext.current)

    /**
     * Associates a [LocalOnBackPressedDispatcherOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(
        dispatcherOwner: OnBackPressedDispatcherOwner
    ): ProvidedValue<OnBackPressedDispatcherOwner?> {
        return LocalOnBackPressedDispatcherOwner.provides(dispatcherOwner)
    }
}

/**
 * An effect for handling presses of the system back button.
 *
 * This effect registers a callback to be invoked when the system back button is pressed. The
 * [onBack] will be invoked when the system back button is pressed (that is, `onCompleted`).
 *
 * The handler is registered once and stays attached for the lifetime of the [LifecycleOwner]. Its
 * [OnBackPressedCallback.isEnabled] state automatically follows the lifecycle: it becomes enabled
 * when the lifecycle is at least [Lifecycle.State.STARTED] and disabled otherwise.
 *
 * ## Precedence
 * If multiple [BackHandler] are present in the composition, the one that is composed **last** among
 * all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use the `enabled` parameter to
 * control whether the handler is active. This is preferable to conditionally calling [BackHandler]
 * (e.g., inside an `if` block), as conditional calls can change the order of composition, leading
 * to unpredictable behavior where different handlers are invoked after recomposition.
 *
 * ## Legacy Behavior
 * To restore the legacy add/remove behavior, set
 * [ActivityFlags.isOnBackPressedLifecycleOrderMaintained] to `false`. In legacy mode, the handler
 * is added on [Lifecycle.Event.ON_START] and removed on [Lifecycle.Event.ON_STOP], which may change
 * dispatch ordering across lifecycle transitions.
 *
 * @sample androidx.activity.compose.samples.BackHandler
 * @param enabled If `true`, this handler will be enabled and eligible to handle the back press.
 * @param onBack The action to be invoked when the system back button is pressed.
 */
@SuppressWarnings("MissingJvmstatic")
@OptIn(ExperimentalActivityApi::class)
@Composable
public fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val navigationEventDispatcherOwner = LocalNavigationEventDispatcherOwner.current
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val owner =
        requireNotNull(navigationEventDispatcherOwner ?: onBackPressedDispatcherOwner) {
            "No NavigationEventDispatcherOwner was provided via " +
                "LocalNavigationEventDispatcherOwner and no OnBackPressedDispatcherOwner was " +
                "provided via LocalOnBackPressedDispatcherOwner. Please provide one of the two."
        }

    val dispatcher = remember {
        // Create a dispatcher compatibility layer that decides whether to use the new
        // 'NavigationEventDispatcher' or the legacy 'OnBackPressedDispatcher'.
        BackHandlerDispatcherCompat(
            navigationEventDispatcher = navigationEventDispatcherOwner?.navigationEventDispatcher,
            onBackPressedDispatcher = onBackPressedDispatcherOwner?.onBackPressedDispatcher,
        )
    }

    val compositeKey = currentCompositeKeyHashCode
    val handler =
        remember(dispatcher, compositeKey) {
            ComposeBackHandler(BackHandlerInfo(owner, compositeKey))
        }

    if (ActivityFlags.isOnBackPressedLifecycleOrderMaintained) {
        // Keep the handler instance stable across recompositions, but update the active parameters.
        SideEffect { handler.currentOnBackCompleted = onBack }

        // Use LifecycleStartEffect to add the handler in sync with the lifecycle,
        // avoiding the frame delay that happens with state-based APIs like collectAsState().
        LifecycleStartEffect(enabled, handler) {
            handler.isBackEnabled = enabled
            onStopOrDispose { handler.isBackEnabled = false }
        }

        DisposableEffect(dispatcher, handler) {
            dispatcher.addHandler(handler)
            onDispose { dispatcher.removeHandler(handler) }
        }
    } else {
        // Keep the handler instance stable across recompositions, but update the active parameters.
        SideEffect {
            handler.isBackEnabled = enabled
            handler.currentOnBackCompleted = onBack
        }

        // Use LifecycleStartEffect to add the handler in sync with the lifecycle,
        // avoiding the frame delay that happens with state-based APIs like collectAsState().
        LifecycleStartEffect(dispatcher, handler) {
            dispatcher.addHandler(handler)
            onStopOrDispose { dispatcher.removeHandler(handler) }
        }
    }
}

private class ComposeBackHandler(info: BackHandlerInfo) : BackHandlerCompat(info) {

    var currentOnBackCompleted: () -> Unit = {}

    override fun onBackCompleted() {
        currentOnBackCompleted()
    }
}

private data class BackHandlerInfo(val owner: Any, val compositeKey: Long) : NavigationEventInfo()
