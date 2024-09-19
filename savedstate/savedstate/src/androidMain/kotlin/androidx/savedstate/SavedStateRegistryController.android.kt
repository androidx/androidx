/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.MainThread
import androidx.savedstate.internal.SavedStateRegistryImpl

public actual class SavedStateRegistryController
private actual constructor(
    private val impl: SavedStateRegistryImpl,
) {

    actual val savedStateRegistry: SavedStateRegistry = SavedStateRegistry(impl)

    @MainThread
    actual fun performAttach() {
        impl.performAttach()
    }

    @MainThread
    actual fun performRestore(savedState: SavedState?) {
        impl.performRestore(savedState)
    }

    @MainThread
    actual fun performSave(outBundle: SavedState) {
        impl.performSave(outBundle)
    }

    actual companion object {

        @JvmStatic
        actual fun create(owner: SavedStateRegistryOwner): SavedStateRegistryController {
            val impl =
                SavedStateRegistryImpl(
                    owner = owner,
                    onAttach = { owner.lifecycle.addObserver(Recreator(owner)) },
                )
            return SavedStateRegistryController(impl)
        }
    }
}
