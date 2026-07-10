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

/**
 * A [RetainedValuesStore] acts as a storage area for objects being retained. When a retained value
 * is in the composition, then the composition hierarchy owns the value and retains it there.
 * [RetainedValuesStore] defines what happens to a retained value when it leaves and re-enters the
 * compositions.
 *
 * Retained objects are offered to implementations of [RetainedValuesStore] as they exit the
 * composition. Implementations may continue to retained such exited objects until they are restored
 * into the composition hierarchy when the removed content is recreated. Implementations define
 * whether, how, and for how long retained values should be stored after they leave the composition.
 *
 * The general pattern for retention is as follows:
 * 1. The RetainedValuesStore receives a call to [onContentExitComposition] signaling that the owned
 *    content is about to be removed.
 * 2. Transient content removal begins. The content is recomposed, removed from the hierarchy, and
 *    remembered values are forgotten. Values remembered by [retain] leave the composition but are
 *    not yet released. Every value returned by [retain] will be passed as an argument to
 *    [saveExitingValue] so that it can later be returned by [consumeExitedValueOrDefault].
 * 3. An arbitrary amount of time passes, and the store is installed in the composition hierarchy
 *    again along with its content. When a [retain] call is invoked during the restoration, it calls
 *    [consumeExitedValueOrDefault]. If all the input keys match a retained value, the previous
 *    result is returned and the retained value is removed from the pool of restorable objects that
 *    exited the previous composition. This step may be skipped if it becomes impossible to return
 *    to the transiently removed content while this store is retaining exited values.
 * 4. The content finishes composing after being restored, and the entire frame completes.
 *    [onContentEnteredComposition] is invoked to signal the complete consumption of all retained
 *    exited values. Any values that are retained and not currently used in a composition (and
 *    therefore not restored by [consumeExitedValueOrDefault]) are then immediately discarded.
 *
 * A given `RetainedValuesStore` should only be used by a single
 * [Recomposer][androidx.compose.runtime.Recomposer] at a time. It can move between recomposers (for
 * example, when the Window is recreated), but should never be used by two Recomposers
 * simultaneously. Implementations may or may not support being used in multiple places
 * simultaneously. Unless otherwise specified, it's assumed that an implementation of
 * [RetainedValuesStore] should only be installed at one location in a single composition hierarchy.
 *
 * @see retain
 * @see LocalRetainedValuesStore
 * @see ManagedRetainedValuesStore
 * @see ForgetfulRetainedValuesStore
 */
public interface RetainedValuesStore {

    /**
     * If this store is currently retaining exited values and has a value previously created with
     * the given [key], its original record is returned and removed from the list of exited kept
     * objects that this store is tracking.
     *
     * This method is always called from the composition thread.
     *
     * @param key The keys to resolve a retained value that has left composition
     * @param defaultValue A value to be returned if there are no retained values that have exited
     *   composition and are being held by this RetainedValuesStore for the given [key].
     * @return A retained value for [key] if there is one and it hasn't already re-entered
     *   composition, otherwise [defaultValue].
     */
    public fun consumeExitedValueOrDefault(key: Any, defaultValue: Any?): Any?

    /**
     * Invoked when a retained value is exiting composition while this store is retaining exited
     * values. It is up to the implementation of this method to decide whether and how to store
     * these values so that they can later be retrieved by [consumeExitedValueOrDefault].
     *
     * The given [key] are not guaranteed to be unique. To handle duplicate keys, implementors
     * should return retained values with the same keys from [consumeExitedValueOrDefault] in the
     * opposite order they are received by [saveExitingValue].
     *
     * If the implementation of this store does not accept this value into its kept exited object
     * list, it MUST call [RetainObserver.onRetired] if [value] implements [RetainObserver],
     * following the threading guarantees specified in [RetainObserver]'s documentation.
     *
     * This method is always called from the applier thread.
     */
    public fun saveExitingValue(key: Any, value: Any?)

    /**
     * Invoked to indicate that all content has finished entering the composition for the first time
     * after the store is installed. Any unconsumed retained values should be discarded at this
     * time.
     *
     * This function is called by the library when the store is installed with
     * [LocalRetainedValuesStoreProvider]. It should not be invoked directly in production code. The
     * default installation will always invoke this callback on the applier thread for the content's
     * latest destination, which may differ when moved between compositions.
     *
     * Implementors of this function MUST call [RetainObserver.onRetired] for all discarded values
     * that implement [RetainObserver] following the threading guarantees specified in
     * [RetainObserver]'s documentation.
     */
    public fun onContentEnteredComposition()

    /**
     * Invoked to indicate that the associated content is _about_ to start leaving composition. The
     * store should make any necessary preparations to start retaining values as they exit the
     * composition.
     *
     * This function is called by the library when the store is installed with
     * [LocalRetainedValuesStoreProvider]. It should not be invoked directly in production code. The
     * default installation will always invoke this callback on the applier thread that content
     * entered composition on.
     */
    public fun onContentExitComposition()
}
