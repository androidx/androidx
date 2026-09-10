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

package androidx.savedstate.internal

import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistry.SavedStateRestorer
import androidx.savedstate.SavedStateValue

internal class ProviderSavedStateValue(
    override val value: SavedStateProvider,
    var savedState: SavedState? = null,
) : SavedStateValue<SavedStateProvider> {

    var isRestored: Boolean = false
        private set

    override fun saveState(): SavedState {
        return value.saveState()
    }

    override fun restoreState(savedState: SavedState?) {
        isRestored = true
        if (value is SavedStateRestorer) {
            value.restoreState(savedState)
        } else {
            this.savedState = savedState
        }
    }
}
