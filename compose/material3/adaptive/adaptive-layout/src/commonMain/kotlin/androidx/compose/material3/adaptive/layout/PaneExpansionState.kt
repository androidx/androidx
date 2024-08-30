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

import androidx.annotation.FloatRange
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableLongList
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneExpansionState.Companion.Unspecified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.math.abs
import kotlin.math.roundToInt
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
fun rememberPaneExpansionState(
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
fun rememberPaneExpansionState(
    key: PaneExpansionStateKey = PaneExpansionStateKey.Default,
    anchors: List<PaneExpansionAnchor> = emptyList()
): PaneExpansionState {
    val dataMap = rememberSaveable(saver = PaneExpansionStateSaver()) { mutableStateMapOf() }
    val expansionState = remember {
        val defaultData = PaneExpansionStateData()
        dataMap[PaneExpansionStateKey.Default] = defaultData
        PaneExpansionState(defaultData)
    }
    return expansionState.apply {
        restore(dataMap[key] ?: PaneExpansionStateData().also { dataMap[key] = it }, anchors)
    }
}

/**
 * This class manages the pane expansion state for pane scaffolds. By providing and modifying an
 * instance of this class, you can specify the expanded panes' expansion width or proportion when
 * pane scaffold is displaying a dual-pane layout.
 *
 * This class also serves as the [DraggableState] of pane expansion handle. When a handle
 * implementation is provided to the associated pane scaffold, the scaffold will use
 * [PaneExpansionState] to store and manage dragging and anchoring of the handle, and thus the pane
 * expansion state.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
class PaneExpansionState
internal constructor(
    // TODO(conradchen): Handle state change during dragging and settling
    data: PaneExpansionStateData = PaneExpansionStateData(),
    anchors: List<PaneExpansionAnchor> = emptyList()
) : DraggableState {

    internal val firstPaneWidth
        get() =
            if (maxExpansionWidth == Unspecified || data.firstPaneWidthState == Unspecified) {
                Unspecified
            } else {
                data.firstPaneWidthState.coerceIn(0, maxExpansionWidth)
            }

    internal val firstPaneProportion: Float
        get() = data.firstPaneProportionState

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

    private var data by mutableStateOf(data)

    internal var isDragging by mutableStateOf(false)
        private set

    internal var isSettling by mutableStateOf(false)
        private set

    internal val isDraggingOrSettling
        get() = isDragging || isSettling

    @VisibleForTesting
    internal var maxExpansionWidth by mutableIntStateOf(Unspecified)
        private set

    // Use this field to store the dragging offset decided by measuring instead of dragging to
    // prevent redundant re-composition.
    @VisibleForTesting
    internal var currentMeasuredDraggingOffset = Unspecified
        private set

    private var anchors: List<PaneExpansionAnchor> by mutableStateOf(anchors)

    private lateinit var measuredDensity: Density

    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    private val dragMutex = MutatorMutex()

    /** Returns `true` if none of [firstPaneWidth] or [firstPaneProportion] has been set. */
    fun isUnspecified(): Boolean =
        firstPaneWidth == Unspecified &&
            firstPaneProportion.isNaN() &&
            currentDraggingOffset == Unspecified

    override fun dispatchRawDelta(delta: Float) {
        if (currentMeasuredDraggingOffset == Unspecified) {
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

    /**
     * Set the width of the first expanded pane in the layout. When the set value gets applied, it
     * will be coerced within the range of `[0, the full displayable width of the layout]`.
     *
     * Note that setting this value will reset the first pane proportion previously set via
     * [setFirstPaneProportion] or the current dragging result if there's any. Also if user drags
     * the pane after setting the first pane width, the user dragging result will take the priority
     * over this set value when rendering panes, but the set value will be saved.
     */
    fun setFirstPaneWidth(firstPaneWidth: Int) {
        data.firstPaneProportionState = Float.NaN
        data.currentDraggingOffsetState = Unspecified
        data.firstPaneWidthState = firstPaneWidth
    }

    /**
     * Set the proportion of the first expanded pane in the layout. The set value needs to be within
     * the range of `[0f, 1f]`, otherwise the setter throws.
     *
     * Note that setting this value will reset the first pane width previously set via
     * [setFirstPaneWidth] or the current dragging result if there's any. Also if user drags the
     * pane after setting the first pane proportion, the user dragging result will take the priority
     * over this set value when rendering panes, but the set value will be saved.
     */
    fun setFirstPaneProportion(@FloatRange(0.0, 1.0) firstPaneProportion: Float) {
        require(firstPaneProportion in 0f..1f) { "Proportion value needs to be in [0f, 1f]" }
        data.firstPaneWidthState = Unspecified
        data.currentDraggingOffsetState = Unspecified
        data.firstPaneProportionState = firstPaneProportion
    }

    /**
     * Clears any previously set [firstPaneWidth] or [firstPaneProportion], as well as the user
     * dragging result.
     */
    fun clear() {
        data.firstPaneWidthState = Unspecified
        data.firstPaneProportionState = Float.NaN
        data.currentDraggingOffsetState = Unspecified
    }

    internal fun restore(data: PaneExpansionStateData, anchors: List<PaneExpansionAnchor>) {
        this.data = data
        this.anchors = anchors
        if (!anchors.contains(Snapshot.withoutReadObservation { data.currentAnchorState })) {
            data.currentAnchorState = null
        }
    }

    internal fun onMeasured(measuredWidth: Int, density: Density) {
        if (measuredWidth == maxExpansionWidth) {
            return
        }
        maxExpansionWidth = measuredWidth
        measuredDensity = density
        Snapshot.withoutReadObservation {
            // Changes will always apply to the ongoing measurement, no need to trigger remeasuring
            data.currentAnchorState?.also {
                currentDraggingOffset = it.positionIn(measuredWidth, density)
            }
                ?: {
                    if (currentDraggingOffset != Unspecified) {
                        // To re-coerce the value
                        currentDraggingOffset = currentDraggingOffset
                    }
                }
        }
    }

    internal fun onExpansionOffsetMeasured(measuredOffset: Int) {
        currentMeasuredDraggingOffset = measuredOffset
    }

    internal suspend fun settleToAnchorIfNeeded(velocity: Float) {
        val currentAnchorPositions = anchors.toPositions(maxExpansionWidth, measuredDensity)
        if (currentAnchorPositions.isEmpty()) {
            return
        }

        dragMutex.mutate(MutatePriority.PreventUserInput) {
            isSettling = true
            val anchorPosition =
                currentAnchorPositions.getPositionOfTheClosestAnchor(
                    currentMeasuredDraggingOffset,
                    velocity
                )
            data.currentAnchorState = anchors[anchorPosition.index]
            animate(
                currentMeasuredDraggingOffset.toFloat(),
                anchorPosition.position.toFloat(),
                velocity,
                PaneSnapAnimationSpec,
            ) { value, _ ->
                currentDraggingOffset = value.toInt()
            }
            isSettling = false
        }
    }

    private fun IndexedAnchorPositionList.getPositionOfTheClosestAnchor(
        currentPosition: Int,
        velocity: Float
    ): IndexedAnchorPosition =
        // TODO(conradchen): Add fling support
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
        /** The constant value used to denote the pane expansion is not specified. */
        const val Unspecified = -1

        private const val AnchoringVelocityThreshold = 200F

        private val PaneSnapAnimationSpec =
            spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = 1f)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Stable
internal class PaneExpansionStateData(
    firstPaneWidth: Int = Unspecified,
    firstPaneProportion: Float = Float.NaN,
    currentDraggingOffset: Int = Unspecified,
    currentAnchor: PaneExpansionAnchor? = null
) {
    var firstPaneWidthState by mutableIntStateOf(firstPaneWidth)
    var firstPaneProportionState by mutableFloatStateOf(firstPaneProportion)
    var currentDraggingOffsetState by mutableIntStateOf(currentDraggingOffset)
    var currentAnchorState by mutableStateOf(currentAnchor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneExpansionStateData) return false
        if (firstPaneWidthState != other.firstPaneWidthState) return false
        if (firstPaneProportionState != other.firstPaneProportionState) return false
        if (currentDraggingOffsetState != other.currentDraggingOffsetState) return false
        if (currentAnchorState != other.currentAnchorState) return false
        return true
    }

    override fun hashCode(): Int {
        var result = firstPaneWidthState
        result = 31 * result + firstPaneProportionState.hashCode()
        result = 31 * result + currentDraggingOffsetState
        result = 31 * result + currentAnchorState.hashCode()
        return result
    }
}

/**
 * The implementations of this interface represent different types of anchors of pane expansion
 * dragging. Setting up anchors when create [PaneExpansionState] will force user dragging to snap to
 * the set anchors after user releases the drag.
 */
@ExperimentalMaterial3AdaptiveApi
sealed class PaneExpansionAnchor private constructor() {
    internal abstract fun positionIn(totalSizePx: Int, density: Density): Int

    internal abstract val type: Int

    /**
     * [PaneExpansionAnchor] implementation that specifies the anchor position in the proportion of
     * the total size of the layout at the start side of the anchor.
     *
     * @property proportion the proportion of the layout at the start side of the anchor. layout.
     */
    class Proportion(@FloatRange(0.0, 1.0) val proportion: Float) : PaneExpansionAnchor() {
        override val type = ProportionType

        override fun positionIn(totalSizePx: Int, density: Density) =
            (totalSizePx * proportion).roundToInt().coerceIn(0, totalSizePx)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Proportion) return false
            return proportion == other.proportion
        }

        override fun hashCode(): Int {
            return proportion.hashCode()
        }
    }

    /**
     * [PaneExpansionAnchor] implementation that specifies the anchor position in the offset in
     * [Dp]. If a positive value is provided, the offset will be treated as a start offset, on the
     * other hand, if a negative value is provided, the absolute value of the provided offset will
     * be used as an end offset. For example, if -150.dp is provided, the resulted anchor will be at
     * the position that is 150dp away from the end side of the associated layout.
     *
     * @property offset the offset of the anchor in [Dp].
     */
    class Offset(val offset: Dp) : PaneExpansionAnchor() {
        override val type = OffsetType

        override fun positionIn(totalSizePx: Int, density: Density) =
            with(density) { offset.toPx() }.toInt().let { if (it < 0) totalSizePx + it else it }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Offset) return false
            return offset == other.offset
        }

        override fun hashCode(): Int {
            return offset.hashCode()
        }
    }

    internal companion object {
        internal const val UnspecifiedType = 0
        internal const val ProportionType = 1
        internal const val OffsetType = 2
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal fun PaneExpansionStateSaver():
    Saver<MutableMap<PaneExpansionStateKey, PaneExpansionStateData>, *> =
    listSaver<MutableMap<PaneExpansionStateKey, PaneExpansionStateData>, Any>(
        save = {
            val dataSaver = PaneExpansionStateDataSaver()
            buildList { it.forEach { entry -> add(with(dataSaver) { save(entry) }!!) } }
        },
        restore = {
            val dataSaver = PaneExpansionStateDataSaver()
            val map = mutableMapOf<PaneExpansionStateKey, PaneExpansionStateData>()
            it.fastForEach { with(dataSaver) { restore(it) }!!.apply { map[key] = value } }
            map
        }
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun PaneExpansionStateDataSaver():
    Saver<Map.Entry<PaneExpansionStateKey, PaneExpansionStateData>, Any> =
    listSaver(
        save = {
            val keyType = it.key.type
            val currentAnchorType =
                it.value.currentAnchorState?.type ?: PaneExpansionAnchor.UnspecifiedType
            listOf(
                keyType,
                if (keyType == DefaultPaneExpansionStateKey) {
                    null
                } else {
                    with(TwoPaneExpansionStateKeyImpl.saver()) {
                        save(it.key as TwoPaneExpansionStateKeyImpl)
                    }
                },
                it.value.firstPaneWidthState,
                it.value.firstPaneProportionState,
                it.value.currentDraggingOffsetState,
                currentAnchorType,
                with(it.value.currentAnchorState) {
                    when (this) {
                        is PaneExpansionAnchor.Proportion -> this.proportion
                        is PaneExpansionAnchor.Offset -> this.offset.value
                        else -> null
                    }
                }
            )
        },
        restore = {
            val keyType = it[0] as Int
            val key =
                if (keyType == DefaultPaneExpansionStateKey || it[1] == null) {
                    PaneExpansionStateKey.Default
                } else {
                    with(TwoPaneExpansionStateKeyImpl.saver()) { restore(it[1]!!) }
                }
            val currentAnchorType = it[5] as Int
            val currentAnchor =
                when (currentAnchorType) {
                    PaneExpansionAnchor.ProportionType ->
                        PaneExpansionAnchor.Proportion(it[6] as Float)
                    PaneExpansionAnchor.OffsetType ->
                        PaneExpansionAnchor.Offset((it[6] as Float).dp)
                    else -> null
                }
            object : Map.Entry<PaneExpansionStateKey, PaneExpansionStateData> {
                override val key: PaneExpansionStateKey = key!!
                override val value: PaneExpansionStateData =
                    PaneExpansionStateData(
                        firstPaneWidth = it[2] as Int,
                        firstPaneProportion = it[3] as Float,
                        currentDraggingOffset = it[4] as Int,
                        currentAnchor = currentAnchor
                    )
            }
        }
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PaneExpansionStateKey.type
    get() =
        if (this is TwoPaneExpansionStateKeyImpl) {
            TwoPaneExpansionStateKey
        } else {
            DefaultPaneExpansionStateKey
        }

private const val DefaultPaneExpansionStateKey = 0
private const val TwoPaneExpansionStateKey = 1

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun List<PaneExpansionAnchor>.toPositions(
    maxExpansionWidth: Int,
    density: Density
): IndexedAnchorPositionList {
    val anchors = IndexedAnchorPositionList(size)
    @Suppress("ListIterator") // Not necessarily a random-accessible list
    forEachIndexed { index, anchor ->
        anchors.add(IndexedAnchorPosition(anchor.positionIn(maxExpansionWidth, density), index))
    }
    anchors.sort()
    return anchors
}

private fun <T : Comparable<T>> IndexedAnchorPositionList.minBy(
    selector: (Int) -> T
): IndexedAnchorPosition {
    if (isEmpty()) {
        throw NoSuchElementException()
    }
    var minElem = this[0]
    var minValue = selector(minElem.position)
    for (i in 1 until size) {
        val elem = this[i]
        val value = selector(elem.position)
        if (minValue > value) {
            minElem = elem
            minValue = value
        }
    }
    return minElem
}

@JvmInline
private value class IndexedAnchorPositionList(val value: MutableLongList) {
    constructor(size: Int) : this(MutableLongList(size))

    val size
        get() = value.size

    fun isEmpty() = value.isEmpty()

    fun add(position: IndexedAnchorPosition) = value.add(position.value)

    fun sort() = value.sort()

    operator fun get(index: Int) = IndexedAnchorPosition(value[index])
}

@JvmInline
private value class IndexedAnchorPosition(val value: Long) {
    constructor(position: Int, index: Int) : this(packInts(position, index))

    val position
        get() = unpackInt1(value)

    val index
        get() = unpackInt2(value)
}
