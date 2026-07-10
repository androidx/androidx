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

package androidx.navigationevent

/**
 * A listener that provides a final fallback action for unhandled `backCompleted` callbacks.
 *
 * This is typically used to provide a default system behavior, like finishing an `Activity`, when
 * no other part of the application consumes the back navigation event.
 */
public fun interface OnBackCompletedFallback {

    /**
     * Called when a `backCompleted` callback is dispatched but not handled by any
     * [NavigationEventHandler].
     */
    public fun onBackCompletedFallback()
}
