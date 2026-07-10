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
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateSet
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
     * Returns the set of all keys currently registered in this [SaveableStateHolder].
     *
     * These are the keys that were passed to [SaveableStateProvider] and have not yet been removed
     * via [removeState].
     */
    public val keys: Set<Any>
        // Default to `emptySet` to preserve backward compatibility for existing
        // implementations that don't override this new property.
        get() = emptySet()

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

    // Lazily allocated to avoid overhead in high-frequency usage
    // (e.g., per lazy list item) unless explicitly read by a consumer.
    private var _keys: SnapshotStateSet<Any>? = null
    override val keys: Set<Any>
        get() {
            var keys = _keys
            if (keys == null) {
                // When initialized, we populate it with keys from both:
                // - `registries`: keys that are currently active (composed).
                // - `savedStates`: keys that are currently inactive (disposed but not removed).
                // Note that we rely on `savedStates` keeping track of all disposed keys (even
                // those with empty state) to ensure this set is complete.
                keys = mutableStateSetOf()
                registries.forEachKey { key -> keys += key }
                savedStates.keys.forEach { key -> keys += key }
                _keys = keys
            }
            return keys
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
                _keys?.add(key)
                registries[key] = registry
                onDispose {
                    if (registries.remove(key) === registry) {
                        savedStates[key] = registry.performSave()
                    }
                }
            }
        }
    }

    override fun removeState(key: Any) {
        _keys?.remove(key)
        if (registries.remove(key) == null) {
            savedStates -= key
        }
    }

    companion object {
        val Saver: Saver<SaveableStateHolderImpl, *> =
            Saver(
                // Only save the state if it contains actual data. If the internal state
                // map is empty (e.g., no `rememberSaveable` was used), we drop the key
                // to optimize bundle size. This means these keys won't be restored in
                // the keys set after process death.
                save = { holder ->
                    holder.registries.forEach { key, registry ->
                        holder.savedStates[key] = registry.performSave()
                    }
                    holder.savedStates.values.removeAll { it.isEmpty() }
                    holder.savedStates.ifEmpty { null }
                },
                restore = { savedStates -> SaveableStateHolderImpl(savedStates) },
            )
    }
}
