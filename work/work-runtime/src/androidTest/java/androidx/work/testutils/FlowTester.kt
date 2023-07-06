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

package androidx.work.testutils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun <T> CoroutineScope.launchTester(flow: Flow<T>): FlowTester<T> {
    val tester = FlowTester(flow)
    // we don't block parent from completing and simply stop collecting once parent is done
    val forked = Job()
    coroutineContext.job.invokeOnCompletion { forked.cancel() }
    launch(Job()) { tester.launch(this) }
    return tester
}

class FlowTester<T>(private val flow: Flow<T>) {
    private val channel = Channel<T>(10)

    suspend fun awaitNext(): T {
        val result = try {
            withTimeout(3000L) { channel.receive() }
        } catch (e: TimeoutCancellationException) {
            throw AssertionError("Didn't receive event")
        }
        val next = channel.tryReceive()
        if (next.isSuccess || next.isClosed)
            throw AssertionError(
                "Two events received instead of one;\n" +
                    "first: $result;\nsecond: ${next.getOrNull()}"
            )
        return result
    }

    fun launch(scope: CoroutineScope) {
        flow.onEach { channel.send(it) }.launchIn(scope)
    }
}
