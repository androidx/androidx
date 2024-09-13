/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import androidx.annotation.CheckResult

/**
 * A utility that creates [Runnable] objects that can be [run][Runnable.run] more than once, with
 * the guarantee that [callback] is executed only once.
 *
 * It operates in a cycle that alternates between two distinct phases - a setup phase and an
 * execution phase. The setup phase consists of calling [setUp] one or more times and registering
 * each resulting [Runnable] to be [run][Runnable.run] later. The execution phase is when those
 * [Runnable] objects are [run][Runnable.run], de-duplicated such that [callback] is executed only
 * once. After this, a new cycle is begun by entering the setup phase again.
 *
 * This is useful with a callback framework that doesn't deduplicate the [Runnable] instances that
 * it is given (e.g. [android.view.View.postOnAnimation]), initiated from call sites where there is
 * not an obvious boundary between the setup phase and the execution phase (e.g. event handlers like
 * [android.view.View.onTouchEvent]).
 *
 * For example, to perform some action on Android once per frame when there has been some user
 * input, after all the input for the frame has been processed:
 * ```
 * val doAtEndOfFrame = Runnable {
 *   // Non-idempotent logic that should only be run once per frame.
 *   // ...
 * }
 * val doOnceAtEndOfFrame = AtMostOnceAfterSetUp(doAtEndOfFrame)
 *
 * fun onTouch(v: View, event: MotionEvent) {
 *   // Some touch handling logic.
 *
 *   // Incorrect - would potentially execute the non-idempotent logic multiple times per frame,
 *   // once for every call to `onTouch` within the frame.
 *   // v.postOnAnimation(doAtEndOfFrame)
 *
 *   // Correct - if `onTouch` is called multiple times within this frame, or even if this line is
 *   // repeated multiple times within this `onTouch` call, the non-idempotent logic in
 *   // `doAtEndOfFrame` will be executed just once.
 *   v.postOnAnimation(doOnceAtEndOfFrame.setUp())
 * }
 * ```
 */
internal class AtMostOnceAfterSetUp(private val callback: () -> Unit) {

    /**
     * Flipped to `true` by [setUp] to allow [callback] to execute again, and flipped back to
     * `false` when [callback] is run to allow for another cycle.
     */
    private var isSetUp = false

    /**
     * This will be [run][Runnable.run] once for each call to [setUp], so use [isSetUp] state to
     * ensure that [callback] is executed only once despite potentially multiple calls to [setUp]
     * and therefore multiple [runs][Runnable.run] of this [Runnable].
     */
    private val runnable = Runnable {
        // Do nothing if `callback` has already been executed.
        if (!isSetUp) return@Runnable

        // `callback` is being executed, so set the flag to ensure it cannot be executed again until
        // `setUp` is called again in the setup phase of the next cycle.
        isSetUp = false
        callback()
    }

    /**
     * Returns a [Runnable], which when [run][Runnable.run], executes [callback] if [callback]
     * hasn't yet been executed, or does nothing if [callback] has already been executed by a
     * previous call to [Runnable.run]. After [callback] has been executed, a subsequent call to
     * [setUp] will start the cycle anew and allow [callback] to be executed as a response to one or
     * more [setUp] calls (and [running][Runnable.run] their resulting [Runnable] objects).
     *
     * Be sure to use the return value [Runnable], for example by registering it with
     * [android.view.View.postOnAnimation].
     */
    @CheckResult
    fun setUp(): Runnable {
        // Mark `callback` as able to be executed, but just once until `setUp` is called again to
        // flip
        // this flag back to `true`.
        isSetUp = true
        return runnable
    }
}
