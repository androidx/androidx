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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListenerMapTest {

    companion object {
        class Counter(public var count: Int = 0)
    }

    @Test
    fun runnableMap_add() {
        val map = RunnableListenerMap()
        val counter = Counter()
        map.add(DirectExecutor) { counter.count++ }

        assertThat(counter.count).isEqualTo(0)
        map.fire(Unit)
        assertThat(counter.count).isEqualTo(1)
    }

    @Test
    fun runnableMap_addMultipleListeners() {
        val map = RunnableListenerMap()
        val counter1 = Counter()
        val counter2 = Counter()
        map.add(DirectExecutor) { counter1.count++ }
        map.add(DirectExecutor) { counter1.count++ }
        map.add(DirectExecutor) { counter2.count++ }

        map.fire(Unit)
        assertThat(counter1.count).isEqualTo(2)
        assertThat(counter2.count).isEqualTo(1)

        map.fire(Unit)
        assertThat(counter1.count).isEqualTo(4)
        assertThat(counter2.count).isEqualTo(2)
    }

    @Test
    fun runnableMap_remove() {
        val map = RunnableListenerMap()
        val counter = Counter()
        val listener = Runnable() { counter.count++ }
        map.add(DirectExecutor, listener)

        assertThat(counter.count).isEqualTo(0)
        map.fire(Unit)
        assertThat(counter.count).isEqualTo(1)

        map.remove(listener)
        map.fire(Unit)
        assertThat(counter.count).isEqualTo(1)
    }

    @Test
    fun runnableMap_clear() {
        val map = RunnableListenerMap()
        val counter = Counter()

        map.add(DirectExecutor) { counter.count++ }

        map.fire(Unit)
        assertThat(counter.count).isEqualTo(1)

        map.clear()
        map.fire(Unit)
        assertThat(counter.count).isEqualTo(1)
    }

    @Test
    fun consumerMap_testAddAndRemove() {
        val map = ConsumerListenerMap<Counter>()

        val listener1 = Consumer<Counter> { c -> c.count++ }
        val listener2 = Consumer<Counter> { c -> c.count += 10 }
        val counter = Counter()

        map.add(DirectExecutor, listener1)
        map.add(DirectExecutor, listener2)

        map.fire(counter)
        assertThat(counter.count).isEqualTo(11)

        map.remove(listener2)
        map.fire(counter)
        assertThat(counter.count).isEqualTo(12)
    }
}
