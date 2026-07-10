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

package androidx.compose.runtime.retain

import androidx.collection.MutableScatterMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.impl.checkPrecondition

/**
 * A [RetainedValuesStoreRegistry] creates and manages [RetainedValuesStore] instances for
 * collections of items. This is desirable for components that swap in and out children where each
 * child should be able to retain values when it becomes removed from the composition hierarchy.
 *
 * To use this class, wrap your content in [LocalRetainedValuesStoreProvider] with a unique key.
 * Each content block wrapped in this way will receive a unique [RetainedValuesStore] that retains
 * values when the content is removed from the composition hierarchy. Values are retained for as
 * removed content block until either the registry is [disposed][dispose] or its key is
 * [cleared][clearChild] from this registry.
 *
 * When a [RetainedValuesStoreRegistry] is no longer used, you must call [dispose] before the
 * provider is garbage collected. This ensures that all retained values are correctly retired.
 * Failure to do so may result in leaked memory from undispatched [RetainObserver.onRetired]
 * callbacks. Instances created by [RetainedValuesStoreRegistry] are automatically disposed when the
 * provider stops being retained.
 *
 * This registry is intended to be used for managing multiple [RetainedValuesStore] instances, for
 * child composables that enter and exit the composition at different times and should retain values
 * separately. For example, this component may be used with navigation containers, lists, etc.
 *
 * @sample androidx.compose.runtime.retain.samples.retainedValuesStoreRegistrySample
 */
public class RetainedValuesStoreRegistry {
    private var isDisposed = false
    private val childStores = MutableScatterMap<Any?, ManagedRetainedValuesStore>()

    /**
     * Installs child [content] that should be retained under the given [key]. [key]s must be unique
     * within the composition hierarchy for a given [RetainedValuesStoreRegistry].
     *
     * When removed, this composable retains exited values from the [content] lambda under the given
     * [key]. When added back to the composition hierarchy, the underlying store will restore these
     * values and dispose unused values when the composition completes.
     *
     * This composable only attempts to manage the retention lifecycle for the [content] and [key]
     * pair. It will retain removed content indefinitely until [clearChild] or [clearChildren] is
     * invoked.
     *
     * @param key The unique child key associated with the given [content]. This key is used to
     *   identify the retention pool for objects [retained][retain] by the content composable.
     * @param content The composable content to compose with the [RetainedValuesStore] of the given
     *   [key]
     * @throws IllegalStateException if [dispose] has been called
     * @throws IllegalArgumentException if the same [key] is used twice in the composition hierarchy
     *   under this registry.
     */
    @Composable
    public fun LocalRetainedValuesStoreProvider(key: Any?, content: @Composable () -> Unit) {
        LocalRetainedValuesStoreProvider(
            store = getOrCreateRetainedValuesStoreForChild(key),
            content = content,
        )
    }

    private fun getOrCreateRetainedValuesStoreForChild(key: Any?): RetainedValuesStore {
        checkPrecondition(!isDisposed) {
            "Cannot get a RetainedValuesStore after a RetainedValuesStoreRegistry has been disposed."
        }

        return childStores.getOrPut(key) { ManagedRetainedValuesStore() }
    }

    /**
     * Removes the [RetainedValuesStore] for the child with the given [key] from this
     * [RetainedValuesStoreRegistry]. If the key doesn't have an associated [RetainedValuesStore]
     * yet (either because it hasn't been created or has already been cleared), this function does
     * nothing.
     *
     * If the store being cleared is currently retaining exited values, it will stop as a result of
     * this call. If a child with the given [key] is currently in the composition hierarchy, its
     * retained values will not be persisted the next time the child content is destroyed. Children
     * orphaned this way effectively behave as if their [LocalRetainedValuesStore] is the
     * [ForgetfulRetainedValuesStore] until [LocalRetainedValuesStoreProvider] is recomposed to
     * create a new store.
     *
     * If [LocalRetainedValuesStoreProvider] is called again for the given [key], a new
     * [RetainedValuesStore] will be created for the child.
     *
     * @param key The key of the child content whose [RetainedValuesStore] should be discarded
     */
    public fun clearChild(key: Any?) {
        childStores.remove(key)?.dispose()
    }

    /**
     * Bulk removes all child stores for which the [predicate] returns true. This function follows
     * the same clearing rules as [clearChild].
     *
     * @param predicate The predicate to evaluate on all child keys in the
     *   [RetainedValuesStoreRegistry]. If the predicate returns `true` for a given key, it will be
     *   cleared. If the predicate returns `false` it will remain in the collection.
     * @see clearChild
     */
    public fun clearChildren(predicate: (key: Any?) -> Boolean) {
        childStores.removeIf { key, store -> predicate(key).also { if (it) store.dispose() } }
    }

    /**
     * Removes all child [RetainedValuesStore]s from this [RetainedValuesStoreRegistry] and marks it
     * as ineligible for future use. This is required to invoke when the store is no longer used to
     * retire any retained values. Failing to do so may result in memory leaks from undispatched
     * [RetainObserver.onRetired] and [RetainedEffect] callbacks. When this function is called, all
     * values retained in stores managed by this provider will be immediately retired.
     *
     * If this store has already been disposed, this function will do nothing.
     */
    public fun dispose() {
        isDisposed = true
        clearChildren { true }
    }
}

/**
 * Returns a [retain]ed instance of a new [RetainedValuesStoreRegistry]. A
 * RetainedValuesStoreRegistry is a container of [RetainedValuesStore]s that allows a parent
 * composable to have children with different retention lifecycles. See
 * [RetainedValuesStoreRegistry] for more information on how to use this class, including a sample.
 *
 * When this [RetainedValuesStoreRegistry] is retired, its child stores will also be retired and the
 * store will be [disposed][RetainedValuesStoreRegistry.dispose].
 */
@Composable
public fun retainRetainedValuesStoreRegistry(): RetainedValuesStoreRegistry {
    return retain { RetainedValuesStoreRegistryWrapper() }.retainedValuesStoreRegistry
}

private class RetainedValuesStoreRegistryWrapper : RetainObserver {
    val retainedValuesStoreRegistry = RetainedValuesStoreRegistry()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        retainedValuesStoreRegistry.dispose()
    }

    override fun onUnused() {
        retainedValuesStoreRegistry.dispose()
    }
}
