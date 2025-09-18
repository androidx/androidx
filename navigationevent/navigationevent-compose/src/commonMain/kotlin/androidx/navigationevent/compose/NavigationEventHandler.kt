/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

/**
 * A composable that handles navigation events using simple lambda handlers, providing contextual
 * information about both back and forward navigation destinations.
 *
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * *last* among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use [isBackEnabled] and
 * [isForwardEnabled] to control whether the handler is active. This is preferable to conditionally
 * calling [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change
 * the order of composition, leading to unpredictable behavior where different handlers are invoked
 * after recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back or forward gesture may be dispatched within a rendering
 * frame before the corresponding `enabled` flag is updated, which can cause unexpected behavior
 * (see b/375343407, b/384186542). For example, if [isBackEnabled] is set to `false`, a back gesture
 * initiated in the same frame may still trigger this handler because the system sees the stale
 * `true` value.
 *
 * @param T The type of the navigation information.
 * @param currentInfo An object containing information about the current destination.
 * @param backInfo A list of destinations the user may navigate back to. Can be empty if not
 *   available.
 * @param forwardInfo A list of destinations the user may navigate forward to. Can be empty if not
 *   available.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes and navigation
 *   occurs.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes and navigation occurs.
 */
@Composable
public fun <T : NavigationEventInfo> NavigationEventHandler(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
    // ---- Forward Events ----
    isForwardEnabled: Boolean = true,
    onForwardCancelled: () -> Unit = {},
    onForwardCompleted: () -> Unit = {},
    // ---- Back Events ----
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit = {},
) {
    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
            }
            .navigationEventDispatcher

    val handler = remember { ComposeNavigationEventHandler<T>() }

    SideEffect {
        handler.isForwardEnabled = isForwardEnabled
        handler.currentOnForwardCancelled = onForwardCancelled
        handler.currentOnForwardCompleted = onForwardCompleted

        handler.isBackEnabled = isBackEnabled
        handler.currentOnBackCancelled = onBackCancelled
        handler.currentOnBackCompleted = onBackCompleted

        handler.setInfo(currentInfo, backInfo, forwardInfo)
    }

    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}

/**
 * A composable that handles only back navigation gestures.
 *
 * This is a convenience wrapper around [NavigationEventHandler] for cases where forward navigation
 * is not relevant.
 *
 * @param T The type of the navigation information.
 * @param currentInfo Information about the current destination.
 * @param backInfo A list of destinations the user may navigate back to. Can be empty.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes and navigation occurs.
 */
@Composable
public fun <T : NavigationEventInfo> NavigationBackHandler(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit,
) {
    NavigationEventHandler(
        currentInfo = currentInfo,
        backInfo = backInfo,
        forwardInfo = emptyList(),
        onForwardCancelled = {},
        onForwardCompleted = {},
        isForwardEnabled = false, // disable forward
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
        isBackEnabled = isBackEnabled,
    )
}

/**
 * A composable that handles only forward navigation gestures.
 *
 * This is a convenience wrapper around [NavigationEventHandler] for cases where back navigation is
 * not relevant.
 *
 * @param T The type of the navigation information.
 * @param currentInfo Information about the current destination.
 * @param forwardInfo A list of destinations the user may navigate forward to. Can be empty.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes and navigation
 *   occurs.
 */
@Composable
public fun <T : NavigationEventInfo> NavigationForwardHandler(
    currentInfo: T,
    forwardInfo: List<T> = emptyList(),
    isForwardEnabled: Boolean = true,
    onForwardCancelled: () -> Unit = {},
    onForwardCompleted: () -> Unit,
) {
    NavigationEventHandler(
        currentInfo = currentInfo,
        backInfo = emptyList(),
        forwardInfo = forwardInfo,
        onForwardCancelled = onForwardCancelled,
        onForwardCompleted = onForwardCompleted,
        isForwardEnabled = isForwardEnabled,
        onBackCancelled = {},
        onBackCompleted = {},
        isBackEnabled = false, // disable back
    )
}

/** A simple [NavigationEventHandler] that delegates its methods to lambda functions. */
private class ComposeNavigationEventHandler<T : NavigationEventInfo> :
    NavigationEventHandler<T>(isBackEnabled = false, isForwardEnabled = false) {

    var currentOnForwardCancelled: () -> Unit = {}
    var currentOnForwardCompleted: () -> Unit = {}
    var currentOnBackCancelled: () -> Unit = {}
    var currentOnBackCompleted: () -> Unit = {}

    override fun onForwardCancelled() {
        currentOnForwardCancelled.invoke()
    }

    override fun onForwardCompleted() {
        currentOnForwardCompleted.invoke()
    }

    override fun onBackCancelled() {
        currentOnBackCancelled.invoke()
    }

    override fun onBackCompleted() {
        currentOnBackCompleted.invoke()
    }
}
