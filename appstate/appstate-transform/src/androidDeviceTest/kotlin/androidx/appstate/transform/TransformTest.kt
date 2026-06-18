/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TransformTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBasicTransform() =
        runTest(testDispatcher) {
            val state =
                transform(
                    defaultValue = 0,
                    context = backgroundScope.coroutineContext,
                    scope = backgroundScope,
                ) {
                    1
                }
            assertThat(state.value).isEqualTo(1)
        }

    @Test
    fun testStateUpdate() =
        runTest(testDispatcher) {
            var sourceState by mutableStateOf(0)
            val state =
                transform(
                    defaultValue = 0,
                    context = backgroundScope.coroutineContext,
                    scope = backgroundScope,
                ) {
                    sourceState
                }

            assertThat(state.value).isEqualTo(0)

            sourceState = 1
            runCurrent()

            assertThat(state.value).isEqualTo(1)
        }

    @Test
    fun testCancellation() =
        runTest(testDispatcher) {
            val childJob = Job(parent = backgroundScope.coroutineContext[Job])
            val childScope = CoroutineScope(backgroundScope.coroutineContext + childJob)
            var sourceState by mutableStateOf(0)
            val state =
                transform(
                    defaultValue = 0,
                    context = childScope.coroutineContext,
                    scope = childScope,
                ) {
                    sourceState
                }

            assertThat(state.value).isEqualTo(0)

            sourceState = 1
            runCurrent()
            assertThat(state.value).isEqualTo(1)

            childScope.cancel()

            sourceState = 2
            runCurrent()
            // Should still be 1 because the scope was cancelled.
            assertThat(state.value).isEqualTo(1)
        }

    @Test
    fun testSingleStateFlowToState() =
        runTest(testDispatcher) {
            val flow = MutableStateFlow(0)
            val state =
                transform(
                    defaultValue = -1,
                    context = backgroundScope.coroutineContext,
                    scope = backgroundScope,
                ) {
                    flow.collectAsState().value
                }

            assertThat(state.value).isEqualTo(0)

            flow.value = 1
            runCurrent()
            assertThat(state.value).isEqualTo(1)
        }

    @Test
    fun testMultipleStateFlowsToState() =
        runTest(testDispatcher) {
            val flow1 = MutableStateFlow(1)
            val flow2 = MutableStateFlow(2)
            val flow3 = MutableStateFlow(3)

            val state =
                transform(
                    defaultValue = 0,
                    context = backgroundScope.coroutineContext,
                    scope = backgroundScope,
                ) {
                    val v1 = flow1.collectAsState().value
                    val v2 = flow2.collectAsState().value
                    val v3 = flow3.collectAsState().value
                    v1 + v2 + v3
                }

            assertThat(state.value).isEqualTo(6)

            flow1.value = 10
            runCurrent()
            assertThat(state.value).isEqualTo(15)

            flow2.value = 20
            flow3.value = 30
            runCurrent()
            assertThat(state.value).isEqualTo(60)
        }
}
