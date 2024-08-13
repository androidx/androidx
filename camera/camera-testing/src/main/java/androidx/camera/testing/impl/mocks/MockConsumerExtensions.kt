/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.mocks

import androidx.camera.testing.impl.mocks.MockConsumer.NO_TIMEOUT
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor
import androidx.camera.testing.impl.mocks.helpers.CallTimes

/**
 * Verifies if [MockConsumer.accept] method was invoked properly during test.
 *
 * Usually invoked from a method with [org.junit.Test] annotation.
 *
 * @param classType the class type to verify for the parameter of [MockConsumer.accept] method.
 * @param inOrder the [MockConsumer.verifyAcceptCall] method invocations with inOrder = true are
 *   chained together to make sure they were in order.
 * @param timeoutMs the time limit in millis seconds to wait for asynchronous operation.
 * @param callTimes the condition for how many times [MockConsumer.accept] method should be called.
 * @param onCall the callback with a list of instances of [MockConsumer.accept] calls when the
 *   condition is met.
 * @see [MockConsumer.verifyAcceptCall]
 */
public fun <T> MockConsumer<T>.verifyAcceptCallExt(
    classType: Class<*>,
    inOrder: Boolean = false,
    timeoutMs: Long = NO_TIMEOUT,
    callTimes: CallTimes,
    onCall: ((List<T>) -> Unit)? = null,
) {
    val captor = onCall?.let { ArgumentCaptor<T>() }
    verifyAcceptCall(classType, inOrder, timeoutMs, callTimes, captor)
    onCall?.invoke(captor!!.allValues as List<T>)
}
