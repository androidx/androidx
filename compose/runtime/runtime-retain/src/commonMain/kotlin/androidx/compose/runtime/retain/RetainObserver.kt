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
 * Objects implementing this interface are notified of their usage in [retain].
 *
 * An object is retained when its first captured as the return result of the calculation block used
 * with `retain`. It is retired when the `retain` invocation is removed from composition and not
 * being retained. In between being retained and retired, objects may enter and exit the
 * composition. This occurs when the retained object is removed from composition but transiently
 * held on to if the removed portion of the composition hierarchy is to be restored with its
 * retained state.
 *
 * When objects implementing this interface are retained or entering composition together, their
 * [onRetained] and [onEnteredComposition] callbacks are dispatched in the order that they were
 * added to the composition. When objects exit composition together, the [onExitedComposition]
 * callback is invoked in the opposite order that the objects appear in the composition hierarchy.
 * There are no ordering guarantees around when or in what order the [onRetired] callback may be
 * dispatched.
 *
 * Objects that implement this interface and used in the composition as the result of a
 * [remember][androidx.compose.runtime.remember] call will receive no callbacks to this interface,
 * and are encouraged to implement [RememberObserver][androidx.compose.runtime.RememberObserver]
 * either additionally or instead of `RetainObserver`.
 */
@Suppress("CallbackName")
public interface RetainObserver {
    /**
     * Called when this object is successfully [retain]ed. This occurs when the result of a [retain]
     * call is successfully created and installed in a [RetainScope] and has the same timing as
     * [onRemembered][androidx.compose.runtime.RememberObserver.onRemembered] for the initial
     * retention of a value.
     *
     * When the composition is successful, this call will be immediately followed by a call to
     * [onEnteredComposition].
     *
     * If the composition is abandoned and the associated [RetainScope] is currently keeping exited
     * values, the value will be retained and may be used in a future (successful) composition. This
     * is the only scenario in which it is possible for a RetainObserver to experience a lifecycle
     * of Retained -> Retired without receiving any calls to [onEnteredComposition].
     *
     * If the composition is unsuccessful and the associated [RetainScope] is not keeping exited
     * values, this callback will be skipped and [onUnused] will be called instead.
     */
    public fun onRetained()

    /**
     * Called after [onRetained] or [onExitedComposition] to indicate that content in the
     * composition hierarchy has successfully realized this value as the return result of a call to
     * [retain]. A composition is currently referencing this object.
     *
     * This callback has the same ordering guarantees as
     * [RememberObserver.onRemembered][androidx.compose.runtime.RememberObserver.onRemembered],
     * relative both to other RetainObservers and other
     * [RememberObserver][androidx.compose.runtime.RememberObserver] instances. This function can be
     * called multiple times and will be invoked only after [onRetained] or [onExitedComposition].
     */
    public fun onEnteredComposition()

    /**
     * Called after [onEnteredComposition] to indicate that the Composition this value was retained
     * for is no longer referencing this object. This object will be retained until [onRetired] or
     * [onEnteredComposition] is called, at which point it can either be released entirely or
     * reused, respectively.
     *
     * This callback is invoked in the opposite order as [onEnteredComposition] and has the same
     * ordering guarantees as
     * [RememberObserver.onForgotten][androidx.compose.runtime.RememberObserver.onForgotten],
     * relative both to other RetainObservers and other
     * [RememberObserver][androidx.compose.runtime.RememberObserver] instances.
     */
    public fun onExitedComposition()

    /**
     * Called to indicate that this value was not used by a composition and will not be retained
     * further. This value will never be reused unless it's returned as a result of the [retain]
     * calculation block (and receives another call to [onRetained]).
     *
     * This callback can be invoked after either [onRetained], to indicate that the value was
     * created but the composition was abandoned (see
     * [androidx.compose.runtime.RememberObserver.onAbandoned]), or after [onExitedComposition].
     * When called after [onExitedComposition], this indicates that this value was previously used
     * in composition, but the content retaining this value has been removed and will not be
     * returned to (either because the relevant [RetainScope] was not retaining at the time of
     * removal, or the removed content was not restored after all retained objects were restored).
     *
     * Implementations of this method can be used to release resources held by the instance.
     *
     * This callback does not guarantee any relative ordering among other RetainObserver instances.
     * Similarly, because the timing of a retainment period comes with no guarantees, there could be
     * an arbitrary delay between [onExitedComposition] and when this method is called — anywhere
     * between immediately after exiting the composition, a single frame later, or indefinitely
     * later.
     */
    public fun onRetired()

    /**
     * Called when this object is returned by [retain] but not successfully applied in a
     * composition. This is analogous to
     * [RememberObserver.onAbandoned][androidx.compose.runtime.RememberObserver.onAbandoned].
     *
     * This method is only called when this value is returned by [retain] and will not receive a
     * call to [onRetained].
     */
    public fun onUnused()
}
