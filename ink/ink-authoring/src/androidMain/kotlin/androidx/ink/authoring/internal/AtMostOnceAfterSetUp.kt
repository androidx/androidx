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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A thread-safe utility that creates [Runnable] objects that can be [run][Runnable.run] more than
 * once, with the guarantee that [callback] is executed only once until the next call to [setUp].
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
     * True between the most recent call to [setUp] and the first subsequent execution of the
     * returned [Runnable].
     */
    private val isSetUp = AtomicBoolean(false)

    /** Runs [callback] exactly once after the most recent call to [setUp]. */
    private val runnable = Runnable {
        if (isSetUp.getAndSet(false)) {
            callback()
        }
    }

    /**
     * Returns a [Runnable] which executes [callback] the first time it is run after the most recent
     * call to [setUp] and does nothing until [setUp] is called again.
     */
    @CheckResult
    fun setUp(): Runnable {
        isSetUp.set(true)
        return runnable
    }
}
