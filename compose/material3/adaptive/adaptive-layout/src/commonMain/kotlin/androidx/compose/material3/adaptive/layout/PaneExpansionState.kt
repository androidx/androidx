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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneExpansionState.Companion.DefaultAnchoringAnimationSpec
import androidx.compose.material3.adaptive.layout.PaneExpansionState.Companion.Unspecified
import androidx.compose.material3.adaptive.layout.internal.Strings
import androidx.compose.material3.adaptive.layout.internal.getString
import androidx.compose.material3.adaptive.layout.internal.identityHashCode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
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
            return identityHashCode(this)
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
 * @param initialAnchoredIndex the index of the anchor that is supposed to be used during the
 *   initial layout of the associated scaffold; it has to be a valid index of the provided [anchors]
 *   otherwise the function throws; by default the value will be -1 and no initial anchor will be
 *   used.
 * @param anchoringAnimationSpec the animation spec used to perform anchoring animation; by default
 *   it will be a spring motion.
 * @param flingBehavior the fling behavior used to handle flings; by default
 *   [ScrollableDefaults.flingBehavior] will be applied.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberPaneExpansionState(
    keyProvider: PaneExpansionStateKeyProvider,
    anchors: List<PaneExpansionAnchor> = emptyList(),
    initialAnchoredIndex: Int = -1,
    anchoringAnimationSpec: FiniteAnimationSpec<Float> = DefaultAnchoringAnimationSpec,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
): PaneExpansionState =
    rememberPaneExpansionState(
        keyProvider.paneExpansionStateKey,
        anchors,
        initialAnchoredIndex,
        anchoringAnimationSpec,
        flingBehavior,
    )

