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

import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistry.SavedStateRestorer

/**
 * Interface representing a saved state component holding a [value].
 *
 * A [SavedStateValue] combines state saving ([SavedStateProvider]) and restoration
 * ([SavedStateRestorer]) with direct access to the underlying [value].
 *
 * @param T type of the underlying value
 */
public interface SavedStateValue<T> : SavedStateProvider, SavedStateRestorer {
    /** The current value held by this saved state component. */
    public val value: T
}
