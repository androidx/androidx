/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleFlowExtTest {
    val testScope = TestCoroutineScope()

    @Test
    fun scan_basic() = testScope.runBlockingTest {
        val arguments = mutableListOf<Pair<Int, Int>>()
        assertThat(
            flowOf(1, 2, 3).simpleScan(0) { acc, value ->
                arguments.add(acc to value)
                value + acc
            }.toList()
        ).containsExactly(
            0, 1, 3, 6
        ).inOrder()
        assertThat(arguments).containsExactly(
            0 to 1,
            1 to 2,
            3 to 3
        ).inOrder()
    }

    @Test
    fun scan_initialValue() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleScan("x") { _, value ->
                "$value"
            }.toList()
        ).containsExactly("x")
    }

    @Test
    fun runningReduce_basic() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4).simpleRunningReduce { acc, value ->
                acc + value
            }.toList()
        ).containsExactly(1, 3, 6, 10)
    }

    @Test
    fun runningReduce_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleRunningReduce { acc, value ->
                acc + value
            }.toList()
        ).isEmpty()
    }

    @Test
    fun mapLatest() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4)
                .onEach {
                    delay(1)
                }
                .simpleMapLatest { value ->
                    delay(value.toLong())
                    "$value-$value"
                }.toList()
        ).containsExactly(
            "1-1", "4-4"
        ).inOrder()
    }

    @Test
    fun mapLatest_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleMapLatest { value ->
                "$value-$value"
            }.toList()
        ).isEmpty()
    }

    @Test
    fun flatMapLatest() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4)
                .onEach {
                    delay(1)
                }
                .simpleFlatMapLatest { value ->
                    flow {
                        repeat(value) {
                            emit(value)
                        }
                    }
                }.toList()
        ).containsExactly(
            1, 2, 2, 3, 3, 3, 4, 4, 4, 4
        ).inOrder()
    }

    @Test
    fun flatMapLatest_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>()
                .simpleFlatMapLatest {
                    flowOf(it)
                }.toList()
        ).isEmpty()
    }
}