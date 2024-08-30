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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.lifecycle

import androidx.lifecycle.util.ScopesRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LiveDataAsFlowTest {
    @get:Rule
    val scopes = ScopesRule()
    private val mainScope = scopes.mainScope
    private val testScope = scopes.testScope

    @Test
    fun checkCancellation() {
        val ld = MutableLiveData<Int>()
        val flow = ld.asFlow()
        scopes.triggerAllActions()
        // check that flow creation didn't make livedata active
        assertThat(ld.hasActiveObservers()).isFalse()
        val job = testScope.launch { flow.collect { } }
        scopes.triggerAllActions()
        // collection started there should be an active observer
        assertThat(ld.hasActiveObservers()).isTrue()
        job.cancel()
        scopes.triggerAllActions()
        // job with collection was cancelled, so no active observers again
        assertThat(ld.hasActiveObservers()).isFalse()
    }

    @Test
    fun checkCancellationFromInitialValue() {
        val ld = MutableLiveData<Int>()
        ld.value = 1
        val flow = ld.asFlow()
        // check that flow creation didn't make livedata active
        assertThat(ld.hasActiveObservers()).isFalse()
        // Collect only a single value to get the initial value and cancel immediately
        val job = testScope.launch { assertThat(flow.take(1).toList()).isEqualTo(listOf(1)) }
        scopes.triggerAllActions()
        mainScope.launch {
            // This should never be received by the take(1)
            ld.value = 2
        }
        scopes.triggerAllActions()
        // Verify that the job completing removes the observer
        assertThat(job.isCompleted).isTrue()
        assertThat(ld.hasActiveObservers()).isFalse()
    }

    @Test
    fun checkCancellationAfterJobCompletes() {
        val ld = MutableLiveData<Int>()
        ld.value = 1
        val flow = ld.asFlow()
        // check that flow creation didn't make livedata active
        assertThat(ld.hasActiveObservers()).isFalse()
        val job = testScope.launch { assertThat(flow.take(2).toList()).isEqualTo(listOf(1, 2)) }
        scopes.triggerAllActions()
        mainScope.launch {
            // Receiving this should complete the job and remove the observer
            ld.value = 2
        }
        scopes.triggerAllActions()
        // Verify that the job completing removes the observer
        assertThat(job.isCompleted).isTrue()
        assertThat(ld.hasActiveObservers()).isFalse()
    }

    @Test
    fun dispatchMultiple() {
        val ld = MutableLiveData<Int>()
        val collected = mutableListOf<Int>()
        val job = testScope.launch {
            ld.asFlow().collect {
                delay(100)
                collected.add(it)
            }
        }
        mainScope.launch {
            ld.value = 1
            delay(1000)
            ld.value = 2
            ld.value = 3
            ld.value = 4
        }
        scopes.triggerAllActions()
        scopes.advanceTimeBy(200)
        assertThat(collected).isEqualTo(listOf(1))
        scopes.advanceTimeBy(800)
        testScope.testScheduler.advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(1, 2, 4))
        job.cancel()
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isFalse()
    }

    @Test
    fun reusingFlow() {
        val ld = MutableLiveData<Int>()
        val flow = ld.asFlow()
        val firstCollection = testScope.launch {
            assertThat(flow.first()).isEqualTo(1)
        }
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isTrue()

        mainScope.launch { ld.value = 1 }
        scopes.triggerAllActions()
        // check that we're done with previous collection
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(firstCollection.isCompleted).isTrue()

        val secondCollection = testScope.launch {
            assertThat(flow.take(2).toList()).isEqualTo(listOf(1, 2))
        }
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isTrue()
        mainScope.launch { ld.value = 2 }
        scopes.triggerAllActions()
        assertThat(secondCollection.isCompleted).isTrue()
        assertThat(ld.hasActiveObservers()).isFalse()
    }

    @Test
    fun twoFlowsInParallel() {
        val ld = MutableLiveData<Int>()
        val flowA = ld.asFlow()
        val flowB = ld.asFlow()
        assertThat(ld.hasActiveObservers()).isFalse()
        val jobA = testScope.launch {
            assertThat(flowA.first()).isEqualTo(1)
        }
        val jobB = testScope.launch {
            assertThat(flowB.take(2).toList()).isEqualTo(listOf(1, 2))
        }
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isTrue()

        mainScope.launch { ld.value = 1 }
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isTrue()
        assertThat(jobA.isCompleted).isTrue()
        assertThat(jobB.isCompleted).isFalse()
        mainScope.launch { ld.value = 2 }
        scopes.triggerAllActions()
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(jobB.isCompleted).isTrue()
    }
}
