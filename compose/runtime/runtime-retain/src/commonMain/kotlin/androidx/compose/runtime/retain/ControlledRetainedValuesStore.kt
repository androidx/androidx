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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.retain.RetainStateProvider.AlwaysRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.retain.impl.SafeMultiValueMap
import androidx.compose.runtime.retain.impl.checkPrecondition

/**
 * A [ControlledRetainedValuesStore] is effectively a "Mutable" [RetainedValuesStore]. This store
 * can be used to define a custom retain scenario and supports nesting within another
 * [RetainedValuesStore] via [setParentRetainStateProvider].
 *
 * This class can be used to create your own retention scenario. A retention scenario is a situation
 * in which content is transiently removed from the composition hierarchy and can be restored with
 * the retained values from the previous composition.
 *
 * When using this class to create your own retention scenario, call [startRetainingExitedValues] to
 * make this store start retaining exited values state before any content is transiently removed.
 * When the transiently removed content is restored, call [stopRetainingExitedValues] **after all
 * content has been restored**. You can use
 * [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback] or
 * [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to ensure that all content has
 * settled in subcompositions and movable content that may not be realized or applied in as part of
 * a composition that is currently ongoing.
 *
 * To create a [ControlledRetainedValuesStore] that is managed entirely within the composition
 * hierarchy, you can use [retainControlledRetainedValuesStore] to create a
 * [ControlledRetainedValuesStore] that is automatically parented to the current
 * [LocalRetainedValuesStore].
 */
public class ControlledRetainedValuesStore : RetainedValuesStore() {
    private val keptExitedValues = SafeMultiValueMap<Any, Any?>()

    private var parentStore: RetainStateProvider = NeverRetainExitedValues
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartRetainingExitedValues() {
                requestRetainExitedValues()
            }

            override fun onStopRetainingExitedValues() {
                unRequestRetainExitedValues()
            }
        }

    /**
     * Returns the number of calls to [startRetainingExitedValues] - [stopRetainingExitedValues].
     * Effectively, the total number of active requests to this [ControlledRetainedValuesStore].
     *
     * Note that this value **ignores any parent state.** It only counts explicit requests from the
     * user to [startRetainingExitedValues]. This store could still be retaining if this value is
     * `0` if the parent store is retaining. This is useful if you want to track your contributions
     * to this store's state, ignoring the parent.
     */
    public val retainExitedValuesRequestsFromSelf: Int
        get() = retainExitedValuesRequests - if (parentStore.isRetainingExitedValues) 1 else 0

    /**
     * Calling this function will automatically mirror the state of [isRetainingExitedValues] to
     * match [parent]'s state. This is an addition to requests made on the
     * [ControlledRetainedValuesStore], so retaining exited values is a function of whether the
     * parent is retaining exited values OR this store has been requested to retain exited values.
     *
     * A [ControlledRetainedValuesStore] can only have one parent. If a new parent is provided, it
     * will replace the old one and will match the new parent's [isRetainingExitedValues] state.
     * This may cause this store to start or stop retaining exited values if this store has no other
     * active requests from [startRetainingExitedValues].
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = parentStore
        parentStore = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        if (parent.isRetainingExitedValues) startRetainingExitedValues()
        if (oldParent.isRetainingExitedValues) stopRetainingExitedValues()
    }

    /**
     * Indicates that this store should continue to retain values that exit the composition. If this
     * store is already in this mode, the store will not change states. The number of times this
     * function is called is tracked and must be matched by the same number of calls to
     * [stopRetainingExitedValues] before the kept values will be retired.
     */
    public fun startRetainingExitedValues(): Unit = requestRetainExitedValues()

    /**
     * Stops retaining values that exit the composition. This function cancels a request that
     * previously began by calling [startRetainingExitedValues]. If [startRetainingExitedValues] has
     * been called more than [stopRetainingExitedValues], the store will continue to retain values
     * that have exited the composition until [stopRetainingExitedValues] has been called the same
     * number of times as [startRetainingExitedValues].
     *
     * @throws IllegalStateException if [stopRetainingExitedValues] is called more times than
     *   [startRetainingExitedValues]
     */
    public fun stopRetainingExitedValues(): Unit = unRequestRetainExitedValues()

    override fun onStartRetainingExitedValues() {
        checkPrecondition(keptExitedValues.isEmpty()) {
            "Attempted to start retaining exited values with pending exited values"
        }
    }

    override fun onStopRetainingExitedValues() {
        keptExitedValues.forEachValue { value -> if (value is RetainObserver) value.onRetired() }
        keptExitedValues.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getExitedValueOrElse(key: Any, defaultValue: Any?): Any? {
        return keptExitedValues.removeLast(key, defaultValue)
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        keptExitedValues.add(key, value)
    }
}

/**
 * Retains a [ControlledRetainedValuesStore] that is nested under the current
 * [LocalRetainedValuesStore] and has no other defined retention scenarios.
 *
 * A [ControlledRetainedValuesStore] created in this way will mirror the retention behavior of
 * [LocalRetainedValuesStore]. When the parent store begins retaining its values, the returned store
 * will receive a request to start retaining values as well. When the parent store stops retaining
 * values, that request is cleared.
 *
 * This API is available as a building block for other retain stores defined in composition. To
 * define your own retention scenario, call
 * [ControlledRetainedValuesStore.startRetainingExitedValues] and
 * [ControlledRetainedValuesStore.stopRetainingExitedValues] on the returned scope as appropriate.
 * You must also install this scope in the composition hierarchy by providing it as the value of
 * [LocalRetainedValuesStore].
 *
 * When this value stops being retained, it will automatically stop retaining exited values,
 * regardless of how many times [ControlledRetainedValuesStore.startRetainingExitedValues] was
 * called.
 *
 * @return A [ControlledRetainedValuesStore] nested under the [LocalRetainedValuesStore], ready to
 *   be installed in the composition hierarchy and be used to define a retention scenario.
 * @sample androidx.compose.runtime.retain.samples.retainControlledRetainedValuesStoreSample
 * @see RetainedContentHost
 */
@Composable
public fun retainControlledRetainedValuesStore(): ControlledRetainedValuesStore {
    val retainedValuesStore =
        retain { RetainControlledRetainedValuesStoreWrapper() }.retainedValuesStore

    val parentStore = LocalRetainedValuesStore.current
    DisposableEffect(parentStore) {
        retainedValuesStore.setParentRetainStateProvider(parentStore)
        onDispose {
            // Keep the parent's state until we get a new store. This lets us continue
            // retaining when the composition hierarchy is destroyed and this parent is removed.
            retainedValuesStore.setParentRetainStateProvider(
                if (parentStore.isRetainingExitedValues) {
                    AlwaysRetainExitedValues
                } else {
                    NeverRetainExitedValues
                }
            )
        }
    }

    return retainedValuesStore
}

private class RetainControlledRetainedValuesStoreWrapper : RetainObserver {
    val retainedValuesStore = ControlledRetainedValuesStore()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        // The retainedValuesStore has stopped being retained. Dispose it.
        retainedValuesStore.setParentRetainStateProvider(NeverRetainExitedValues)
        while (retainedValuesStore.isRetainingExitedValues) retainedValuesStore
            .stopRetainingExitedValues()
    }

    override fun onUnused() {
        // Need to clean up if the store is abandoned, in case our parent caused us
        // to initialize with kept values.
        onRetired()
    }
}
