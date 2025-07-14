/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState

/**
 * Creates [DisposableSaveableStateRegistry] with the restored values using [SavedStateRegistry] and
 * saves the values when [SavedStateRegistry] performs save.
 */
internal fun DisposableSaveableStateRegistry(
    id: String,
    savedStateRegistryOwner: SavedStateRegistryOwner,
): DisposableSaveableStateRegistry {
    val key = "${SaveableStateRegistry::class.simpleName}:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val savedState = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = savedState?.toMap()

    val saveableStateRegistry = SaveableStateRegistry(restored) { true }
    val registered =
        try {
            androidxRegistry.registerSavedStateProvider(key) {
                saveableStateRegistry.performSave().toSavedState()
            }
            true
        } catch (ignore: IllegalArgumentException) {
            false
        }
    return DisposableSaveableStateRegistry(saveableStateRegistry) {
        if (registered) {
            androidxRegistry.unregisterSavedStateProvider(key)
        }
    }
}

/** [SaveableStateRegistry] which can be disposed using [dispose]. */
internal class DisposableSaveableStateRegistry(
    saveableStateRegistry: SaveableStateRegistry,
    private val onDispose: () -> Unit,
) : SaveableStateRegistry by saveableStateRegistry {

    fun dispose() {
        onDispose()
    }
}

@Suppress("UNCHECKED_CAST")
private fun SavedState.toMap(): Map<String, List<Any?>> = read {
    toMap() as Map<String, List<Any?>>
}

private fun Map<String, List<Any?>>.toSavedState() = savedState(this)
