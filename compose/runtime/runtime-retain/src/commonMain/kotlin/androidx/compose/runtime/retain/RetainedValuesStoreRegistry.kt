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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.retain.RetainStateProvider.AlwaysRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.retain.impl.checkPrecondition

/**
 * A [RetainedValuesStoreRegistry] creates and manages [RetainedValuesStore] instances for
 * collections of items. This is desirable for components that swap in and out children where each
 * child should be able to retain values when it becomes removed from the composition hierarchy.
 *
 * To use this class, call [getOrCreateRetainedValuesStoreForChild] to instantiate the
 * [RetainedValuesStore] that should be installed for a given child content block. For automatic
 * installation and content tracking, wrap your content in [ProvideChildRetainedValuesStore].
 *
 * You can also install the managed [RetainedValuesStore]s manually by obtaining a
 * [RetainedValuesStore] with [getOrCreateRetainedValuesStoreForChild] and setting it as the
 * [LocalRetainedValuesStore] for your children's content. When a child is being removed, call
 * [startRetainingExitedValues] to begin the transient destruction phase of your retention scenario.
 * After the child has been added back to the composition, invoke [stopRetainingExitedValues] to
 * finalize the restoration of retained values.
 *
 * When a [RetainedValuesStoreRegistry] is no longer used, you must call [dispose] before the
 * provider is garbage collected. This ensures that all retained values are correctly retired.
 * Failure to do so may result in leaked memory from undispatched [RetainObserver.onRetired]
 * callbacks. Instances created by [retainRetainedValuesStoreRegistry] are automatically disposed
 * when the provider stops being retained.
 *
 * @sample androidx.compose.runtime.retain.samples.retainedValuesStoreRegistrySample
 */
public class RetainedValuesStoreRegistry() {
    private var isDisposed = false
    private val childStores = MutableScatterMap<Any?, ControlledRetainedValuesStore>()

    private var parent: RetainStateProvider = NeverRetainExitedValues
    private var isParentRetainingExitedValues = false
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartRetainingExitedValues() {
                setRetainExitedValues(true)
            }

