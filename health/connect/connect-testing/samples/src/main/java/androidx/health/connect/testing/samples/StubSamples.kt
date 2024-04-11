/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.testing.samples

import androidx.annotation.Sampled
import androidx.health.connect.client.testing.stubs.Stub
import androidx.health.connect.client.testing.stubs.stub

/** Stub where output depends on input */
@Sampled
fun stub_mapping() {
    val stub: Stub<Int, String> = Stub { if (it == 0) "zero" else it.toString() }
    stub.next(request = 0) // Returns "zero"
    stub.next(request = 1) // Returns "1"
    stub.next(request = 2) // Returns "2"
}

/** Stub where the output is fixed */
@Sampled
fun stub_defaultOnly() {
    val stub: Stub<Int, String> = stub("Fixed response")
    stub.next(request = 0) // Returns "Fixed response"
    stub.next(request = 1) // Returns "Fixed response"
}

/** Stub with 2 elements in a queue that once consumed returns a fixed response */
@Sampled
fun stub_defaultAndQueue() {
    val stub: Stub<Int, String> = stub(listOf("zero", "one")) { "Default" }
    stub.next(request = 0) // Returns "zero"
    stub.next(request = 0) // Returns "one"
    stub.next(request = 0) // Returns "Default"
}

/** Stub that throws an exception when used */
@Sampled
fun stub_defaultException() {
    val stubWithException: Stub<Int, String> = stub { throw Exception() }
    stubWithException.next(request = 0) // Throws exception
    stubWithException.next(request = 0) // Throws exception
}

/** Stub with 1 element in a queue that once consumed throws an exception when used */
@Sampled
fun stub_queueAndDefaultException() {
    val stubWithQueueAndException: Stub<Int, String> =
        stub(queue = listOf("first")) { throw Exception() }
    stubWithQueueAndException.next(request = 0) // Returns "first"
    stubWithQueueAndException.next(request = 0) // Throws exception
}
