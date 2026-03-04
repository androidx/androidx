/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.ink.authoring.CompletedShapeRenderer
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.InProgressShape
import androidx.ink.authoring.InProgressShapeRenderer
import androidx.ink.authoring.ShapeWorkflow
import androidx.ink.geometry.Box
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInputBatch

/** A test-only implementation that implements the [ShapeWorkflow] interface in a trivial way. */
@ExperimentalCustomShapeWorkflowApi
internal class FakeShapeWorkflow :
    ShapeWorkflow<FakeShapeSpec, FakeInProgressShape, ImmutableStrokeInputBatch> {
    override fun getShapeType(shapeSpec: FakeShapeSpec): Int {
        // Pretend there is some recycling benefit to treating these different parameter categories
        // as
        // different types of shapes.
        var shapeType = 0
        shapeType += if (shapeSpec.updatesAfterCompletion) 1 else 0
        shapeType += if (shapeSpec.completionAfterFinishDurationMillis > 0) 2 else 0
        return shapeType
    }

    override fun create(shapeType: Int): FakeInProgressShape = FakeInProgressShape()

    override val inProgressShapeRenderer =
        object : InProgressShapeRenderer<FakeInProgressShape> {
            override fun draw(
                canvas: Canvas,
                shape: FakeInProgressShape,
                strokeToScreenTransform: Matrix,
            ) {}
        }

    override val completedShapeRenderer =
        object : CompletedShapeRenderer<ImmutableStrokeInputBatch> {
            override fun draw(
                canvas: Canvas,
                shape: ImmutableStrokeInputBatch,
                strokeToScreenTransform: Matrix,
                systemElapsedTimeMillis: Long,
            ) {}
        }
}

internal data class FakeShapeSpec(
    val completionAfterFinishDurationMillis: Long = 0,
    val updatesAfterCompletion: Boolean = false,
)

/** A test-only implementation that implements the [InProgressShape] interface in a trivial way. */
@ExperimentalCustomShapeWorkflowApi
internal class FakeInProgressShape : InProgressShape<FakeShapeSpec, ImmutableStrokeInputBatch> {
    private var shapeSpec: FakeShapeSpec? = null
    private var startSystemElapsedTimeMillis = Long.MIN_VALUE

    private val updatedRealInputs = MutableStrokeInputBatch()
    private val pendingRealInputs = MutableStrokeInputBatch()
    internal var lastUpdateSystemElapsedTimeMillis: Long? = null
    private var forcedCompletion = false
    private var finishedTimeMillis: Long? = null
    private var updateSinceLastReset = false
    private var canceled = false

    override fun isCanceled(): Boolean = canceled

    override fun start(shapeSpec: FakeShapeSpec, systemElapsedTimeMillis: Long) {
        this.shapeSpec = shapeSpec
        startSystemElapsedTimeMillis = systemElapsedTimeMillis

        updatedRealInputs.clear()
        pendingRealInputs.clear()
        lastUpdateSystemElapsedTimeMillis = null
        forcedCompletion = false
        finishedTimeMillis = null
        updateSinceLastReset = false
        canceled = false
    }

    override fun enqueueInputs(realInputs: StrokeInputBatch, predictedInputs: StrokeInputBatch) {
        pendingRealInputs.add(realInputs)
    }

    override fun changesWithTime(): Boolean =
        checkNotNull(shapeSpec).let {
            it.updatesAfterCompletion || it.completionAfterFinishDurationMillis > 0
        }

    override fun update(shapeDurationMillis: Long) {
        updatedRealInputs.add(pendingRealInputs)
        pendingRealInputs.clear()
        lastUpdateSystemElapsedTimeMillis = startSystemElapsedTimeMillis + shapeDurationMillis
        updateSinceLastReset = true
    }

    override fun forceCompletion() {
        forcedCompletion = true
    }

    override fun cancel() {
        canceled = true
    }

    override fun getUpdatedRegion(): Box? = if (updateSinceLastReset) FAKE_UPDATE_BOX else null

    override fun resetUpdatedRegion() {
        updateSinceLastReset = false
    }

    override fun finishInput() {
        finishedTimeMillis = lastUpdateSystemElapsedTimeMillis ?: 0
    }

    override fun getCompletedShape(): ImmutableStrokeInputBatch? {
        val spec = checkNotNull(shapeSpec)
        val finishedTime = finishedTimeMillis ?: return null
        val lastUpdateTime = lastUpdateSystemElapsedTimeMillis ?: return null
        return if (
            forcedCompletion ||
                lastUpdateTime >= (finishedTime + spec.completionAfterFinishDurationMillis)
        ) {
            updatedRealInputs.toImmutable()
        } else {
            null
        }
    }

    private companion object {
        val FAKE_UPDATE_BOX =
            ImmutableBox.fromCenterAndDimensions(
                center = ImmutableVec(1F, 2F),
                width = 3F,
                height = 4F,
            )
    }
}
