/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp.test.util

import android.util.Log

private const val TAG = "RetryBlock"

class RetryException(msg: String) : Exception(msg)

/**
 * Retry a block several times to deal with test flakiness that is out of our control. The given
 * [tryBlock] will be executed at most [n] times, retrying it when a [RetryException] is thrown.
 * Before a retry, the [resetBlock] is executed to get the system in a stable state. Any other
 * exception that is thrown from the tryBlock is not caught and will not lead to a retry. If the
 * block throws a RetryException on the nth execution, it is rethrown as an [AssertionError].
 *
 * The block should only throw this exception if it detects a specific circumstance that will lead
 * to a flaking test. This typically deals with timing issues where the correct setup of a test
 * depends on time sensitive steps, such as injecting motion events to simulate a swipe. If those
 * events are not delivered on time, the VelocityTracker that tracks the gesture may calculate an
 * incorrect velocity.
 *
 * *Only* use this retry mechanism for retrying code that sets up the test situation necessary to
 * test your functionality, *do not* use it to retry the test code itself.
 */
fun tryNTimes(n: Int, resetBlock: () -> Unit, tryBlock: () -> Unit) {
    repeat(n) { i ->
        try {
            tryBlock()
            return
        } catch (e: RetryException) {
            if (i < n - 1) {
                Log.w(TAG, "Bad state, retrying block", e)
            } else {
                throw AssertionError("Block hit bad state $n times", e)
            }
            resetBlock()
        }
    }
}
