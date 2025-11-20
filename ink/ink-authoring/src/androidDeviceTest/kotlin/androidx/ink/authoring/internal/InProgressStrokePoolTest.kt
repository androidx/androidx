/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCustomShapeWorkflowApi::class)
@RunWith(AndroidJUnit4::class)
class InProgressStrokePoolTest {

    @Test
    fun obtain_whenCalledTwiceWithSameShapeType_returnsDifferentInstances() {
        val pool = InProgressStrokePoolImpl(FakeShapeWorkflow())

        val first = pool.obtain(FakeShapeSpec())
        val second = pool.obtain(FakeShapeSpec())

        assertThat(second).isNotSameInstanceAs(first)
    }

    @Test
    fun obtain_whenCalledTwiceWithDifferentShapeType_returnsDifferentInstances() {
        val pool = InProgressStrokePoolImpl(FakeShapeWorkflow())

        val first = pool.obtain(FakeShapeSpec(updatesAfterCompletion = false))
        val second = pool.obtain(FakeShapeSpec(updatesAfterCompletion = true))

        assertThat(second).isNotSameInstanceAs(first)
    }

    @Test
    fun obtain_whenCalledAfterRecycleWithSameShapeType_returnsSameInstance() {
        val pool = InProgressStrokePoolImpl(FakeShapeWorkflow())

        val first = pool.obtain(FakeShapeSpec())
        pool.recycle(first)
        val second = pool.obtain(FakeShapeSpec())

        assertThat(second).isSameInstanceAs(first)
    }

    @Test
    fun obtain_whenCalledAfterRecycleWithDifferentShapeType_returnsDifferentInstance() {
        val pool = InProgressStrokePoolImpl(FakeShapeWorkflow())

        val first = pool.obtain(FakeShapeSpec(updatesAfterCompletion = false))
        pool.recycle(first)
        val second = pool.obtain(FakeShapeSpec(updatesAfterCompletion = true))

        assertThat(second).isNotSameInstanceAs(first)
    }

    @Test
    fun onHandoff_afterMultipleHandoffs_shouldTrimInProgressStrokePoolToMaxCohortSize() {
        val shapeAdapter = FakeShapeWorkflow()
        val inProgressStrokePool = InProgressStrokePoolImpl(shapeAdapter)
        val shapeSpec = FakeShapeSpec()
        val shapeType = shapeAdapter.getShapeType(shapeSpec)

        val cohortSizesForHandoffs = listOf(2, 9, 3, 8, 4, 7, 5, 6, 1, 1, 1, 1, 1, 1)
        val maxOfLast10CohortSizes = listOf(2, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 7)
        check(maxOfLast10CohortSizes.size == cohortSizesForHandoffs.size)

        for (handoffIndex in cohortSizesForHandoffs.indices) {
            val cohortSize = cohortSizesForHandoffs[handoffIndex]
            val cohortInstances = mutableSetOf<FakeInProgressShape>()
            repeat(cohortSize) { cohortInstances.add(inProgressStrokePool.obtain(shapeSpec)) }
            assertThat(cohortInstances).hasSize(cohortSize)
            val recyclingData =
                assertNotNull(inProgressStrokePool.recyclingDataForShapeType(shapeType))
            assertThat(recyclingData.currentlyObtainedShapeCount).isEqualTo(cohortSize)

            // Recycle all the instances, which at the end can potentially trim down the pool size.
            for (cohortInstance in cohortInstances) {
                inProgressStrokePool.recycle(cohortInstance)
            }
            assertThat(recyclingData.poolSize).isEqualTo(maxOfLast10CohortSizes[handoffIndex])
        }
    }
}