            override fun onStopRetainingExitedValues() {
                setRetainExitedValues(false)
            }
        }

    /**
     * Starts retaining exited values for a child with the given [key]. If a [RetainedValuesStore]
     * has not been created for this key (because [getOrCreateRetainedValuesStoreForChild] was not
     * called for the key or it has been cleared with [clearChild] or [clearChildren]), then this
     * function does nothing. If the [RetainedValuesStore] for the given key is already retaining
     * exited values, the store will not change states. The number of times this function is called
     * is tracked and must be matched by the same number of calls to [stopRetainingExitedValues] for
     * the given key before its kept values will be retired.
     *
     * This function must be called **before** any content for the associated child is removed from
     * the composition hierarchy.
     *
     * @param key The key of the child to begin retention for
     */
    public fun startRetainingExitedValues(key: Any?) {
        val store = childStores[key] ?: return
        store.startRetainingExitedValues()
    }

    /**
     * Stops retaining exited values for a child with the given [key] as previously started by
     * [startRetainingExitedValues]. If the underlying store is not retaining because
     * [startRetainingExitedValues] has not been called, this function will throw an exception. If
     * no such retain store exists because it was cleared with [clearChild] or never created with
     * [getOrCreateRetainedValuesStoreForChild], this function will do nothing.
     *
     * If [startRetainingExitedValues] has been called more than [stopRetainingExitedValues], the
     * store will continue to retain values that have exited the composition until
     * [stopRetainingExitedValues] has been called the same number of times as
     * [startRetainingExitedValues].
     *
     * This function must be called **after** the completion of the frame in which the child content
     * is being restored to allow the restored child to re-consume all of its retained values. You
     * can use [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback] or
     * [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to insert a sufficient delay.
     *
     * @param key The key of the child to end retention for
     * @throws IllegalStateException if [startRetainingExitedValues] is called more times than
     *   [stopRetainingExitedValues] has been called for the given key
     */
    public fun stopRetainingExitedValues(key: Any?) {
        val store = childStores[key] ?: return
        checkPrecondition(store.externalRetainExitedValuesRequests >= 1) {
            "Unexpected call to stopRetainingExitedValues() without a " +
                "corresponding startRetainingExitedValues() for key $key"
        }
        store.stopRetainingExitedValues()
    }

    /**
     * Gets the total number of active requests from [startRetainingExitedValues] for the given
     * [key]. Effectively, this is the number of calls to [startRetainingExitedValues] minus the
     * number of calls to [stopRetainingExitedValues] for the given [key].
     *
     * This counter resets if [clearStore] is called for the given [key]. If the store has not been
     * created for [key] by [getOrCreateRetainedValuesStoreForChild], this function will return `0`.
     *
     * @param key the key of the child to look up
     * @return The number of active requests against the given child to retain exited values
     * @see ControlledRetainedValuesStore.retainExitedValuesRequestsFromSelf
     */
    public fun retainExitedValuesRequestsFor(key: Any?): Int {
        val store = childStores[key] ?: return 0
        return store.externalRetainExitedValuesRequests
    }

    // We manage the parent ourselves without wiring it up, because it is more efficient than
    // attaching a listener. Because of this, we need to manually subtract out the parent count.
    private val ControlledRetainedValuesStore.externalRetainExitedValuesRequests: Int
        get() = retainExitedValuesRequestsFromSelf - if (isParentRetainingExitedValues) 1 else 0

    /**
     * Installs child [content] that should be retained under the given [key].
     * [startRetainingExitedValues] and [stopRetainingExitedValues] and automatically called based
     * on the presence of this composable for the [key].
     *
     * When removed, this composable begins retaining exited values from the [content] lambda under
     * the given [key]. When added back to the composition hierarchy, the store will stop retaining
     * exited values once the composition completes and will release any unused values. The keys
     * used with this method should only be used once per [RetainedValuesStoreRegistry] in a
     * composition.
     *
     * This composable only attempts to manage the retention lifecycle for the [content] and [key]
     * pair. It will retain removed content indefinitely until [clearChild] or [clearChildren] is
     * invoked.
     *
     * @param key The child key associated with the given [content]. This key is used to identify
     *   the retention pool for objects [retained][retain] by the content composable.
     * @param content The composable content to compose with the [RetainedValuesStore] of the given
     *   [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    @Composable
    public fun ProvideChildRetainedValuesStore(key: Any?, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild(key)
        ) {
            content()
            PresenceIndicator(key)
        }
    }

    /**
     * Indicates the presence of [key] in the composition hierarchy. When this composable is added,
     * [stopRetainingExitedValues] is called for the [key]. When this composable is removed,
     * [startRetainingExitedValues] will be called at the completion of the frame.
     *
     * **This composable must be placed such that it appears AFTER the composable content content
     * for [key] in a preorder traversal of the composition hierarchy.** Otherwise, the underlying
     * requests that start and stop retaining exited values may be scheduled in an incorrect order,
     * causing lost values. For example,
     * ```kotlin
     * // Correct ordering.
     * if (showA) {
     *     CompositionLocalProvider(
     *         LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild("A")
     *     ) {
     *         ContentA()
     *     }
     *     PresenceIndicator("A")
     * }
     *
     * // Incorrect ordering.
     * if (showB) {
     *     PresenceIndicator("B")
     *
     *     CompositionLocalProvider(
     *         LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild("B")
     *     ) {
     *         ContentB()
     *     }
     * }
     * ```
     */
    @Composable
    private fun PresenceIndicator(key: Any?) {
        val composer = currentComposer
        DisposableEffect(key) {
            // This key is entering the composition. End retention when the frame completes. Use
            // the request count to not attempt `stopRetainingExitedValues` when we initially enter
            // the composition.
            val endRetainHandle =
                if (retainExitedValuesRequestsFor(key) > 0) {
                    composer.scheduleFrameEndCallback { stopRetainingExitedValues(key) }
                } else {
                    null
                }
            onDispose {
                // This key is exiting the composition. Begin retaining now.
                endRetainHandle?.cancel()
                startRetainingExitedValues(key)
            }
        }
    }

    /**
     * Creates or returns a previously created [RetainedValuesStore] instance for the given [key].
     * The returned [RetainedValuesStore] will be managed by this provider. It will begin retaining
     * if the parent retain store starts retaining or if [startRetainingExitedValues] is called with
     * the same [key], and it will stop retaining with the parent retain store ends retaining and
     * there is no [startRetainingExitedValues] call without a corresponding
     * [stopRetainingExitedValues] call for the specified [key].
     *
     * The first time this function is called for a given [key], a new [RetainedValuesStore] is
     * created for the [key]. When this function is called for the same [key], it will return the
     * same [RetainedValuesStore] it originally returned. If a given [key]'s store is
     * [cleared][clearChild], then a new one will be created for it the next time it is requested
     * via this function.
     *
     * This function must be called before [startRetainingExitedValues] or
     * [stopRetainingExitedValues] is called for those two methods to have any effect on the
     * retention state for the given [key].
     *
     * @param key The [key] to return an existing [RetainedValuesStore] instance for, if one exists,
     *   or to create a new instance for
     * @return A [RetainedValuesStore] instance suitable to be installed as the
     *   [LocalRetainedValuesStore] for the child content with the specified [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    public fun getOrCreateRetainedValuesStoreForChild(key: Any?): RetainedValuesStore {
        checkPrecondition(!isDisposed) {
            "Cannot get a RetainedValuesStore after a RetainedValuesStoreRegistry has been disposed."
        }

        return childStores.getOrPut(key) {
            ControlledRetainedValuesStore().apply {
                if (isParentRetainingExitedValues) startRetainingExitedValues()
            }
        }
    }

    /**
     * When a [RetainStateProvider] is set as the parent of a [RetainedValuesStoreRegistry], the
     * [RetainedValuesStoreRegistry] will mirror the retention state of the parent. If the parent
     * stops retaining, all children that have started retaining via [startRetainingExitedValues]
     * will continue being retained after the parent stops retaining.
     *
     * If this function is called twice, the new parent will replace the old parent. The new
     * parent's state is immediately applied to the child stores.
     *
     * To clear a parent, call this function and pass in either
     * [RetainStateProvider.AlwaysRetainExitedValues] or
     * [RetainStateProvider.NeverRetainExitedValues] depending on whether you want this store to
     * retain exited values in the absence of a parent.
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = this@RetainedValuesStoreRegistry.parent
        this@RetainedValuesStoreRegistry.parent = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        setRetainExitedValues(parent.isRetainingExitedValues)
    }

    /**
     * Removes the [RetainedValuesStore] for the child with the given [key] from this
     * [RetainedValuesStoreRegistry]. If the key doesn't have an associated [RetainedValuesStore]
     * yet (either because it hasn't been created or has already been cleared), this function does
     * nothing.
     *
     * If the store being cleared is currently retaining exited values, it will stop as a result of
     * this call. If a child with the given [key] is currently in the composition hierarchy, its
     * retained values will not be persisted the next time the child content is destroyed. Orphaned
     * RetainedValuesStores will never begin retaining exited values, and the content will need to
     * be recreated with a new RetainedValuesStore before exited values will be kept again.
     *
     * If [getOrCreateRetainedValuesStoreForChild] is called again for the given [key], a new
     * [RetainedValuesStore] will be created and returned.
     *
     * @param key The key of the child content whose [RetainedValuesStore] should be discarded
     */
    public fun clearChild(key: Any?) {
        childStores.remove(key)?.let { clearStore(it) }
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
        childStores.removeIf { key, store -> predicate(key).also { if (it) clearStore(store) } }
    }

    private fun clearStore(store: ControlledRetainedValuesStore) {
        while (store.retainExitedValuesRequestsFromSelf > 0) store.stopRetainingExitedValues()
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

    private fun setRetainExitedValues(shouldRetain: Boolean) {
        if (shouldRetain == isParentRetainingExitedValues) return
        isParentRetainingExitedValues = shouldRetain

        if (shouldRetain) {
            childStores.forEachValue { store -> store.startRetainingExitedValues() }
        } else {
            childStores.forEachValue { store -> store.stopRetainingExitedValues() }
        }
    }
}

