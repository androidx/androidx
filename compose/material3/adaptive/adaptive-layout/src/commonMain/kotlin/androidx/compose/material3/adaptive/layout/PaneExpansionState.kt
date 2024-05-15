/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope

@ExperimentalMaterial3AdaptiveApi
@Stable
internal class PaneExpansionState(
    internal val anchors: List<PaneExpansionAnchor> = emptyList()
) : DraggableState {
    private var firstPaneWidthState by mutableIntStateOf(UnspecifiedWidth)
    private var firstPanePercentageState by mutableFloatStateOf(Float.NaN)
    private var currentDraggingOffsetState by mutableIntStateOf(UnspecifiedWidth)

    var firstPaneWidth: Int
        set(value) {
            firstPanePercentageState = Float.NaN
            currentDraggingOffsetState = UnspecifiedWidth
            firstPaneWidthState = value.coerceIn(0, maxExpansionWidth)
        }
        get() = firstPaneWidthState

    var firstPanePercentage: Float
        set(value) {
            require(value in 0f..1f) { "Percentage value needs to be in [0, 1]" }
            firstPaneWidthState = UnspecifiedWidth
            currentDraggingOffsetState = UnspecifiedWidth
            firstPanePercentageState = value
        }
        get() = firstPanePercentageState

    internal var currentDraggingOffset
        get() = currentDraggingOffsetState
        private set(value) {
            val coercedValue = value.coerceIn(0, maxExpansionWidth)
            if (value == currentDraggingOffsetState) {
                return
            }
            currentDraggingOffsetState = coercedValue
            currentMeasuredDraggingOffset = coercedValue
        }

    internal var isDragging by mutableStateOf(false)
        private set

    internal var isSettling by mutableStateOf(false)
        private set

    internal val isDraggingOrSettling get() = isDragging || isSettling

    @VisibleForTesting
    internal var maxExpansionWidth = 0
        private set

    // Use this field to store the dragging offset decided by measuring instead of dragging to
    // prevent redundant re-composition.
    @VisibleForTesting
    internal var currentMeasuredDraggingOffset = UnspecifiedWidth
        private set

    private var anchorPositions: IntList = emptyIntList()

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
    }

    private val dragMutex = MutatorMutex()

    fun isUnspecified(): Boolean =
        firstPaneWidthState == UnspecifiedWidth &&
            firstPanePercentage.isNaN() &&
            currentDraggingOffset == UnspecifiedWidth

    override fun dispatchRawDelta(delta: Float) {
        if (currentMeasuredDraggingOffset == UnspecifiedWidth) {
            return
        }
        currentDraggingOffset = (currentMeasuredDraggingOffset + delta).toInt()
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ) = coroutineScope {
        isDragging = true
        dragMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    internal fun onMeasured(measuredWidth: Int, density: Density) {
        if (measuredWidth == maxExpansionWidth) {
            return
        }
        maxExpansionWidth = measuredWidth
        if (firstPaneWidth != UnspecifiedWidth) {
            firstPaneWidth = firstPaneWidth
        }
        anchorPositions = anchors.toPositions(measuredWidth, density)
    }

    internal fun onExpansionOffsetMeasured(measuredOffset: Int) {
        currentMeasuredDraggingOffset = measuredOffset
    }

    internal suspend fun settleToAnchorIfNeeded(velocity: Float) {
        val currentAnchorPositions = anchorPositions
        if (currentAnchorPositions.isEmpty()) {
            return
        }
        dragMutex.mutate(MutatePriority.PreventUserInput) {
            isSettling = true
            // TODO(conradchen): Use the right animation spec here.
            animate(
                currentMeasuredDraggingOffset.toFloat(),
                currentAnchorPositions.getPositionOfTheClosestAnchor(
                    currentMeasuredDraggingOffset,
                    velocity
                ).toFloat(),
                velocity,
            ) { value, _ ->
                currentDraggingOffset = value.toInt()
            }
            isSettling = false
        }
    }

    private fun IntList.getPositionOfTheClosestAnchor(
        currentPosition: Int,
        velocity: Float
    ): Int = minBy(
        when {
            velocity >= AnchoringVelocityThreshold -> {
                { anchorPosition: Int ->
                    val delta = anchorPosition - currentPosition
                    if (delta < 0) Int.MAX_VALUE else delta
                }
            }
            velocity <= -AnchoringVelocityThreshold -> {
                { anchorPosition: Int ->
                    val delta = currentPosition - anchorPosition
                    if (delta < 0) Int.MAX_VALUE else delta
                }
            }
            else -> {
                { anchorPosition: Int ->
                    abs(currentPosition - anchorPosition)
                }
            }
        }
    )

    companion object {
        const val UnspecifiedWidth = -1
        private const val AnchoringVelocityThreshold = 200F
    }
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class PaneExpansionAnchor private constructor(
    val percentage: Int,
    val startOffset: Dp // TODO(conradchen): confirm RTL support
) {
    constructor(@IntRange(0, 100) percentage: Int) : this(percentage, Dp.Unspecified)

    constructor(startOffset: Dp) : this(Int.MIN_VALUE, startOffset)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun List<PaneExpansionAnchor>.toPositions(
    maxExpansionWidth: Int,
    density: Density
): IntList {
    val anchors = MutableIntList(size)
    @Suppress("ListIterator") // Not necessarily a random-accessible list
    forEach { anchor ->
        if (anchor.startOffset.isSpecified) {
            val position = with(density) { anchor.startOffset.toPx() }.toInt().let {
                if (it < 0) maxExpansionWidth + it else it
            }
            if (position in 0..maxExpansionWidth) {
                anchors.add(position)
            }
        } else {
            anchors.add(maxExpansionWidth * anchor.percentage / 100)
        }
    }
    anchors.sort()
    return anchors
}

private fun <T : Comparable<T>> IntList.minBy(selector: (Int) -> T): Int {
    if (isEmpty()) {
        throw NoSuchElementException()
    }
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1 until size) {
        val elem = this[i]
        val value = selector(elem)
        if (minValue > value) {
            minElem = elem
            minValue = value
        }
    }
    return minElem
}
