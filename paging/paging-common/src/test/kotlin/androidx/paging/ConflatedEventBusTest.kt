/*
 * Copyright 2020 The Android Open Source Project
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConflatedEventBusTest {
    val testScope = TestCoroutineScope()

    @Test
    fun noInitialValue() {
        val bus = ConflatedEventBus<Unit>(null)
        val collector = bus.createCollector().also {
            it.start()
        }
        testScope.runCurrent()
        assertThat(
            collector.values
        ).isEmpty()
        bus.send(Unit)
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(Unit)
    }

    @Test
    fun withInitialValue() {
        val bus = ConflatedEventBus<Int>(1)
        val collector = bus.createCollector().also {
            it.start()
        }
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(1)
        bus.send(2)
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(1, 2)
    }

    @Test
    fun allowDuplicateValues() {
        val bus = ConflatedEventBus<Int>(1)
        val collector = bus.createCollector().also {
            it.start()
        }
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(1)
        bus.send(1)
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(1, 1)
    }

    @Test
    fun conflateValues() {
        val bus = ConflatedEventBus<Int>(1)

        val collector = bus.createCollector()
        bus.send(2)
        collector.start()
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(2)
        bus.send(3)
        testScope.runCurrent()
        assertThat(
            collector.values
        ).containsExactly(2, 3)
    }

    @Test
    fun multipleCollectors() {
        val bus = ConflatedEventBus(1)
        val c1 = bus.createCollector().also {
            it.start()
        }
        testScope.runCurrent()
        bus.send(2)
        testScope.runCurrent()
        val c2 = bus.createCollector().also {
            it.start()
        }
        testScope.runCurrent()
        assertThat(c1.values).containsExactly(1, 2)
        assertThat(c2.values).containsExactly(2)
        bus.send(3)
        testScope.runCurrent()
        assertThat(c1.values).containsExactly(1, 2, 3)
        assertThat(c2.values).containsExactly(2, 3)
        bus.send(3)
        testScope.runCurrent()
        assertThat(c1.values).containsExactly(1, 2, 3, 3)
        assertThat(c2.values).containsExactly(2, 3, 3)
    }

    private fun <T : Any> ConflatedEventBus<T>.createCollector() = Collector(testScope, this)

    private class Collector<T : Any>(
        private val scope: CoroutineScope,
        private val bus: ConflatedEventBus<T>
    ) {
        private val _values = mutableListOf<T>()
        val values
            get() = _values

        fun start() {
            scope.launch {
                bus.flow.collect {
                    _values.add(it)
                }
            }
        }
    }
}
