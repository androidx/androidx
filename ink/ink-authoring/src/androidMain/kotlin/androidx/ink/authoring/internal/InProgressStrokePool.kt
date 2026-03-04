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

import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectIntMap
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.InProgressShape
import androidx.ink.authoring.ShapeWorkflow

@OptIn(ExperimentalCustomShapeWorkflowApi::class)
internal interface InProgressStrokePool<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, *>,
> {
    fun obtain(shapeSpec: ShapeSpecT): InProgressShapeT

    fun recycle(inProgressShape: InProgressShapeT)
}

@OptIn(ExperimentalCustomShapeWorkflowApi::class)
internal class InProgressStrokePoolImpl<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, *>,
>(private val shapeWorkflow: ShapeWorkflow<ShapeSpecT, InProgressShapeT, *>) :
    InProgressStrokePool<ShapeSpecT, InProgressShapeT> {

    private val shapeTypeToRecyclingData =
        MutableIntObjectMap<ShapeTypeRecyclingData<InProgressShapeT>>()
    private val loanedShapesToShapeType = MutableObjectIntMap<InProgressShapeT>()

    @VisibleForTesting
    internal fun recyclingDataForShapeType(shapeType: Int) = shapeTypeToRecyclingData[shapeType]

    override fun obtain(shapeSpec: ShapeSpecT): InProgressShapeT {
        val shapeType = shapeWorkflow.getShapeType(shapeSpec)
        val recyclingData =
            shapeTypeToRecyclingData.getOrPut(shapeType) { ShapeTypeRecyclingData() }
        val ips = recyclingData.obtainOrCreate { shapeWorkflow.create(shapeType) }
        loanedShapesToShapeType[ips] = shapeType
        return ips
    }

    override fun recycle(inProgressShape: InProgressShapeT) {
        check(loanedShapesToShapeType.containsKey(inProgressShape)) {
            "Trying to recycle shape that was not obtained: $inProgressShape"
        }
        val shapeType = loanedShapesToShapeType[inProgressShape]
        val recyclingData =
            checkNotNull(shapeTypeToRecyclingData[shapeType]) {
                "Trying to recycle shape with unrecognized shape type: $shapeType"
            }
        inProgressShape.prepareToRecycle()
        recyclingData.recycle(inProgressShape)
    }

    @VisibleForTesting
    internal class ShapeTypeRecyclingData<InProgressShapeT> {
        /**
         * This will grow as needed to match the size of the biggest stroke cohort seen in the last
         * N handoffs. A hard limit on the pool size wouldn't be appropriate as each app and each
         * user will have different patterns, and different [InProgressStrokesRenderHelper]
         * implementations can influence the number of instances needed at once. But trimming the
         * size of this pool according to recent activity (see [recentCohortSizes]) ensures that an
         * unusually large cohort won't force too much memory to be held for the rest of the inking
         * session.
         */
        private val pool = mutableListOf<InProgressShapeT>()

        internal val poolSize
            get() = pool.size

        /** Number of calls to [obtainOrCreate] minus number of calls to [recycle]. */
        internal var currentlyObtainedShapeCount = 0
            private set

        /**
         * The high water mark of [currentlyObtainedShapeCount], resets when
         * [currentlyObtainedShapeCount] reaches 0.
         */
        private var currentCohortSize = 0

        /**
         * The N most recent values for how many [InProgressShapeT] instances have been needed at
         * once. Start with all zeroes because so far none have been needed.
         */
        private val recentCohortSizes = IntArray(10)

        /** The index in the [recentCohortSizes] circular buffer to update next. */
        private var recentCohortSizesNextIndex = 0

        inline fun obtainOrCreate(createBlock: () -> InProgressShapeT): InProgressShapeT =
            (pool.removeFirstOrNull() ?: createBlock()).also {
                currentlyObtainedShapeCount++
                currentCohortSize++
            }

        fun recycle(inProgressShape: InProgressShapeT) {
            pool.add(inProgressShape)
            currentlyObtainedShapeCount--

            if (currentlyObtainedShapeCount == 0) {
                recentCohortSizes[recentCohortSizesNextIndex] = currentCohortSize
                currentCohortSize = 0
                recentCohortSizesNextIndex++
                if (recentCohortSizesNextIndex >= recentCohortSizes.size) {
                    recentCohortSizesNextIndex = 0
                }
                val newMaxPoolSize = recentCohortSizes.max()
                if (pool.size <= newMaxPoolSize) return
                pool.subList(newMaxPoolSize, pool.size).clear()
                check(pool.size == newMaxPoolSize)
            }
        }
    }
}