/**
 * Returns a [retain] instance of a new [RetainedValuesStoreRegistry]. A RetainedValuesStoreRegistry
 * is a container of [RetainedValuesStore]s that allows a parent composable to have children with
 * different retention lifecycles. See [RetainedValuesStoreRegistry] for more information on how to
 * use this class, including a sample.
 *
 * The returned provider will be parented to the [LocalRetainedValuesStore] at this point in the
 * composition hierarchy. If the [LocalRetainedValuesStore] is changed, the returned provider will
 * be re-parented to the new [LocalRetainedValuesStore]. When this invocation leaves composition, it
 * will continue retaining if its parent store was retaining. When this
 * [RetainedValuesStoreRegistry] is retired, its child stores will also be retired and the store
 * will be [disposed][RetainedValuesStoreRegistry.dispose].
 *
 * This method is intended to be used for managing retained values in composables that swap in and
 * out children arbitrarily.
 */
@Composable
public fun retainRetainedValuesStoreRegistry(): RetainedValuesStoreRegistry {
    val provider = retain { RetainedValuesStoreRegistryWrapper() }.retainedValuesStoreRegistry
    val parentStore = LocalRetainedValuesStore.current
    DisposableEffect(parentStore) {
        provider.setParentRetainStateProvider(parentStore)
        onDispose {
            provider.setParentRetainStateProvider(
                parent =
                    if (parentStore.isRetainingExitedValues) {
                        AlwaysRetainExitedValues
                    } else {
                        NeverRetainExitedValues
                    }
            )
        }
    }
    return provider
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
