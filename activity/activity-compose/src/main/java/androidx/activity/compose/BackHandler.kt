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

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * Provides a [OnBackPressedDispatcher] that can be used by Composables hosted in a
 * [androidx.activity.ComponentActivity].
 */
public object LocalOnBackPressedDispatcherOwner {
    private val LocalOnBackPressedDispatcherOwner =
        compositionLocalOf<OnBackPressedDispatcherOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not
     * been provided nor is one available by looking at the [LocalContext].
     */
    public val current: OnBackPressedDispatcherOwner?
        @Composable
        get() = LocalOnBackPressedDispatcherOwner.current
            ?: findOwner<OnBackPressedDispatcherOwner>(LocalContext.current)

    /**
     * Associates a [LocalOnBackPressedDispatcherOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(dispatcherOwner: OnBackPressedDispatcherOwner):
        ProvidedValue<OnBackPressedDispatcherOwner?> {
            return LocalOnBackPressedDispatcherOwner.provides(dispatcherOwner)
        }
}

/**
 * An effect for handling presses of the system back button.
 *
 * Calling this in your composable adds the given lambda to the [OnBackPressedDispatcher] of the
 * [LocalOnBackPressedDispatcherOwner].
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume
 * the call to system back and invoke its lambda. The call will continue to propagate up until it
 * finds an enabled BackHandler.
 *
 * @sample androidx.activity.compose.samples.BackHandler
 *
 * @param enabled if this BackHandler should be enabled
 * @param onBack the action invoked by pressing the system back
 */
@SuppressWarnings("MissingJvmstatic")
@Composable
public fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)
    // Remember in Composition a back callback that calls the `onBack` lambda
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    // On every successful composition, update the callback with the `enabled` value
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        // Add callback to the backDispatcher
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            backCallback.remove()
        }
    }
}
