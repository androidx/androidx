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

package androidx.compose.runtime.saveable

import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.remember
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * Allows to save the state defined with [rememberSaveable] for the subtree before disposing it to
 * make it possible to compose it back next time with the restored state. It allows different
 * navigation patterns to keep the ui state like scroll position for the currently not composed
 * screens from the backstack.
 *
 * @sample androidx.compose.runtime.saveable.samples.SimpleNavigationWithSaveableStateSample
 *
 * The content should be composed using [SaveableStateProvider] while providing a key representing
 * this content. Next time [SaveableStateProvider] will be used with the same key its state will be
 * restored.
 */
public interface SaveableStateHolder {
    /**
     * Put your content associated with a [key] inside the [content]. This will automatically save
     * all the states defined with [rememberSaveable] before disposing the content and will restore
     * the states when you compose with this key again.
     *
     * @param key to be used for saving and restoring the states for the subtree. Note that on
     *   Android you can only use types which can be stored inside the Bundle.
     * @param content the content for which [key] is associated.
     */
    @Composable public fun SaveableStateProvider(key: Any, content: @Composable () -> Unit)

    /** Removes the saved state associated with the passed [key]. */
    public fun removeState(key: Any)
}

/** Creates and remembers the instance of [SaveableStateHolder]. */
@Composable
public fun rememberSaveableStateHolder(): SaveableStateHolder =
    rememberSaveable(saver = SaveableStateHolderImpl.Saver) { SaveableStateHolderImpl() }
        .apply { parentSaveableStateRegistry = LocalSaveableStateRegistry.current }

private class SaveableStateHolderImpl(
    private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : SaveableStateHolder {
    private val registries = mutableScatterMapOf<Any, SaveableStateRegistry>()
    var parentSaveableStateRegistry: SaveableStateRegistry? = null
    private val canBeSaved: (Any) -> Boolean = {
        parentSaveableStateRegistry?.canBeSaved(it) ?: true
    }

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        ReusableContent(key) {
            val registry = remember {
                require(canBeSaved(key)) {
                    "Type of the key $key is not supported. On Android you can only use types " +
                        "which can be stored inside the Bundle."
                }
                SaveableStateRegistryWrapper(
                    base = SaveableStateRegistry(restoredValues = savedStates[key], canBeSaved)
                )
            }
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides registry,
                LocalSavedStateRegistryOwner provides registry,
                content = content,
            )
            DisposableEffect(Unit) {
                require(key !in registries) { "Key $key was used multiple times " }
                savedStates -= key
                registries[key] = registry
                onDispose {
                    if (registries.remove(key) === registry) {
                        registry.saveTo(savedStates, key)
                    }
                }
            }
        }
    }

    private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
        val map = savedStates
        registries.forEach { key, registry -> registry.saveTo(map, key) }
        return map.ifEmpty { null }
    }

    override fun removeState(key: Any) {
        if (registries.remove(key) == null) {
            savedStates -= key
        }
    }

    private fun SaveableStateRegistry.saveTo(
        map: MutableMap<Any, Map<String, List<Any?>>>,
        key: Any,
    ) {
        val savedData = performSave()
        if (savedData.isEmpty()) {
            map -= key
        } else {
            map[key] = savedData
        }
    }

    companion object {
        val Saver: Saver<SaveableStateHolderImpl, *> =
            Saver(save = { it.saveAll() }, restore = { SaveableStateHolderImpl(it) })
    }
}