/**
 * Remembers and returns a [PaneExpansionState] associated to a given [PaneExpansionStateKey].
 *
 * Note that the remembered [PaneExpansionState] with all keys that have been used will be
 * persistent through the associated pane scaffold's lifecycles.
 *
 * @param key the key of [PaneExpansionStateKey]
 * @param anchors the anchor list of the returned [PaneExpansionState]
 * @param initialAnchoredIndex the index of the anchor that is supposed to be used during the
 *   initial layout of the associated scaffold; it has to be a valid index of the provided [anchors]
 *   otherwise the function throws; by default the value will be -1 and no initial anchor will be
 *   used.
 * @param anchoringAnimationSpec the animation spec used to perform anchoring animation; by default
 *   it will be a spring motion.
 * @param flingBehavior the fling behavior used to handle flings; by default
 *   [ScrollableDefaults.flingBehavior] will be applied.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberPaneExpansionState(
    key: PaneExpansionStateKey = PaneExpansionStateKey.Default,
    anchors: List<PaneExpansionAnchor> = emptyList(),
    initialAnchoredIndex: Int = -1,
    anchoringAnimationSpec: FiniteAnimationSpec<Float> = DefaultAnchoringAnimationSpec,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
): PaneExpansionState {
    fun MutableMap<PaneExpansionStateKey, PaneExpansionStateData>.getOrCreate(
        key: PaneExpansionStateKey,
        initialAnchor: PaneExpansionAnchor?,
    ): PaneExpansionStateData =
        getOrPut(key) { PaneExpansionStateData(currentAnchor = initialAnchor) }

    val dataMap = rememberSaveable(saver = PaneExpansionStateSaver()) { mutableStateMapOf() }
    val initialAnchor =
        remember(anchors, initialAnchoredIndex) {
            if (initialAnchoredIndex == -1) null else anchors[initialAnchoredIndex]
        }
    val expansionState = remember { PaneExpansionState(dataMap.getOrCreate(key, initialAnchor)) }
    LaunchedEffect(key, anchors, anchoringAnimationSpec, flingBehavior) {
        expansionState.restore(
            dataMap.getOrCreate(key, initialAnchor),
            anchors,
            anchoringAnimationSpec,
            flingBehavior,
        )
    }
    return expansionState
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
@Stable
class PaneExpansionState
internal constructor(
    // TODO(conradchen): Handle state change during dragging and settling
    data: PaneExpansionStateData = PaneExpansionStateData(),
    anchors: List<PaneExpansionAnchor> = emptyList(),
) {
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

    /**
     * The current anchor that pane expansion has been settled or is settling to. Note that this
     * field might be `null` if:
     * 1. No anchors have been set to the state.
     * 2. Pane expansion is set directly via [setFirstPaneWidth] or set [setFirstPaneProportion].
     * 3. Pane expansion is in its initial state without an initial anchor provided.
     */
    var currentAnchor
        get() = data.currentAnchorState
        private set(value) {
            data.currentAnchorState = value
        }

    internal val nextAnchor: PaneExpansionAnchor?
        get() {
            // maxExpansionWidth will be initialized in onMeasured and is backed by a state. Check
            // it
            // here so the next anchor will be updated when measuring is done.
            if (maxExpansionWidth == Unspecified || anchors.isEmpty()) {
                return null
            }
            val currentOffset =
                if (currentDraggingOffset == Unspecified) {
                    currentMeasuredDraggingOffset
                } else {
                    currentDraggingOffset
                }
            measuredAnchorPositions.forEach { index, position ->
                if (currentOffset < position) {
                    return anchors[index]
                }
            }
            return anchors[0]
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

    internal var measuredAnchorPositions = IndexedAnchorPositionList(0)
        private set

    private lateinit var anchoringAnimationSpec: FiniteAnimationSpec<Float>

    private lateinit var flingBehavior: FlingBehavior

    private var measuredDensity: Density? = null

    private val dragScope =
        object : DragScope, ScrollScope {
            override fun dragBy(pixels: Float): Unit = draggableState.dispatchRawDelta(pixels)

            override fun scrollBy(pixels: Float): Float { // To support fling
                val offsetBeforeDrag = currentDraggingOffset
                dragBy(pixels)
                val consumed = currentDraggingOffset - offsetBeforeDrag
                return consumed.toFloat()
            }
        }

    private val dragMutex = MutatorMutex()

    internal val draggableState: DraggableState =
        object : DraggableState {
            override fun dispatchRawDelta(delta: Float) {
                if (currentMeasuredDraggingOffset == Unspecified) {
                    return
                }
                currentDraggingOffset = (currentMeasuredDraggingOffset + delta).toInt()
            }

            override suspend fun drag(
                dragPriority: MutatePriority,
                block: suspend DragScope.() -> Unit,
            ) = coroutineScope {
                isDragging = true
                dragMutex.mutateWith(dragScope, dragPriority, block)
                isDragging = false
            }
        }

    /** Returns `true` if none of [firstPaneWidth] or [firstPaneProportion] has been set. */
    fun isUnspecified(): Boolean =
        firstPaneWidth == Unspecified &&
            firstPaneProportion.isNaN() &&
            currentDraggingOffset == Unspecified

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
        currentAnchor = null
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
        currentAnchor = null
    }

    /**
     * Animate the pane expansion to the given [PaneExpansionAnchor]. Note that the given anchor
     * must be one of the provided anchor when creating the state with [rememberPaneExpansionState];
     * otherwise the function throws.
     *
     * @param anchor the anchor to animate to
     * @param initialVelocity the initial velocity of the animation
     */
    suspend fun animateTo(anchor: PaneExpansionAnchor, initialVelocity: Float = 0F) {
        require(anchors.contains(anchor)) { "The provided $anchor is not in the anchor list!" }
        currentAnchor = anchor
        measuredDensity?.apply {
            val position = anchor.positionIn(maxExpansionWidth, this)
            animateToInternal(position, initialVelocity)
        }
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

    internal suspend fun restore(
        data: PaneExpansionStateData,
        anchors: List<PaneExpansionAnchor>,
        anchoringAnimationSpec: FiniteAnimationSpec<Float>,
        flingBehavior: FlingBehavior,
    ) {
        dragMutex.mutate(MutatePriority.PreventUserInput) {
            this.data = data
            this.anchors = anchors
            measuredDensity?.let {
                measuredAnchorPositions =
                    anchors.toPositions(
                        // When maxExpansionWidth is updated, the anchor positions will be
                        // recalculated.
                        maxExpansionWidth,
                        it,
                    )
            }
            if (!anchors.contains(currentAnchor)) {
                currentAnchor = null
            }
            this.anchoringAnimationSpec = anchoringAnimationSpec
            this.flingBehavior = flingBehavior
        }
    }

    internal fun onMeasured(measuredWidth: Int, density: Density) {
        if (measuredWidth == maxExpansionWidth && measuredDensity == density) {
            return
        }
        maxExpansionWidth = measuredWidth
        measuredDensity = density
        Snapshot.withoutReadObservation {
            measuredAnchorPositions = anchors.toPositions(measuredWidth, density)
            // Changes will always apply to the ongoing measurement, no need to trigger remeasuring
            if (currentAnchor != null) {
                currentDraggingOffset = currentAnchor!!.positionIn(measuredWidth, density)
            } else if (currentDraggingOffset != Unspecified) {
                // To re-coerce the value
                currentDraggingOffset = currentDraggingOffset
            }
        }
    }

    internal fun onExpansionOffsetMeasured(measuredOffset: Int) {
        currentMeasuredDraggingOffset = measuredOffset
    }

    internal fun snapToAnchor(anchor: PaneExpansionAnchor) {
        Snapshot.withoutReadObservation {
            measuredDensity?.let {
                currentDraggingOffset = anchor.positionIn(maxExpansionWidth, it)
            }
        }
    }

    internal suspend fun settleToAnchorIfNeeded(velocity: Float) {
        if (measuredAnchorPositions.isEmpty()) {
            return
        }

        dragMutex.mutate(MutatePriority.PreventUserInput) {
            try {
                isSettling = true
                val leftVelocity = flingBehavior.run { dragScope.performFling(velocity) }
                val anchorPosition =
                    measuredAnchorPositions.getPositionOfTheClosestAnchor(
                        currentMeasuredDraggingOffset,
                        leftVelocity,
                    )
                currentAnchor = anchors[anchorPosition.index]
                animateToInternal(anchorPosition.position, leftVelocity)
            } finally {
                isSettling = false
            }
        }
    }

    private suspend fun animateToInternal(offset: Int, initialVelocity: Float) {
        try {
            isSettling = true
            animate(
                currentMeasuredDraggingOffset.toFloat(),
                offset.toFloat(),
                initialVelocity,
                anchoringAnimationSpec,
            ) { value, _ ->
                currentDraggingOffset = value.toInt()
            }
        } finally {
            currentDraggingOffset = offset
            isSettling = false
        }
    }

    private fun IndexedAnchorPositionList.getPositionOfTheClosestAnchor(
        currentPosition: Int,
        velocity: Float,
    ): IndexedAnchorPosition =
        minBy(
            when {
                velocity >= AnchoringVelocityThreshold -> {
                    { anchorPosition: Int ->
                        val delta = anchorPosition - currentPosition
                        if (delta < 0) {
                            // If there's no anchor on the swiping direction, use the closet anchor
                            maxExpansionWidth - delta
                        } else {
                            delta
                        }
                    }
                }
                velocity <= -AnchoringVelocityThreshold -> {
                    { anchorPosition: Int ->
                        val delta = currentPosition - anchorPosition
                        if (delta < 0) {
                            // If there's no anchor on the swiping direction, use the closet anchor
                            maxExpansionWidth - delta
                        } else {
                            delta
                        }
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

        internal val DefaultAnchoringAnimationSpec =
            spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = 1f)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Stable
internal class PaneExpansionStateData(
    firstPaneWidth: Int = Unspecified,
    firstPaneProportion: Float = Float.NaN,
    currentDraggingOffset: Int = Unspecified,
    currentAnchor: PaneExpansionAnchor? = null,
) {
    var firstPaneWidthState by mutableIntStateOf(firstPaneWidth)
    var firstPaneProportionState by mutableFloatStateOf(firstPaneProportion)
    var currentDraggingOffsetState by mutableIntStateOf(currentDraggingOffset)
    var currentAnchorState by mutableStateOf(currentAnchor)

    override fun equals(other: Any?): Boolean =
        // TODO(conradchen): Check if we can remove this by directly reading/writing states in
        //                   PaneExpansionState
        Snapshot.withoutReadObservation {
            if (this === other) return true
            if (other !is PaneExpansionStateData) return false
            if (firstPaneWidthState != other.firstPaneWidthState) return false
            if (firstPaneProportionState != other.firstPaneProportionState) return false
            if (currentDraggingOffsetState != other.currentDraggingOffsetState) return false
            if (currentAnchorState != other.currentAnchorState) return false
            return true
        }

    override fun hashCode(): Int =
        // TODO(conradchen): Check if we can remove this by directly reading/writing states in
        //                   PaneExpansionState
        Snapshot.withoutReadObservation {
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
sealed class PaneExpansionAnchor {
    internal abstract fun positionIn(totalSizePx: Int, density: Density): Int

    internal abstract val type: Int

    /**
     * The description of the anchor that will be used in
     * [androidx.compose.ui.semantics.SemanticsProperties] like accessibility services.
     */
    @get:Composable abstract val description: String

    internal abstract val CompositionLocalConsumerModifierNode.description: String

    /**
     * [PaneExpansionAnchor] implementation that specifies the anchor position in the proportion of
     * the total size of the layout at the start side of the anchor.
     *
     * @param proportion the proportion of the layout at the start side of the anchor. For example,
     *   if the current layout from the start to the end is list-detail, when the proportion value
     *   is 0.3 and this anchor is used, the list pane will occupy 30% of the layout and the detail
     *   pane will occupy 70% of it.
     */
    class Proportion(@FloatRange(0.0, 1.0) val proportion: Float) : PaneExpansionAnchor() {
        override val type = ProportionType

        override val description
            @Composable
            get() =
                getString(
                    Strings.defaultPaneExpansionProportionAnchorDescription,
                    (proportion * 100).toInt(),
                )

        override val CompositionLocalConsumerModifierNode.description: String
            get() =
                getString(
                    Strings.defaultPaneExpansionProportionAnchorDescription,
                    (proportion * 100).toInt(),
                )

        override fun positionIn(totalSizePx: Int, density: Density) =
            (totalSizePx * proportion).roundToInt().coerceIn(0, totalSizePx)

        override fun toString(): String = "PaneExpansionAnchor(Proportion = $proportion)"

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
     * [PaneExpansionAnchor] implementation that specifies the anchor position based on the offset
     * in [Dp].
     *
     * @property offset the offset of the anchor in [Dp].
     */
    abstract class Offset internal constructor(val offset: Dp, override internal val type: Int) :
        PaneExpansionAnchor() {
        /**
         * Indicates the direction of the offset.
         *
         * @see Direction.FromStart
         * @see Direction.FromEnd
         */
        val direction: Direction = Direction(type)

        override fun toString(): String = "PaneExpansionAnchor(Offset = $offset)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Offset) return false
            return offset == other.offset && direction == other.direction
        }

        override fun hashCode(): Int {
            return offset.hashCode() * 31 + direction.hashCode()
        }

        /** Represents the direction from where the offset will be calculated. */
        @JvmInline
        value class Direction internal constructor(internal val value: Int) {
            companion object {
                /**
                 * Indicates the offset will be calculated from the start. For example, if the
                 * offset is 150.dp, the resulted anchor will be at the position that is 150dp away
                 * from the start side of the associated layout.
                 */
                val FromStart = Direction(OffsetFromStartType)

                /**
                 * Indicates the offset will be calculated from the end. For example, if the offset
                 * is 150.dp, the resulted anchor will be at the position that is 150dp away from
                 * the end side of the associated layout.
                 */
                val FromEnd = Direction(OffsetFromEndType)
            }
        }

        private class StartOffset(offset: Dp) : Offset(offset, OffsetFromStartType) {
            override val description
                @Composable
                get() =
                    getString(
                        Strings.defaultPaneExpansionStartOffsetAnchorDescription,
                        offset.value.toInt(),
                    )

            override val CompositionLocalConsumerModifierNode.description
                get() =
                    getString(
                        Strings.defaultPaneExpansionStartOffsetAnchorDescription,
                        offset.value.toInt(),
                    )

            override fun positionIn(totalSizePx: Int, density: Density) =
                with(density) { offset.roundToPx() }
        }

        private class EndOffset(offset: Dp) : Offset(offset, OffsetFromEndType) {
            override val description
                @Composable
                get() =
                    getString(
                        Strings.defaultPaneExpansionEndOffsetAnchorDescription,
                        offset.value.toInt(),
                    )

            override val CompositionLocalConsumerModifierNode.description
                get() =
                    getString(
                        Strings.defaultPaneExpansionEndOffsetAnchorDescription,
                        offset.value.toInt(),
                    )

            override fun positionIn(totalSizePx: Int, density: Density) =
                totalSizePx - with(density) { offset.roundToPx() }
        }

        companion object {
            /**
             * Create an [androidx.compose.material3.adaptive.layout.PaneExpansionAnchor.Offset]
             * anchor from the start side of the layout.
             *
             * @param offset offset to be used in [Dp].
             */
            fun fromStart(offset: Dp): Offset {
                require(offset >= 0.dp) { "Offset must larger than or equal to 0 dp." }
                return StartOffset(offset)
            }

            /**
             * Create an [androidx.compose.material3.adaptive.layout.PaneExpansionAnchor.Offset]
             * anchor from the end side of the layout.
             *
             * @param offset offset to be used in [Dp].
             */
            fun fromEnd(offset: Dp): Offset {
                require(offset >= 0.dp) { "Offset must larger than or equal to 0 dp." }
                return EndOffset(offset)
            }
        }
    }

    internal companion object {
        internal const val UnspecifiedType = 0
        internal const val ProportionType = 1
        internal const val OffsetFromStartType = 2
        internal const val OffsetFromEndType = 3
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberDefaultPaneExpansionState(
    keyProvider: () -> PaneExpansionStateKeyProvider,
    mutable: Boolean,
): PaneExpansionState =
    if (mutable) {
        rememberPaneExpansionState(keyProvider())
    } else {
        remember { PaneExpansionState() } // Use a stub impl to avoid performance overhead
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
        },
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
                },
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
                    PaneExpansionAnchor.OffsetFromStartType ->
                        PaneExpansionAnchor.Offset.fromStart((it[6] as Float).dp)
                    PaneExpansionAnchor.OffsetFromEndType ->
                        PaneExpansionAnchor.Offset.fromEnd((it[6] as Float).dp)
                    else -> null
                }
            object : Map.Entry<PaneExpansionStateKey, PaneExpansionStateData> {
                override val key: PaneExpansionStateKey = key!!
                override val value: PaneExpansionStateData =
                    PaneExpansionStateData(
                        firstPaneWidth = it[2] as Int,
                        firstPaneProportion = it[3] as Float,
                        currentDraggingOffset = it[4] as Int,
                        currentAnchor = currentAnchor,
                    )
            }
        },
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
    density: Density,
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
internal value class IndexedAnchorPositionList(val value: MutableLongList) {
    constructor(size: Int) : this(MutableLongList(size))

    val size
        get() = value.size

    fun isEmpty() = value.isEmpty()

    fun add(position: IndexedAnchorPosition) = value.add(position.value)

    fun sort() = value.sort()

    operator fun get(index: Int) = IndexedAnchorPosition(value[index])
}

internal inline fun IndexedAnchorPositionList.forEach(action: (index: Int, position: Int) -> Unit) {
    value.forEach { with(IndexedAnchorPosition(it)) { action(index, position) } }
}

@JvmInline
internal value class IndexedAnchorPosition(val value: Long) {
    constructor(position: Int, index: Int) : this(packInts(position, index))

    val position
        get() = unpackInt1(value)

    val index
        get() = unpackInt2(value)
}
