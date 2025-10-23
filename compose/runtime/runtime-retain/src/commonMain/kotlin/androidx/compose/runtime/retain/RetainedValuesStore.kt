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

import androidx.collection.MutableScatterSet
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.retain.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.retain.impl.checkPrecondition

/**
 * A RetainedValuesStore acts as a storage area for objects being retained. An instance of a
 * RetainedValuesStore also defines a specific retention policy to describe when removed retained
 * values should be kept for future reuse and when they should be retired.
 *
 * The general pattern for retention is as follows:
 * 1. The RetainedValuesStore receives a notification that transient content removal is about to
 *    begin. The source of this notification varies depending on what retention scenario is being
 *    captured, but could, for example, be a signal that an Android Activity is being recreated, or
 *    that content is about to be navigated away from/collapsed with the potential of being returned
 *    to. At this time, the store's owner should call [requestRetainExitedValues].
 * 2. Transient content removal begins. The content is recomposed, removed from the hierarchy, and
 *    remembered values are forgotten. Values remembered by [retain] leave the composition but are
 *    not yet released. Every value returned by [retain] will be passed as an argument to
 *    [saveExitingValue] so that it can later be returned by [getExitedValueOrDefault].
 * 3. An arbitrary amount of time passes, and the removed content is restored in the composition
 *    hierarchy at its previous location. When a [retain] call is invoked during the restoration, it
 *    calls [getExitedValueOrDefault]. If all the input keys match a retained value, the previous
 *    result is returned and the retained value is removed from the pool of restorable objects that
 *    exited the previous composition. This step may be skipped if it becomes impossible to return
 *    to the transiently removed content while this store is retaining exited values.
 * 4. The content finishes composing after being restored, and the entire frame completes. The owner
 *    of this store should call [unRequestRetainExitedValues]. When retention stops being requested,
 *    it immediately ends. Any values that are retained and not currently used in a composition (and
 *    therefore not restored by [getExitedValueOrDefault]) are then immediately discarded.
 *
 * A given `RetainedValuesStore` should only be used by a single
 * [Recomposer][androidx.compose.runtime.Recomposer] at a time. It can move between recomposers (for
 * example, when the Window is recreated), but should never be used by two Recomposers
 * simultaneously. It is valid for a RetainedValuesStore to be used in multiple compositions at the
 * same time, or in the same composition multiple times.
 *
 * @see retain
 * @see LocalRetainedValuesStore
 * @see ControlledRetainedValuesStore
 * @see ForgetfulRetainedValuesStore
 */
public abstract class RetainedValuesStore : RetainStateProvider {

    protected var retainExitedValuesRequests: Int = 0
        private set

    final override val isRetainingExitedValues: Boolean
        get() = retainExitedValuesRequests > 0

    private val observers = MutableScatterSet<RetainStateObserver>(0)

    /**
     * If this store is currently retaining exited values and has a value previously created with
     * the given [key], its original record is returned and removed from the list of exited kept
     * objects that this store is tracking.
     *
     * @param key The keys to resolve a retained value that has left composition
     * @param defaultValue A value to be returned if there are no retained values that have exited
     *   composition and are being held by this RetainedValuesStore for the given [key].
     * @return A retained value for [key] if there is one and it hasn't already re-entered
     *   composition, otherwise [defaultValue].
     */
    public abstract fun getExitedValueOrElse(key: Any, defaultValue: Any?): Any?

    /**
     * Invoked when a retained value is exiting composition while this store is retaining exited
     * values. It is up to the implementation of this method to decide whether and how to store
     * these values so that they can later be retrieved by [getExitedValueOrElse].
     *
     * The given [key] are not guaranteed to be unique. To handle duplicate keys, implementors
     * should return retained values with the same keys from [getExitedValueOrElse] in the opposite
     * order they are received by [saveExitingValue].
     *
     * If the implementation of this store does not accept this value into its kept exited object
     * list, it MUST call [RetainObserver.onRetired] if [value] implements [RetainObserver].
     */
    protected abstract fun saveExitingValue(key: Any, value: Any?)

    /**
     * Called to increment the number of requests to retain exited values. When there are a positive
     * number of requests, this store begins retaining exited values and continues until all
     * requests are cleared.
     *
     * This method is not thread safe and should only be called on the applier thread.
     */
    protected fun requestRetainExitedValues() {
        if (retainExitedValuesRequests++ == 0) {
            onStartRetainingExitedValues()
            observers.forEach { it.onStartRetainingExitedValues() }
        }
    }

    /**
     * Clears a previous call to [requestRetainExitedValues]. If all requests to retain exited
     * values have been cleared, this store will stop retaining exited values.
     *
     * This method is not thread safe and should only be called on the applier thread.
     *
     * @throws IllegalStateException if [unRequestRetainExitedValues] is called more times than
     *   [requestRetainExitedValues] has been called.
     */
    protected fun unRequestRetainExitedValues() {
        checkPrecondition(isRetainingExitedValues) {
            "Unexpected call to unRequestRetainExitedValues() without a " +
                "corresponding requestRetainExitedValues()"
        }
        if (--retainExitedValuesRequests == 0) {
            onStopRetainingExitedValues()
            observers.forEach { it.onStopRetainingExitedValues() }
        }
    }

    /**
     * Called when this store first starts to retain exited values (i.e. when
     * [isRetainingExitedValues] transitions from false to true). When this is called, implementors
     * should prepare to begin to store values they receive from [saveExitingValue].
     */
    protected abstract fun onStartRetainingExitedValues()

    /**
     * Called when this store stops retaining exited values (i.e. when [isRetainingExitedValues]
     * transitions from true to false). After this is called, all exited values that have been kept
     * and not restored via [getExitedValueOrElse] should be retired.
     *
     * Implementors MUST invoke [RetainObserver.onRetired] for all exited and unrestored
     * [RememberObservers][RememberObserver] when this method is invoked.
     */
    protected abstract fun onStopRetainingExitedValues()

    final override fun addRetainStateObserver(observer: RetainStateObserver) {
        observers += observer
    }

    final override fun removeRetainStateObserver(observer: RetainStateObserver) {
        observers -= observer
    }

    internal class RetainedValueHolder<out T>
    internal constructor(
        val key: Any,
        val value: T,
        owner: RetainedValuesStore,
        private var isNewlyRetained: Boolean,
    ) : RememberObserver {

        var owner: RetainedValuesStore = owner
            private set

        init {
            if (value is RememberObserver && value !is RetainObserver) {
                throw IllegalArgumentException(
                    "Retained a value that implements RememberObserver but not RetainObserver. " +
                        "To receive the correct callbacks, the retained value '$value' must also " +
                        "implement RetainObserver."
                )
            }
        }

        internal fun readoptUnder(newStore: RetainedValuesStore) {
            owner = newStore
        }

        override fun onRemembered() {
            if (value is RetainObserver) {
                if (isNewlyRetained) {
                    isNewlyRetained = false
                    value.onRetained()
                }
                value.onEnteredComposition()
            }
        }

        override fun onForgotten() {
            if (owner.isRetainingExitedValues) {
                owner.saveExitingValue(key, value)
            }

            if (value is RetainObserver) {
                value.onExitedComposition()
                if (!owner.isRetainingExitedValues) value.onRetired()
            }
        }

        override fun onAbandoned() {
            if (owner.isRetainingExitedValues) {
                if (value is RetainObserver) value.onRetained()
                owner.saveExitingValue(key, value)
            } else if (value is RetainObserver) {
                value.onUnused()
            }
        }
    }
}
