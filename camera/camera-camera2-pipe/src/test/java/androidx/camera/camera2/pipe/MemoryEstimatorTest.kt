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

package androidx.camera.camera2.pipe

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class MemoryEstimatorTest {
    @Test
    fun unboundedEstimator_canAlwaysAllocate() {
        val estimator = MemoryEstimator.create()

        assertThat(estimator.canAllocateNow(10L)).isTrue()
        assertThat(estimator.canAllocateNow(Long.MAX_VALUE)).isTrue()
    }

    @Test
    fun impl_initialStateIsCorrect() = runTest {
        val estimator = MemoryEstimator.create(initialCapacity = 1000L)

        assertThat(estimator.memoryUsage.value).isEqualTo(0L)
        assertThat(estimator.capacityFlow.first()).isEqualTo(1000L)
        assertThat(estimator.evictableMemory.value).isEqualTo(0L)
    }

    @Test
    fun impl_incrementUsageUpdatesCapacityAndUsage() = runTest {
        val estimator = MemoryEstimator.create(initialCapacity = 1000L)

        estimator.incrementUsage(300L)

        assertThat(estimator.memoryUsage.value).isEqualTo(300L)
        assertThat(estimator.capacityFlow.first()).isEqualTo(700L)
    }

    @Test
    fun impl_incrementUsageAllowsNegativeCapacity() = runTest {
        val estimator = MemoryEstimator.create(1000L)

        estimator.incrementUsage(1200L)

        assertThat(estimator.memoryUsage.value).isEqualTo(1200L)
        assertThat(estimator.capacityFlow.first()).isEqualTo(-200L)
    }

    @Test
    fun impl_decrementUsageUpdatesCapacityAndUsage() = runTest {
        val estimator = MemoryEstimator.create(1000L)

        estimator.incrementUsage(500L)
        assertThat(estimator.memoryUsage.value).isEqualTo(500L)
        assertThat(estimator.capacityFlow.first()).isEqualTo(500L)

        estimator.decrementUsage(200L)
        assertThat(estimator.memoryUsage.value).isEqualTo(300L)
        assertThat(estimator.capacityFlow.first()).isEqualTo(700L)
    }

    @Test
    fun impl_updateEvictableModifiesState() {
        val estimator = MemoryEstimator.create(1000L)

        // Add to evictable
        estimator.incrementEvictableBytes(200L)
        assertThat(estimator.evictableMemory.value).isEqualTo(200L)

        // Subtract from evictable (resource going from evictable to non-evictable)
        estimator.decrementEvictableBytes(50L)
        assertThat(estimator.evictableMemory.value).isEqualTo(150L)
    }

    @Test
    fun impl_canAllocateNowReflectsAvailableCapacity() {
        val estimator = MemoryEstimator.create(1000L)

        // Can allocate exact capacity
        assertThat(estimator.canAllocateNow(1000L)).isTrue()
        // Cannot allocate more than capacity
        assertThat(estimator.canAllocateNow(1001L)).isFalse()

        estimator.incrementUsage(600L)

        assertThat(estimator.canAllocateNow(400L)).isTrue()
        assertThat(estimator.canAllocateNow(401L)).isFalse()
    }
}
