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
import androidx.health.connect.client.testing.stubs.MutableStub
import androidx.health.connect.client.testing.stubs.buildStub
import androidx.health.connect.client.testing.stubs.enqueue
import androidx.health.connect.client.testing.stubs.plusAssign

/** MutableStub with a default value and a new item enqueued after consumption. */
@Sampled
fun simpleMutableStub() {
    val simpleMutableStub = MutableStub("default")
    simpleMutableStub.next(request = 0) // Returns "default"

    simpleMutableStub.enqueue("new")

    simpleMutableStub.next(request = 0) // Returns "new"
    simpleMutableStub.next(request = 0) // Returns "default"
}

/** MutableStub with an item in the queue and a default value, alternative construction. */
@Sampled
fun simpleMutableStub2() {
    val mutableStub =
        MutableStub<Any, String>().apply {
            enqueue("first")
            defaultHandler = { "Default" }
        }
    mutableStub.next(request = 0) // Returns "first"
    mutableStub.next(request = 0) // Returns "Default"
}

/**
 * Mutable stub with 1 element in a queue that once consumed returns a fixed response which depends
 * on the input
 */
@Sampled
fun fullMutableStub() {
    val mutableStub = MutableStub<Int, String>(defaultHandler = { it.toString() })
    mutableStub.enqueue("zero")
    mutableStub += "one" // += is equivalent to enqueue()
    mutableStub.next(request = 42) // Returns "zero"
    mutableStub.next(request = 42) // Returns "one"
    mutableStub.next(request = 42) // Returns "42"

    // A new response can be enqueued at any time
    mutableStub.enqueue("new")
    // The default handler can also change at any time
    mutableStub.defaultHandler = { (it + 1).toString() }
    mutableStub.next(request = 42) // Returns "new"
    mutableStub.next(request = 42) // Returns "43"
}

/** Mutable stub created with the [buildStub] builder. */
@Sampled
fun builderMutableStub() {
    val builderMutableStub =
        buildStub<Int, String> {
            enqueue("first")
            defaultHandler = { "Default" }
        }
    builderMutableStub.next(request = 0) // Returns "first"
    builderMutableStub.next(request = 0) // Returns "Default"
}
