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

package androidx.savedstate

/**
 * Contributes to the saved state.
 *
 * Implementations can optionally implement [SavedStateRestorer] to receive and restore state during
 * the state restoration phase.
 */
public fun interface SavedStateProvider {
    /**
     * Called to retrieve the state from a component before it is killed so the state can be
     * retrieved later from [SavedStateRegistry.consumeRestoredStateForKey].
     *
     * @return The [SavedState] containing the saved state.
     */
    public fun saveState(): SavedState
}
