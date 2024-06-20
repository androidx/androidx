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
import androidx.compose.material3.adaptive.layout.PaneExpansionState.Companion.UnspecifiedWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope

/**
 * Interface that provides [PaneExpansionStateKey] to remember and retrieve [PaneExpansionState]
 * with [rememberPaneExpansionState].
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed interface PaneExpansionStateKeyProvider {
    /** The key that represents the unique state of the provider to index [PaneExpansionState]. */
    val paneExpansionStateKey: PaneExpansionStateKey
}

/**
 * Interface that serves as keys to remember and retrieve [PaneExpansionState] with
 * [rememberPaneExpansionState].
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
sealed interface PaneExpansionStateKey {
    private class DefaultImpl : PaneExpansionStateKey {
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    companion object {
        /**
         * The default [PaneExpansionStateKey]. If you want to always share the same
         * [PaneExpansionState] no matter what current scaffold state is, this key can be used. For
         * example if the default key is used and a user drag the list-detail layout to a 50-50
         * split, when the layout switches to, say, detail-extra, it will remain the 50-50 split
         * instead of using a different (default or user-set) split for it.
         */
        val Default: PaneExpansionStateKey = DefaultImpl()
    }
}

/**
 * Remembers and returns a [PaneExpansionState] associated to a given
 * [PaneExpansionStateKeyProvider].
 *
 * Note that the remembered [PaneExpansionState] with all keys that have been used will be
 * persistent through the associated pane scaffold's lifecycles.
 *
 * @param keyProvider the provider of [PaneExpansionStateKey]
 * @param anchors the anchor list of the returned [PaneExpansionState]
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun rememberPaneExpansionState(
    keyProvider: PaneExpansionStateKeyProvider,
    anchors: List<PaneExpansionAnchor> = emptyList()
): PaneExpansionState = rememberPaneExpansionState(keyProvider.paneExpansionStateKey, anchors)

/**
 * Remembers and returns a [PaneExpansionState] associated to a given [PaneExpansionStateKey].
 *
 * Note that the remembered [PaneExpansionState] with all keys that have been used will be
 * persistent through the associated pane scaffold's lifecycles.
 *
 * @param key the key of [PaneExpansionStateKey]
 * @param anchors the anchor list of the returned [PaneExpansionState]
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun rememberPaneExpansionState(
    key: PaneExpansionStateKey = PaneExpansionStateKey.Default,
    anchors: List<PaneExpansionAnchor> = emptyList()
): PaneExpansionState {
    // TODO(conradchen): Implement this as saveables
    val dataMap = remember { mutableStateMapOf<PaneExpansionStateKey, PaneExpansionStateData>() }
    val expansionState = remember {
        val defaultData = PaneExpansionStateData()
        dataMap[PaneExpansionStateKey.Default] = defaultData
        PaneExpansionState(defaultData)
    }
    return expansionState.apply {
        this.data = dataMap[key] ?: PaneExpansionStateData().also { dataMap[key] = it }
        this.anchors = anchors
    }
}

@ExperimentalMaterial3AdaptiveApi
@Stable
internal class PaneExpansionState
internal constructor(
    // TODO(conradchen): Handle state change during dragging and settling
    data: PaneExpansionStateData = PaneExpansionStateData(),
    internal var anchors: List<PaneExpansionAnchor> = emptyList()
) : DraggableState {

    var firstPaneWidth: Int
        set(value) {
            data.firstPanePercentageState = Float.NaN
            data.currentDraggingOffsetState = UnspecifiedWidth
            val coercedValue = value.coerceIn(0, maxExpansionWidth)
            data.firstPaneWidthState = coercedValue
        }
        get() = data.firstPaneWidthState

    var firstPanePercentage: Float
        set(value) {
            require(value in 0f..1f) { "Percentage value needs to be in [0, 1]" }
            data.firstPaneWidthState = UnspecifiedWidth
            data.currentDraggingOffsetState = UnspecifiedWidth
            data.firstPanePercentageState = value
        }
        get() = data.firstPanePercentageState

    internal var currentDraggingOffset
        get() = data.currentDraggingOffsetState
        private set(value) {
            val coercedValue = value.coerceIn(0, maxExpansionWidth)
            if (coercedValue == data.currentDraggingOffsetState) {
                return
            }
            data.currentDraggingOffsetState = coercedValue
            currentMeasuredDraggingOffset = coercedValue
        }

    internal var data by mutableStateOf(data)

    internal var isDragging by mutableStateOf(false)
        private set

    internal var isSettling by mutableStateOf(false)
        private set

    internal val isDraggingOrSettling
        get() = isDragging || isSettling

    @VisibleForTesting
    internal var maxExpansionWidth = 0
        private set

    // Use this field to store the dragging offset decided by measuring instead of dragging to
    // prevent redundant re-composition.
    @VisibleForTesting
    internal var currentMeasuredDraggingOffset = UnspecifiedWidth
        private set

    private var anchorPositions: IntList = emptyIntList()

    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    private val dragMutex = MutatorMutex()

    fun isUnspecified(): Boolean =
        firstPaneWidth == UnspecifiedWidth &&
            firstPanePercentage.isNaN() &&
            currentDraggingOffset == UnspecifiedWidth

    override fun dispatchRawDelta(delta: Float) {
        if (currentMeasuredDraggingOffset == UnspecifiedWidth) {
            return
        }
        currentDraggingOffset = (currentMeasuredDraggingOffset + delta).toInt()
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) =
        coroutineScope {
            isDragging = true
            dragMutex.mutateWith(dragScope, dragPriority, block)
            isDragging = false
        }

    /** Clears any existing expansion state. */
    fun clear() {
        data.firstPaneWidthState = UnspecifiedWidth
        data.firstPanePercentageState = Float.NaN
        data.currentDraggingOffsetState = UnspecifiedWidth
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

        // TODO(conradchen): Figure out how to use lookahead here to avoid repeating measuring
        dragMutex.mutate(MutatePriority.PreventUserInput) {
            isSettling = true
            // TODO(conradchen): Use the right animation spec here.
            animate(
                currentMeasuredDraggingOffset.toFloat(),
                currentAnchorPositions
                    .getPositionOfTheClosestAnchor(currentMeasuredDraggingOffset, velocity)
                    .toFloat(),
                velocity,
            ) { value, _ ->
                currentDraggingOffset = value.toInt()
            }
            isSettling = false
        }
    }

    private fun IntList.getPositionOfTheClosestAnchor(currentPosition: Int, velocity: Float): Int =
        minBy(
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
                    { anchorPosition: Int -> abs(currentPosition - anchorPosition) }
                }
            }
        )

    companion object {
        const val UnspecifiedWidth = -1
        private const val AnchoringVelocityThreshold = 200F
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class PaneExpansionStateData {
    var firstPaneWidthState by mutableIntStateOf(UnspecifiedWidth)
    var firstPanePercentageState by mutableFloatStateOf(Float.NaN)
    var currentDraggingOffsetState by mutableIntStateOf(UnspecifiedWidth)
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class PaneExpansionAnchor
private constructor(
    val percentage: Int,
    val startOffset: Dp // TODO(conradchen): confirm RTL support
) {
    constructor(@IntRange(0, 100) percentage: Int) : this(percentage, Dp.Unspecified)

    constructor(startOffset: Dp) : this(Int.MIN_VALUE, startOffset)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneExpansionAnchor) return false
        if (percentage != other.percentage) return false
        if (startOffset != other.startOffset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = percentage
        result = 31 * result + startOffset.hashCode()
        return result
    }
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
            val position =
                with(density) { anchor.startOffset.toPx() }
                    .toInt()
                    .let { if (it < 0) maxExpansionWidth + it else it }
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
