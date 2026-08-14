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
 * Restores state for a component.
 *
 * Implement this interface on a [SavedStateRegistry.SavedStateProvider] registered via
 * [SavedStateRegistry.registerSavedStateProvider] to receive restored state automatically.
 *
 * The registry will invoke [restoreState] during the restoration phase or immediately upon
 * registration if the state is already restored.
 */
public fun interface SavedStateRestorer {
    /**
     * Called to restore the state of a component.
     *
     * @param savedState The [SavedState] containing the previously saved state, or `null` if no
     *   state was previously saved for this component.
     */
    public fun restoreState(savedState: SavedState?)
}
