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

package androidx.activity.compose.internal

import androidx.activity.OnBackPressedDispatcher
import androidx.navigationevent.NavigationEventDispatcher

/**
 * A compatibility layer that abstracts over the [NavigationEventDispatcher] and the legacy
 * [OnBackPressedDispatcher].
 *
 * This allows code to register a back handler without needing to know which dispatcher is currently
 * active, prioritizing the [NavigationEventDispatcher] if it is available.
 *
 * @see [androidx.activity.compose.BackHandler]
 * @see [androidx.activity.compose.PredictiveBackHandler]
 */
internal class BackHandlerDispatcherCompat(
    private val navigationEventDispatcher: NavigationEventDispatcher?,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {

    init {
        requireNotNull(navigationEventDispatcher ?: onBackPressedDispatcher) {
            "At least one dispatcher (NavigationEventDispatcher or OnBackPressedDispatcher) " +
                "must be non-null."
        }
    }

    /**
     * Adds the handler to the appropriate dispatcher.
     *
     * It will prioritize the [NavigationEventDispatcher] if it exists. Otherwise, it falls back to
     * [OnBackPressedDispatcher].
     */
    fun addHandler(handler: BackHandlerCompat) {
        when {
            navigationEventDispatcher != null ->
                navigationEventDispatcher.addHandler(handler.navigationEventHandler)
            onBackPressedDispatcher != null ->
                onBackPressedDispatcher.addCallback(handler.onBackPressedCallback)
            else -> error("Unreachable")
        }
    }

    /**
     * Removes the handler from the appropriate dispatcher.
     *
     * It will prioritize the [NavigationEventDispatcher] if it exists. Otherwise, it falls back to
     * [OnBackPressedDispatcher].
     */
    fun removeHandler(handler: BackHandlerCompat) {
        when {
            navigationEventDispatcher != null -> handler.navigationEventHandler.remove()
            onBackPressedDispatcher != null -> handler.onBackPressedCallback.remove()
            else -> error("Unreachable")
        }
    }
}
