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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class MemoryEstimatorTest {
    @Test
    fun noOpEstimator_maintainsDefaultState() {
        val estimator = MemoryEstimator.noop

        // Initial state
        assertThat(estimator.capacity.value).isEqualTo(0L)
        assertThat(estimator.evictable.value).isEqualTo(0L)

        // Actions should not mutate state
        estimator.incrementUsage(100L)
        assertThat(estimator.capacity.value).isEqualTo(0L)

        estimator.decrementUsage(50L)
        assertThat(estimator.capacity.value).isEqualTo(0L)

        estimator.updateEvictable(20L)
        assertThat(estimator.evictable.value).isEqualTo(0L)
    }

    @Test
    fun noOpEstimator_canAlwaysAllocate() {
        val estimator = MemoryEstimator.noop

        assertThat(estimator.canAllocate(10L)).isTrue()
        assertThat(estimator.canAllocate(Long.MAX_VALUE)).isTrue()
    }

    @Test
    fun impl_initialStateIsCorrect() {
        val estimator = MemoryEstimator.create(initialCapacity = 1000L)

        assertThat(estimator.capacity.value).isEqualTo(1000L)
        assertThat(estimator.evictable.value).isEqualTo(0L)
    }

    @Test
    fun impl_incrementUsageReducesCapacity() {
        val estimator = MemoryEstimator.create(initialCapacity = 1000L)

        estimator.incrementUsage(300L)
        assertThat(estimator.capacity.value).isEqualTo(700L)
    }

    @Test
    fun impl_incrementUsageAllowsNegativeCapacity() {
        val estimator = MemoryEstimator.create(1000L)

        estimator.incrementUsage(1200L)
        assertThat(estimator.capacity.value).isEqualTo(-200L)
    }

    @Test
    fun impl_decrementUsageIncreasesCapacity() {
        val estimator = MemoryEstimator.create(1000L)

        estimator.incrementUsage(500L)
        assertThat(estimator.capacity.value).isEqualTo(500L)

        estimator.decrementUsage(200L)
        assertThat(estimator.capacity.value).isEqualTo(700L)
    }

    @Test
    fun impl_updateEvictableModifiesState() {
        val estimator = MemoryEstimator.create(1000L)

        // Add to evictable
        estimator.updateEvictable(200L)
        assertThat(estimator.evictable.value).isEqualTo(200L)

        // Subtract from evictable (resource going from evictable to non-evictable)
        estimator.updateEvictable(-50L)
        assertThat(estimator.evictable.value).isEqualTo(150L)
    }

    @Test
    fun impl_canAllocateReflectsAvailableCapacity() {
        val estimator = MemoryEstimator.create(1000L)

        // Can allocate exact capacity
        assertThat(estimator.canAllocate(1000L)).isTrue()
        // Cannot allocate more than capacity
        assertThat(estimator.canAllocate(1001L)).isFalse()

        estimator.incrementUsage(600L)

        assertThat(estimator.canAllocate(400L)).isTrue()
        assertThat(estimator.canAllocate(401L)).isFalse()
    }
}
