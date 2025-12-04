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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.impl.SafeMultiValueMap
import androidx.compose.runtime.retain.impl.checkPrecondition

/**
 * A [ManagedRetainedValuesStore] is the default implementation of [RetainedValuesStore] that can be
 * used to define custom retention periods in the composition hierarchy.
 *
 * Instances of [ManagedRetainedValuesStore] can only be installed by
 * [LocalRetainedValuesStoreProvider] in one location and composition hierarchy at a time.
 * Repositioning a [ManagedRetainedValuesStore] is allowed within a single frame or by removing the
 * store and adding it again in a later frame.
 *
 * This store retains all exited values when the provider (and its content) are removed from the
 * composition. Optionally, you can toggle whether this scope will retain exited values via
 * [disableRetainingExitedValues] and [enableRetainingExitedValues].
 *
 * When the store is no longer needed, you must call [dispose] on it to ensure that any retained
 * values are released and get calls to [RetainObserver.onRetired]. Failure to do so may result in
 * memory leaks and orphaned jobs.
 *
 * To create a [ManagedRetainedValuesStore] that is owned entirely within the composition hierarchy,
 * you can use [retainManagedRetainedValuesStore] to create a [ManagedRetainedValuesStore] that is
 * automatically scoped to the parent [RetainedValuesStore] and disposed when no longer retained.
 */
public class ManagedRetainedValuesStore : RetainedValuesStore {
    private var isEnabled = true
    private var isDisposed = false
    private var isContentComposed = false
    // TODO(anbailey): For thread safety, we should consider defining SnapshotMultiValueMap instead
    private val keptExitedValues = SafeMultiValueMap<Any, Any?>()

    /**
     * Indicates whether this store will continue to retain values in this store if they exit the
     * composition hierarchy.
     */
    public val isRetainingExitedValues: Boolean
        get() = isEnabled && !isContentComposed

    /**
     * Request that this store should retain exited values the next time that the tracked content
     * exites the composition. If this store is already in this mode, nothing happens.
     *
     * [ManagedRetainedValuesStore]s are initialized in the enabled state.
     *
     * @throws IllegalStateException if [dispose] has been called.
     * @see disableRetainingExitedValues
     */
    public fun enableRetainingExitedValues() {
        checkPrecondition(!isDisposed) {
            "Cannot call enableRetainingExitedValues on a disposed store"
        }
        isEnabled = true
    }

    /**
     * This function will cause the store to never retain values that exit the composition
     * hierarchy. If the store is currently retaining values that have exited the composition, they
     * will be immediately disposed. If the store is already in this mode, nothing happens.
     *
     * When retaining exited values is disabled, this store will behave the same as the
     * [ForgetfulRetainedValuesStore].
     *
     * @see [enableRetainingExitedValues]
     */
    public fun disableRetainingExitedValues() {
        isEnabled = false
        purgeUnusedExitedValues()
    }

    /**
     * Releases the store so that it will never retain exited values. All retained exited values are
     * immediately [retired][RetainObserver.onRetired]. This should be used whenever a
     * [ManagedRetainedValuesStore] is no longer used to ensure correct cleanup of its retained
     * values.
     *
     * Calling this function is equivalent to calling [disableRetainingExitedValues], but prohibits
     * future calls to [enableRetainingExitedValues]. If this function has already been called on a
     * given store, subsequent calls will do nothing.
     *
     * It is safe to install a disposed [ManagedRetainedValuesStore] in the composition hierarchy
     * since exited values under this scope won't be retained and therefore can't leak. Installing a
     * disposed [ManagedRetainedValuesStore] behaves the same as if you had installed the
     * [ForgetfulRetainedValuesStore].
     *
     * Calls to [RetainObserver.onRetired] for subsequently disposed values will be executed on the
     * same thread this method is called on.
     */
    // TODO: Make thread safe.
    public fun dispose() {
        isDisposed = true
        disableRetainingExitedValues()
    }

    override fun onContentExitComposition() {
        if (isDisposed) return

        checkPrecondition(isContentComposed) {
            "ManagedValuesStore tried to leave composition twice. " +
                "Is the store installed in multiple places?"
        }

        checkPrecondition(keptExitedValues.isEmpty()) {
            "Attempted to start retaining exited values with pending exited values"
        }

        isContentComposed = false
    }

    override fun onContentEnteredComposition() {
        if (isDisposed) return

        checkPrecondition(!isContentComposed) {
            "ManagedValuesStore tried to enter composition twice. " +
                "Did you attempt to install the same store multiple times or into two compositions?"
        }

        purgeUnusedExitedValues()
        isContentComposed = true
    }

    private fun purgeUnusedExitedValues() {
        keptExitedValues.forEachValue { value -> if (value is RetainObserver) value.onRetired() }
        keptExitedValues.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun consumeExitedValueOrDefault(key: Any, defaultValue: Any?): Any? {
        return keptExitedValues.removeLast(key, defaultValue)
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        if (isRetainingExitedValues) {
            keptExitedValues.add(key, value)
        } else if (value is RetainObserver) {
            value.onRetired()
        }
    }
}

/**
 * Retains a [ManagedRetainedValuesStore]. The returned store follows the lifespan defined by
 * [retain]. When the retained scope is retired, it will be
 * [disposed][ManagedRetainedValuesStore.dispose].
 *
 * Optionally, you can enable and disable retention of exited values on this scope via
 * [ManagedRetainedValuesStore.enableRetainingExitedValues] and
 * [ManagedRetainedValuesStore.disableRetainingExitedValues]. (The scope is enabled by default.)
 *
 * @return A [ManagedRetainedValuesStore] nested under the [LocalRetainedValuesStore], ready to be
 *   installed with [LocalRetainedValuesStoreProvider].
 * @sample androidx.compose.runtime.retain.samples.retainManagedRetainedValuesStoreSample
 */
@Composable
public fun retainManagedRetainedValuesStore(): ManagedRetainedValuesStore {
    return retain { RetainManagedRetainedValuesStoreWrapper() }.retainedValuesStore
}

private class RetainManagedRetainedValuesStoreWrapper : RetainObserver {
    val retainedValuesStore = ManagedRetainedValuesStore()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        retainedValuesStore.dispose()
    }

    override fun onUnused() {
        retainedValuesStore.dispose()
    }
}
