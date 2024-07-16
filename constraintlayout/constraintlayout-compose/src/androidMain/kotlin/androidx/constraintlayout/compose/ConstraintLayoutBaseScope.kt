/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLElement
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString
import androidx.constraintlayout.core.state.ConstraintSetParser
import androidx.constraintlayout.core.utils.GridCore
import org.jetbrains.annotations.TestOnly

/**
 * Common scope for [ConstraintLayoutScope] and [ConstraintSetScope], the content being shared
 * between the inline DSL API and the ConstraintSet-based API.
 */
abstract class ConstraintLayoutBaseScope internal constructor(extendFrom: CLObject?) {
    @Suppress("unused") // Needed to maintain binary compatibility
    constructor() : this(null)

    @Deprecated("Tasks is unused, it breaks the immutability promise.")
    protected val tasks = mutableListOf<(State) -> Unit>()

    @PublishedApi
    internal val containerObject: CLObject = extendFrom?.clone() ?: CLObject(charArrayOf())

    fun applyTo(state: State) {
        ConstraintSetParser.populateState(
            containerObject,
            state,
            ConstraintSetParser.LayoutVariables()
        )
    }

    open fun reset() {
        containerObject.clear()
        helperId = HelpersStartId
        helpersHashCode = 0
    }

    @PublishedApi internal var helpersHashCode: Int = 0

    private fun updateHelpersHashCode(value: Int) {
        helpersHashCode = (helpersHashCode * 1009 + value) % 1000000007
    }

    private val HelpersStartId = 1000
    private var helperId = HelpersStartId

    private fun createHelperId() = helperId++

    /**
     * Represents a vertical anchor (e.g. start/end of a layout, guideline) that layouts can link to
     * in their `Modifier.constrainAs` or `constrain` blocks.
     *
     * @param reference The [LayoutReference] that this anchor belongs to.
     */
    @Stable
    data class VerticalAnchor
    internal constructor(
        internal val id: Any,
        internal val index: Int,
        val reference: LayoutReference
    )

    /**
     * Represents a horizontal anchor (e.g. top/bottom of a layout, guideline) that layouts can link
     * to in their `Modifier.constrainAs` or `constrain` blocks.
     *
     * @param reference The [LayoutReference] that this anchor belongs to.
     */
    @Stable
    data class HorizontalAnchor
    internal constructor(
        internal val id: Any,
        internal val index: Int,
        val reference: LayoutReference
    )

    /**
     * Represents a horizontal anchor corresponding to the [FirstBaseline] of a layout that other
     * layouts can link to in their `Modifier.constrainAs` or `constrain` blocks.
     *
     * @param reference The [LayoutReference] that this anchor belongs to.
     */
    // TODO(popam): investigate if this can be just a HorizontalAnchor
    @Stable
    data class BaselineAnchor
    internal constructor(internal val id: Any, val reference: LayoutReference)

    /**
     * Specifies additional constraints associated to the horizontal chain identified with [ref].
     */
    fun constrain(
        ref: HorizontalChainReference,
        constrainBlock: HorizontalChainScope.() -> Unit
    ): HorizontalChainScope =
        HorizontalChainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /** Specifies additional constraints associated to the vertical chain identified with [ref]. */
    fun constrain(
        ref: VerticalChainReference,
        constrainBlock: VerticalChainScope.() -> Unit
    ): VerticalChainScope = VerticalChainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /** Specifies the constraints associated to the layout identified with [ref]. */
    fun constrain(
        ref: ConstrainedLayoutReference,
        constrainBlock: ConstrainScope.() -> Unit
    ): ConstrainScope = ConstrainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /** Convenient way to apply the same constraints to multiple [ConstrainedLayoutReference]s. */
    fun constrain(
        vararg refs: ConstrainedLayoutReference,
        constrainBlock: ConstrainScope.() -> Unit
    ) {
        refs.forEach { ref -> constrain(ref, constrainBlock) }
    }

    /** Creates a guideline at a specific offset from the start of the [ConstraintLayout]. */
    fun createGuidelineFromStart(offset: Dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            putNumber("start", offset.value)
        }

        updateHelpersHashCode(1)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates a guideline at a specific offset from the left of the [ConstraintLayout]. */
    fun createGuidelineFromAbsoluteLeft(offset: Dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            putNumber("left", offset.value)
        }

        updateHelpersHashCode(2)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a specific offset from the start of the [ConstraintLayout]. A
     * [fraction] of 0f will correspond to the start of the [ConstraintLayout], while 1f will
     * correspond to the end.
     */
    fun createGuidelineFromStart(fraction: Float): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val percentParams =
            CLArray(charArrayOf()).apply {
                add(CLString.from("start"))
                add(CLNumber(fraction))
            }

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            put("percent", percentParams)
        }

        updateHelpersHashCode(3)
        updateHelpersHashCode(fraction.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a width fraction from the left of the [ConstraintLayout]. A [fraction]
     * of 0f will correspond to the left of the [ConstraintLayout], while 1f will correspond to the
     * right.
     */
    fun createGuidelineFromAbsoluteLeft(fraction: Float): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            putNumber("percent", fraction)
        }

        updateHelpersHashCode(4)
        updateHelpersHashCode(fraction.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates a guideline at a specific offset from the end of the [ConstraintLayout]. */
    fun createGuidelineFromEnd(offset: Dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            putNumber("end", offset.value)
        }

        updateHelpersHashCode(5)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates a guideline at a specific offset from the right of the [ConstraintLayout]. */
    fun createGuidelineFromAbsoluteRight(offset: Dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            putNumber("right", offset.value)
        }

        updateHelpersHashCode(6)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a width fraction from the end of the [ConstraintLayout]. A [fraction]
     * of 0f will correspond to the end of the [ConstraintLayout], while 1f will correspond to the
     * start.
     */
    fun createGuidelineFromEnd(fraction: Float): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val percentParams =
            CLArray(charArrayOf()).apply {
                add(CLString.from("end"))
                add(CLNumber(fraction))
            }

        ref.asCLContainer().apply {
            putString("type", "vGuideline")
            put("percent", percentParams)
        }

        updateHelpersHashCode(3)
        updateHelpersHashCode(fraction.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a width fraction from the right of the [ConstraintLayout]. A
     * [fraction] of 0f will correspond to the right of the [ConstraintLayout], while 1f will
     * correspond to the left.
     */
    fun createGuidelineFromAbsoluteRight(fraction: Float): VerticalAnchor {
        return createGuidelineFromAbsoluteLeft(1f - fraction)
    }

    /** Creates a guideline at a specific offset from the top of the [ConstraintLayout]. */
    fun createGuidelineFromTop(offset: Dp): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "hGuideline")
            putNumber("start", offset.value)
        }

        updateHelpersHashCode(7)
        updateHelpersHashCode(offset.hashCode())
        return HorizontalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a height fraction from the top of the [ConstraintLayout]. A [fraction]
     * of 0f will correspond to the top of the [ConstraintLayout], while 1f will correspond to the
     * bottom.
     */
    fun createGuidelineFromTop(fraction: Float): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "hGuideline")
            putNumber("percent", fraction)
        }

        updateHelpersHashCode(8)
        updateHelpersHashCode(fraction.hashCode())
        return HorizontalAnchor(ref.id, 0, ref)
    }

    /** Creates a guideline at a specific offset from the bottom of the [ConstraintLayout]. */
    fun createGuidelineFromBottom(offset: Dp): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        ref.asCLContainer().apply {
            putString("type", "hGuideline")
            putNumber("end", offset.value)
        }

        updateHelpersHashCode(9)
        updateHelpersHashCode(offset.hashCode())
        return HorizontalAnchor(ref.id, 0, ref)
    }

    /**
     * Creates a guideline at a height percentage from the bottom of the [ConstraintLayout]. A
     * [fraction] of 0f will correspond to the bottom of the [ConstraintLayout], while 1f will
     * correspond to the top.
     */
    fun createGuidelineFromBottom(fraction: Float): HorizontalAnchor {
        return createGuidelineFromTop(1f - fraction)
    }

    /** Creates and returns a start barrier, containing the specified elements. */
    fun createStartBarrier(vararg elements: LayoutReference, margin: Dp = 0.dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "start")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(10)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates and returns a left barrier, containing the specified elements. */
    fun createAbsoluteLeftBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "left")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(11)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates and returns a top barrier, containing the specified elements. */
    fun createTopBarrier(vararg elements: LayoutReference, margin: Dp = 0.dp): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "top")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(12)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return HorizontalAnchor(ref.id, 0, ref)
    }

    /** Creates and returns an end barrier, containing the specified elements. */
    fun createEndBarrier(vararg elements: LayoutReference, margin: Dp = 0.dp): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "end")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(13)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates and returns a right barrier, containing the specified elements. */
    fun createAbsoluteRightBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "right")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(14)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(ref.id, 0, ref)
    }

    /** Creates and returns a bottom barrier, containing the specified elements. */
    fun createBottomBarrier(vararg elements: LayoutReference, margin: Dp = 0.dp): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }

        ref.asCLContainer().apply {
            putString("type", "barrier")
            putString("direction", "bottom")
            putNumber("margin", margin.value)
            put("contains", elementArray)
        }

        updateHelpersHashCode(15)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return HorizontalAnchor(ref.id, 0, ref)
    }

    /**
     * Flow helpers allows a long sequence of Composable widgets to wrap onto multiple rows or
     * columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true arranges the Composables from top to bottom. Normally
     *   they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param padding sets padding around the content
     * @param wrapMode sets the way reach maxElements is handled [Wrap.None] (default) -- no wrap
     *   behavior, [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or
     *   SpreadInside)
     */
    fun createFlow(
        vararg elements: LayoutReference?,
        flowVertically: Boolean = false,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        maxElement: Int = 0, // TODO: shouldn't this be -1? (aka: UNKNOWN)?
        padding: Dp = 0.dp,
        wrapMode: Wrap = Wrap.None,
        verticalAlign: VerticalAlign = VerticalAlign.Center,
        horizontalAlign: HorizontalAlign = HorizontalAlign.Center,
        horizontalFlowBias: Float = 0.0f,
        verticalFlowBias: Float = 0.0f,
        verticalStyle: FlowStyle = FlowStyle.Packed,
        horizontalStyle: FlowStyle = FlowStyle.Packed,
    ): ConstrainedLayoutReference {
        return createFlow(
            elements = elements,
            flowVertically = flowVertically,
            verticalGap = verticalGap,
            horizontalGap = horizontalGap,
            maxElement = maxElement,
            paddingLeft = padding,
            paddingTop = padding,
            paddingRight = padding,
            paddingBottom = padding,
            wrapMode = wrapMode,
            verticalAlign = verticalAlign,
            horizontalAlign = horizontalAlign,
            horizontalFlowBias = horizontalFlowBias,
            verticalFlowBias = verticalFlowBias,
            verticalStyle = verticalStyle,
            horizontalStyle = horizontalStyle
        )
    }

    /**
     * Flow helpers allows a long sequence of Composable widgets to wrap onto multiple rows or
     * columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true aranges the Composables from top to bottom. Normally
     *   they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param paddingHorizontal sets paddingLeft and paddingRight of the content
     * @param paddingVertical sets paddingTop and paddingBottom of the content
     * @param wrapMode sets the way reach maxElements is handled [Wrap.None] (default) -- no wrap
     *   behavior, [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or
     *   SpreadInside)
     */
    fun createFlow(
        vararg elements: LayoutReference?,
        flowVertically: Boolean = false,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        maxElement: Int = 0,
        paddingHorizontal: Dp = 0.dp,
        paddingVertical: Dp = 0.dp,
        wrapMode: Wrap = Wrap.None,
        verticalAlign: VerticalAlign = VerticalAlign.Center,
        horizontalAlign: HorizontalAlign = HorizontalAlign.Center,
        horizontalFlowBias: Float = 0.0f,
        verticalFlowBias: Float = 0.0f,
        verticalStyle: FlowStyle = FlowStyle.Packed,
        horizontalStyle: FlowStyle = FlowStyle.Packed,
    ): ConstrainedLayoutReference {
        return createFlow(
            elements = elements,
            flowVertically = flowVertically,
            verticalGap = verticalGap,
            horizontalGap = horizontalGap,
            maxElement = maxElement,
            paddingLeft = paddingHorizontal,
            paddingTop = paddingVertical,
            paddingRight = paddingHorizontal,
            paddingBottom = paddingVertical,
            wrapMode = wrapMode,
            verticalAlign = verticalAlign,
            horizontalAlign = horizontalAlign,
            horizontalFlowBias = horizontalFlowBias,
            verticalFlowBias = verticalFlowBias,
            verticalStyle = verticalStyle,
            horizontalStyle = horizontalStyle
        )
    }

    /**
     * Flow helpers allows a long sequence of Composable widgets to wrap onto multiple rows or
     * columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true aranges the Composables from top to bottom. Normally
     *   they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param paddingLeft sets paddingLeft of the content
     * @param paddingTop sets paddingTop of the content
     * @param paddingRight sets paddingRight of the content
     * @param paddingBottom sets paddingBottom of the content
     * @param wrapMode sets the way reach maxElements is handled [Wrap.None] (default) -- no wrap
     *   behavior, [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or
     *   SpreadInside)
     */
    fun createFlow(
        vararg elements: LayoutReference?,
        flowVertically: Boolean = false,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        maxElement: Int = 0,
        paddingLeft: Dp = 0.dp,
        paddingTop: Dp = 0.dp,
        paddingRight: Dp = 0.dp,
        paddingBottom: Dp = 0.dp,
        wrapMode: Wrap = Wrap.None,
        verticalAlign: VerticalAlign = VerticalAlign.Center,
        horizontalAlign: HorizontalAlign = HorizontalAlign.Center,
        horizontalFlowBias: Float = 0.0f,
        verticalFlowBias: Float = 0.0f,
        verticalStyle: FlowStyle = FlowStyle.Packed,
        horizontalStyle: FlowStyle = FlowStyle.Packed,
    ): ConstrainedLayoutReference {
        val ref = ConstrainedLayoutReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            if (it != null) {
                elementArray.add(CLString.from(it.id.toString()))
            }
        }
        val paddingArray =
            CLArray(charArrayOf()).apply {
                add(CLNumber(paddingLeft.value))
                add(CLNumber(paddingTop.value))
                add(CLNumber(paddingRight.value))
                add(CLNumber(paddingBottom.value))
            }
        ref.asCLContainer().apply {
            put("contains", elementArray)
            putString("type", if (flowVertically) "vFlow" else "hFlow")
            putNumber("vGap", verticalGap.value)
            putNumber("hGap", horizontalGap.value)
            putNumber("maxElement", maxElement.toFloat())
            put("padding", paddingArray)
            putString("wrap", wrapMode.name)
            putString("vAlign", verticalAlign.name)
            putString("hAlign", horizontalAlign.name)
            putNumber("hFlowBias", horizontalFlowBias)
            putNumber("vFlowBias", verticalFlowBias)
            putString("vStyle", verticalStyle.name)
            putString("hStyle", horizontalStyle.name)
        }
        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }

        return ref
    }

    /**
     * Creates a Grid based helper that lays out its elements in a single Row.
     *
     * Example:
     *
     * @sample androidx.constraintlayout.compose.samples.Row_sample
     * @param elements [LayoutReference]s to be laid out by the Grid-based Row helper.
     * @param spacing Defines the horizontal spacing between each item in the Row.
     * @param weights Defines the weight for each element in the Row. Note that the number of
     *   weights provided are expected to match the number of [elements] given.
     * @throws IllegalArgumentException When non empty [weights] don't match the number of elements.
     * @see createGrid
     */
    @SuppressLint("Range") // Enables internal grid mode for row and column
    fun createRow(
        vararg elements: LayoutReference,
        spacing: Dp = 0.dp,
        weights: FloatArray = floatArrayOf(),
    ): ConstrainedLayoutReference {
        if (weights.isNotEmpty() && elements.size != weights.size) {
            throw IllegalArgumentException(
                "Number of weights (${weights.size}) should match number of elements (${elements.size})."
            )
        }
        return createGrid(
            elements = elements,
            rows = 1,
            columns = 0,
            horizontalSpacing = spacing,
            columnWeights = weights,
        )
    }

    /**
     * Creates a Grid based helper that lays out its elements in a single Column.
     *
     * Example:
     *
     * @sample androidx.constraintlayout.compose.samples.Column_sample
     * @param elements [LayoutReference]s to be laid out by the Grid-based Column helper
     * @param spacing Defines the vertical spacing between each item in the Column.
     * @param weights Defines the weight for each element in the Column. Note that the number of
     *   weights provided are expected to match the number of [elements] given.
     * @throws IllegalArgumentException When non empty [weights] don't match the number of elements.
     * @see createGrid
     */
    @SuppressLint("Range") // Enables internal grid mode for row and column
    fun createColumn(
        vararg elements: LayoutReference,
        spacing: Dp = 0.dp,
        weights: FloatArray = floatArrayOf(),
    ): ConstrainedLayoutReference {
        if (weights.isNotEmpty() && elements.size != weights.size) {
            throw IllegalArgumentException(
                "Number of weights (${weights.size}) should match number of elements (${elements.size})."
            )
        }
        return createGrid(
            elements = elements,
            rows = 0,
            columns = 1,
            verticalSpacing = spacing,
            rowWeights = weights,
        )
    }

    /**
     * Helper that creates a fixed Grid layout.
     *
     * A grid is defined by a set number of rows and columns. By default the given [elements] are
     * arranged horizontally first (left to right, then top to bottom).
     *
     * Either [rowWeights] or [columnWeights] may be provided to modify their size in the grid
     * relative to each other.
     *
     * You may provide [Span]s to define size of each cell within the grid.
     *
     * To avoid placing Layouts in specific cells (or an area within the grid), you may provide
     * [skips]. Note that if the provided [Span]s and [Skip]s overlap, the [Skip]s will take
     * priority, ignoring the overlapping [Span] definition.
     *
     * Here's an example showing how to build a calculator layout using a couple of [Span]s:
     *
     * @sample androidx.constraintlayout.compose.samples.Grid_calculator_sample
     *
     * Here's another example using [Skip]s to easily lay out the typical Keyboard navigation pad:
     *
     * @sample androidx.constraintlayout.compose.samples.Grid_navigationPad_sample
     * @param elements [LayoutReference]s to be laid out by the Grid helper. By default, they are
     *   positioned in the given order based on the arrangement. Horizontal arrangement by default.
     * @param rows Sets the number of rows in the Grid
     * @param columns Sets the number of columns in the Grid
     * @param isHorizontalArrangement Whether to place the given [elements] horizontally, filling
     *   the cells from left to right and top to bottom. Otherwise, the [elements] are placed
     *   vertically, filling each cell from top to bottom and left to right. `true` by default.
     * @param verticalSpacing Defines the gap between each row.
     * @param horizontalSpacing Defines the gap between each column.
     * @param rowWeights Defines the weight for each row. The weight specifies how much space each
     *   row takes relative to each other. Should be either an empty array (all rows are the same
     *   size), or have a value corresponding for each row.
     * @param columnWeights Defines the weight for each column. The weight specifies how much space
     *   each column takes relative to each other. Should be either an empty array (all columns are
     *   the same size), or have a value corresponding for each column.
     * @param skips A [Skip] defines an area within the Grid where Layouts may **not** be placed.
     *   So, as the [elements] are being placed, they will skip any cell covered by the given skips.
     * @param spans A [Span] defines how much area should each cell occupy when placing an item on
     *   it. Keep in mind that when laying out, the Grid won't place any overlapping items over the
     *   spanned area. In that sense, a [Span] works similarly to a [Skip], except that an item will
     *   be placed at the original spanned cell position. Also note, [skips] take priority over
     *   spans, meaning that defining a [Span] that overlaps a [Skip] is a no-op.
     * @param flags A [GridFlag] definition that may change certain behaviors of the Grid helper.
     *   [GridFlag.None] by default.
     * @throws IllegalArgumentException When non empty weights don't match the number of columns or
     *   rows respectively.
     * @see createColumn
     * @see createRow
     */
    fun createGrid(
        vararg elements: LayoutReference,
        @IntRange(from = 1) rows: Int,
        @IntRange(from = 1) columns: Int,
        isHorizontalArrangement: Boolean = true,
        verticalSpacing: Dp = 0.dp,
        horizontalSpacing: Dp = 0.dp,
        rowWeights: FloatArray = floatArrayOf(),
        columnWeights: FloatArray = floatArrayOf(),
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        flags: GridFlag = GridFlag.None,
    ): ConstrainedLayoutReference {
        if (rowWeights.isNotEmpty() && rows > 0 && rows != rowWeights.size) {
            throw IllegalArgumentException(
                "Number of weights (${rowWeights.size}) should match number of rows ($rows)."
            )
        }
        if (columnWeights.isNotEmpty() && columns > 0 && columns != columnWeights.size) {
            throw IllegalArgumentException(
                "Number of weights (${columnWeights.size}) should match number of columns ($columns)."
            )
        }

        val ref = ConstrainedLayoutReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        elements.forEach { elementArray.add(CLString.from(it.id.toString())) }
        var strRowWeights = ""
        var strColumnWeights = ""
        if (rowWeights.size > 1) {
            strRowWeights = rowWeights.joinToString(",")
        }
        if (columnWeights.size > 1) {
            strColumnWeights = columnWeights.joinToString(",")
        }

        var strSkips = ""
        var strSpans = ""
        if (skips.isNotEmpty()) {
            strSkips = skips.joinToString(",") { it.description }
        }
        if (spans.isNotEmpty()) {
            strSpans = spans.joinToString(",") { it.description }
        }

        ref.asCLContainer().apply {
            put("contains", elementArray)
            putString("type", "grid")
            putNumber("orientation", if (isHorizontalArrangement) 0f else 1f)
            putNumber("rows", rows.toFloat())
            putNumber("columns", columns.toFloat())
            putNumber("vGap", verticalSpacing.value)
            putNumber("hGap", horizontalSpacing.value)
            putString("rowWeights", strRowWeights)
            putString("columnWeights", strColumnWeights)
            putString("skips", strSkips)
            putString("spans", strSpans)
            putNumber("flags", flags.value.toFloat())
        }

        return ref
    }

    /**
     * Creates a horizontal chain including the referenced layouts.
     *
     * Use [constrain] with the resulting [HorizontalChainReference] to modify the start/left and
     * end/right constraints of this chain.
     */
    fun createHorizontalChain(
        vararg elements: LayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): HorizontalChainReference {
        val ref = HorizontalChainReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            val chainParams = it.getHelperParams<ChainParams>()
            val elementContent: CLElement =
                if (chainParams != null) {
                    CLArray(charArrayOf()).apply {
                        add(CLString.from(it.id.toString()))
                        add(CLNumber(chainParams.weight))
                        add(CLNumber(chainParams.startMargin.value))
                        add(CLNumber(chainParams.endMargin.value))
                        add(CLNumber(chainParams.startGoneMargin.value))
                        add(CLNumber(chainParams.endGoneMargin.value))
                    }
                } else {
                    CLString.from(it.id.toString())
                }
            elementArray.add(elementContent)
        }
        val styleArray = CLArray(charArrayOf())
        styleArray.add(CLString.from(chainStyle.name))
        styleArray.add(CLNumber(chainStyle.bias ?: 0.5f))

        ref.asCLContainer().apply {
            putString("type", "hChain")
            put("contains", elementArray)
            put("style", styleArray)
        }

        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(chainStyle.hashCode())
        return ref
    }

    /**
     * Creates a vertical chain including the referenced layouts.
     *
     * Use [constrain] with the resulting [VerticalChainReference] to modify the top and bottom
     * constraints of this chain.
     */
    fun createVerticalChain(
        vararg elements: LayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): VerticalChainReference {
        val ref = VerticalChainReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            val chainParams = it.getHelperParams<ChainParams>()
            val elementContent: CLElement =
                if (chainParams != null) {
                    CLArray(charArrayOf()).apply {
                        add(CLString.from(it.id.toString()))
                        add(CLNumber(chainParams.weight))
                        add(CLNumber(chainParams.topMargin.value))
                        add(CLNumber(chainParams.bottomMargin.value))
                        add(CLNumber(chainParams.topGoneMargin.value))
                        add(CLNumber(chainParams.bottomGoneMargin.value))
                    }
                } else {
                    CLString.from(it.id.toString())
                }
            elementArray.add(elementContent)
        }
        val styleArray = CLArray(charArrayOf())
        styleArray.add(CLString.from(chainStyle.name))
        styleArray.add(CLNumber(chainStyle.bias ?: 0.5f))

        ref.asCLContainer().apply {
            putString("type", "vChain")
            put("contains", elementArray)
            put("style", styleArray)
        }

        updateHelpersHashCode(17)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(chainStyle.hashCode())
        return ref
    }

    /**
     * Sets the parameters that are used by chains to customize the resulting layout.
     *
     * Use margins to customize the space between widgets in the chain.
     *
     * Use weight to distribute available space to each widget when their dimensions are not fixed.
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in Chains.
     *
     * Since margins are only for widgets within the chain: Top, Start and End, Bottom margins are
     * ignored when the widget is the first or the last element in the chain, respectively.
     *
     * @param startMargin Added space from the start of this widget to the previous widget
     * @param topMargin Added space from the top of this widget to the previous widget
     * @param endMargin Added space from the end of this widget to the next widget
     * @param bottomMargin Added space from the bottom of this widget to the next widget
     * @param startGoneMargin Added space from the start of this widget when the previous widget has
     *   [Visibility.Gone]
     * @param topGoneMargin Added space from the top of this widget when the previous widget has
     *   [Visibility.Gone]
     * @param endGoneMargin Added space from the end of this widget when the next widget has
     *   [Visibility.Gone]
     * @param bottomGoneMargin Added space from the bottom of this widget when the next widget has
     *   [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     *   layout when the corresponding dimension is not a fixed value.
     * @return The same [LayoutReference] instance with the applied values
     */
    fun LayoutReference.withChainParams(
        startMargin: Dp = 0.dp,
        topMargin: Dp = 0.dp,
        endMargin: Dp = 0.dp,
        bottomMargin: Dp = 0.dp,
        startGoneMargin: Dp = 0.dp,
        topGoneMargin: Dp = 0.dp,
        endGoneMargin: Dp = 0.dp,
        bottomGoneMargin: Dp = 0.dp,
        weight: Float = Float.NaN,
    ): LayoutReference =
        this.apply {
            setHelperParams(
                ChainParams(
                    startMargin = startMargin,
                    topMargin = topMargin,
                    endMargin = endMargin,
                    bottomMargin = bottomMargin,
                    startGoneMargin = startGoneMargin,
                    topGoneMargin = topGoneMargin,
                    endGoneMargin = endGoneMargin,
                    bottomGoneMargin = bottomGoneMargin,
                    weight = weight
                )
            )
        }

    /**
     * Sets the parameters that are used by horizontal chains to customize the resulting layout.
     *
     * Use margins to customize the space between widgets in the chain.
     *
     * Use weight to distribute available space to each widget when their horizontal dimension is
     * not fixed.
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in Chains.
     *
     * Since margins are only for widgets within the chain: Start and End margins are ignored when
     * the widget is the first or the last element in the chain, respectively.
     *
     * @param startMargin Added space from the start of this widget to the previous widget
     * @param endMargin Added space from the end of this widget to the next widget
     * @param startGoneMargin Added space from the start of this widget when the previous widget has
     *   [Visibility.Gone]
     * @param endGoneMargin Added space from the end of this widget when the next widget has
     *   [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     *   layout when the width is not a fixed dimension.
     * @return The same [LayoutReference] instance with the applied values
     */
    fun LayoutReference.withHorizontalChainParams(
        startMargin: Dp = 0.dp,
        endMargin: Dp = 0.dp,
        startGoneMargin: Dp = 0.dp,
        endGoneMargin: Dp = 0.dp,
        weight: Float = Float.NaN
    ): LayoutReference =
        withChainParams(
            startMargin = startMargin,
            topMargin = 0.dp,
            endMargin = endMargin,
            bottomMargin = 0.dp,
            startGoneMargin = startGoneMargin,
            topGoneMargin = 0.dp,
            endGoneMargin = endGoneMargin,
            bottomGoneMargin = 0.dp,
            weight = weight
        )

    /**
     * Sets the parameters that are used by vertical chains to customize the resulting layout.
     *
     * Use margins to customize the space between widgets in the chain.
     *
     * Use weight to distribute available space to each widget when their vertical dimension is not
     * fixed.
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in Chains.
     *
     * Since margins are only for widgets within the chain: Top and Bottom margins are ignored when
     * the widget is the first or the last element in the chain, respectively.
     *
     * @param topMargin Added space from the top of this widget to the previous widget
     * @param bottomMargin Added space from the bottom of this widget to the next widget
     * @param topGoneMargin Added space from the top of this widget when the previous widget has
     *   [Visibility.Gone]
     * @param bottomGoneMargin Added space from the bottom of this widget when the next widget has
     *   [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     *   layout when the height is not a fixed dimension.
     * @return The same [LayoutReference] instance with the applied values
     */
    fun LayoutReference.withVerticalChainParams(
        topMargin: Dp = 0.dp,
        bottomMargin: Dp = 0.dp,
        topGoneMargin: Dp = 0.dp,
        bottomGoneMargin: Dp = 0.dp,
        weight: Float = Float.NaN
    ): LayoutReference =
        withChainParams(
            startMargin = 0.dp,
            topMargin = topMargin,
            endMargin = 0.dp,
            bottomMargin = bottomMargin,
            startGoneMargin = 0.dp,
            topGoneMargin = topGoneMargin,
            endGoneMargin = 0.dp,
            bottomGoneMargin = bottomGoneMargin,
            weight = weight
        )

    internal fun LayoutReference.asCLContainer(): CLObject {
        val idString = id.toString()
        if (containerObject.getObjectOrNull(idString) == null) {
            containerObject.put(idString, CLObject(charArrayOf()))
        }
        return containerObject.getObject(idString)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is ConstraintLayoutBaseScope) {
            return containerObject == other.containerObject
        }
        return false
    }

    override fun hashCode(): Int {
        return containerObject.hashCode()
    }
}

/**
 * Represents a [ConstraintLayout] item that requires a unique identifier. Typically a layout or a
 * helper such as barriers, guidelines or chains.
 */
@Stable
abstract class LayoutReference internal constructor(internal open val id: Any) {
    /**
     * This map should be used to store one instance of different implementations of [HelperParams].
     */
    private val helperParamsMap: MutableMap<String, HelperParams> = mutableMapOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayoutReference) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    internal fun setHelperParams(helperParams: HelperParams) {
        // Use the class name to force one instance per implementation
        helperParams::class.simpleName?.let { helperParamsMap[it] = helperParams }
    }

    /**
     * Returns the [HelperParams] that corresponds to the class type [T]. Null if no instance of
     * type [T] has been set.
     */
    internal inline fun <reified T> getHelperParams(): T? where T : HelperParams {
        return helperParamsMap[T::class.simpleName] as? T
    }
}

/**
 * Helpers that need parameters on a per-widget basis may implement this interface to store custom
 * parameters within [LayoutReference].
 *
 * @see [LayoutReference.getHelperParams]
 * @see [LayoutReference.setHelperParams]
 */
internal interface HelperParams

/**
 * Parameters that may be defined for each widget within a chain.
 *
 * These will always be used instead of similarly named parameters defined with other calls such as
 * [ConstrainScope.linkTo].
 */
internal class ChainParams(
    val startMargin: Dp,
    val topMargin: Dp,
    val endMargin: Dp,
    val bottomMargin: Dp,
    val startGoneMargin: Dp,
    val topGoneMargin: Dp,
    val endGoneMargin: Dp,
    val bottomGoneMargin: Dp,
    val weight: Float,
) : HelperParams {
    companion object {
        internal val Default =
            ChainParams(
                startMargin = 0.dp,
                topMargin = 0.dp,
                endMargin = 0.dp,
                bottomMargin = 0.dp,
                startGoneMargin = 0.dp,
                topGoneMargin = 0.dp,
                endGoneMargin = 0.dp,
                bottomGoneMargin = 0.dp,
                weight = Float.NaN
            )
    }
}

/**
 * Basic implementation of [LayoutReference], used as fallback for items that don't fit other
 * implementations of [LayoutReference], such as [ConstrainedLayoutReference].
 */
@Stable internal class LayoutReferenceImpl internal constructor(id: Any) : LayoutReference(id)

/**
 * Represents a layout within a [ConstraintLayout].
 *
 * This is a [LayoutReference] that may be constrained to other elements.
 */
@Stable
class ConstrainedLayoutReference(override val id: Any) : LayoutReference(id) {
    /**
     * The start anchor of this layout. Represents left in LTR layout direction, or right in RTL.
     */
    @Stable val start = ConstraintLayoutBaseScope.VerticalAnchor(id, -2, this)

    /** The left anchor of this layout. */
    @Stable val absoluteLeft = ConstraintLayoutBaseScope.VerticalAnchor(id, 0, this)

    /** The top anchor of this layout. */
    @Stable val top = ConstraintLayoutBaseScope.HorizontalAnchor(id, 0, this)

    /** The end anchor of this layout. Represents right in LTR layout direction, or left in RTL. */
    @Stable val end = ConstraintLayoutBaseScope.VerticalAnchor(id, -1, this)

    /** The right anchor of this layout. */
    @Stable val absoluteRight = ConstraintLayoutBaseScope.VerticalAnchor(id, 1, this)

    /** The bottom anchor of this layout. */
    @Stable val bottom = ConstraintLayoutBaseScope.HorizontalAnchor(id, 1, this)

    /** The baseline anchor of this layout. */
    @Stable val baseline = ConstraintLayoutBaseScope.BaselineAnchor(id, this)
}

/**
 * Represents a horizontal chain within a [ConstraintLayout].
 *
 * The anchors correspond to the first and last elements in the chain.
 */
@Stable
class HorizontalChainReference internal constructor(id: Any) : LayoutReference(id) {
    /**
     * The start anchor of the first element in the chain.
     *
     * Represents left in LTR layout direction, or right in RTL.
     */
    @Stable val start = ConstraintLayoutBaseScope.VerticalAnchor(id, -2, this)

    /** The left anchor of the first element in the chain. */
    @Stable val absoluteLeft = ConstraintLayoutBaseScope.VerticalAnchor(id, 0, this)

    /**
     * The end anchor of the last element in the chain.
     *
     * Represents right in LTR layout direction, or left in RTL.
     */
    @Stable val end = ConstraintLayoutBaseScope.VerticalAnchor(id, -1, this)

    /** The right anchor of the last element in the chain. */
    @Stable val absoluteRight = ConstraintLayoutBaseScope.VerticalAnchor(id, 1, this)
}

/**
 * Represents a vertical chain within a [ConstraintLayout].
 *
 * The anchors correspond to the first and last elements in the chain.
 */
@Stable
class VerticalChainReference internal constructor(id: Any) : LayoutReference(id) {
    /** The top anchor of the first element in the chain. */
    @Stable val top = ConstraintLayoutBaseScope.HorizontalAnchor(id, 0, this)

    /** The bottom anchor of the last element in the chain. */
    @Stable val bottom = ConstraintLayoutBaseScope.HorizontalAnchor(id, 1, this)
}

/** The style of a horizontal or vertical chain. */
@Immutable
class ChainStyle internal constructor(internal val name: String, internal val bias: Float? = null) {
    companion object {
        /** A chain style that evenly distributes the contained layouts. */
        @Stable val Spread = ChainStyle("spread")

        /**
         * A chain style where the first and last layouts are affixed to the constraints on each end
         * of the chain and the rest are evenly distributed.
         */
        @Stable val SpreadInside = ChainStyle("spread_inside")

        /**
         * A chain style where the contained layouts are packed together and placed to the center of
         * the available space.
         */
        @Stable val Packed = Packed(0.5f)

        /**
         * A chain style where the contained layouts are packed together and placed in the available
         * space according to a given [bias].
         */
        @Stable fun Packed(bias: Float) = ChainStyle("packed", bias)
    }
}

/** The overall visibility of a widget in a [ConstraintLayout]. */
@Immutable
class Visibility internal constructor(internal val name: String) {
    companion object {
        /**
         * Indicates that the widget will be painted in the [ConstraintLayout]. All render-time
         * transforms will apply normally.
         */
        @Stable val Visible = Visibility("visible")

        /**
         * The widget will not be painted in the [ConstraintLayout] but its dimensions and
         * constraints will still apply.
         *
         * Equivalent to forcing the alpha to 0.0.
         */
        @Stable val Invisible = Visibility("invisible")

        /**
         * Like [Invisible], but the dimensions of the widget will collapse to (0,0), the
         * constraints will still apply.
         */
        @Stable val Gone = Visibility("gone")
    }
}

/**
 * Set of individual options that may change the Grid helper behavior, each flag can be combined
 * with the [GridFlag.or] operator.
 *
 * By default, the Grid helper places its [LayoutReference]s as given in the `elements` parameter.
 * Following arrangement rules (skips, spans and orientation).
 *
 * However, when [isPlaceLayoutsOnSpansFirst] is `true`. The given [LayoutReference]s will be first
 * placed on the cells occupied by the given `spans` array. Then, the remaining layouts are placed
 * on the remaining cells following typical arrangement rules.
 *
 * For example, on a grid layout with 1 row and 3 columns, placing two widgets: w0, w1, with a span
 * defined as `Span(position = 1, rows = 1, columns = 2)`. The grid layout by default would place
 * them as `[w0 w1 w1]`. Whereas when [isPlaceLayoutsOnSpansFirst] is `true`, they'd be placed as
 * `[w1 w0 w0]`.
 *
 * In some situations, [isPlaceLayoutsOnSpansFirst] can make it easier to match the desired layouts
 * with the given spans on the Grid.
 *
 * @see ConstraintLayoutBaseScope.createGrid
 */
@JvmInline
value class GridFlag private constructor(internal val value: Int) {

    /**
     * Handles the conversion of compose flags to :constraintlayout-core flags, handled like this
     * since we invert the meaning of one the flags for API ergonomics in Compose.
     */
    private constructor(
        isPlaceLayoutsOnSpansFirst: Boolean = false,
        // isSubGridByColRow is only expected to be used on tests
        isSubGridByColRow: Boolean = false
    ) : this(
        (if (isPlaceLayoutsOnSpansFirst) 0 else GridCore.SPANS_RESPECT_WIDGET_ORDER) or
            (if (isSubGridByColRow) GridCore.SUB_GRID_BY_COL_ROW else 0)
    )

    /** `or` operator override to allow combining flags */
    infix fun or(other: GridFlag): GridFlag =
        // Again, implemented like this as the flag handling is non-standard. It differs from the
        // :constraintlayout-core flag behaviors.
        GridFlag(
            isPlaceLayoutsOnSpansFirst or other.isPlaceLayoutsOnSpansFirst,
            isSubGridByColRow or other.isSubGridByColRow
        )

    /**
     * When true, the Grid helper will first place Layouts on cells occupied by spans, then fill the
     * remaining cells following the typical arrangement rules.
     */
    val isPlaceLayoutsOnSpansFirst: Boolean
        get() = value and GridCore.SPANS_RESPECT_WIDGET_ORDER == 0

    /**
     * Whether area definitions in Spans and Skips are treated as "columns by rows".
     *
     * Note that this property is only relevant for testing.
     */
    internal val isSubGridByColRow: Boolean
        get() = value and GridCore.SUB_GRID_BY_COL_ROW > 0

    override fun toString(): String =
        "GridFlag(isPlaceLayoutsOnSpansFirst = $isPlaceLayoutsOnSpansFirst)"

    companion object {
        /** All default behaviors apply. */
        val None = GridFlag()

        /**
         * Creates a [GridFlag] instance with `isPlaceLayoutsOnSpansFirst` as `true`.
         *
         * Making it so that when placing the layouts, they are first placed on cells occupied by
         * spans, then, any remaining layouts are placed on the remaining cells following the
         * typical arrangement rules.
         */
        val PlaceLayoutsOnSpansFirst = GridFlag(isPlaceLayoutsOnSpansFirst = true)

        /** Not relevant for the public API, only used now to test "internal" features. */
        @TestOnly internal val SubGridByColRow = GridFlag(isSubGridByColRow = true)
    }
}

/** Wrap defines the type of chain */
@Immutable
class Wrap internal constructor(internal val name: String) {
    companion object {
        val None = Wrap("none")
        val Chain = Wrap("chain")
        val Aligned = Wrap("aligned")
    }
}

/** Defines how objects align vertically within the chain */
@Immutable
class VerticalAlign internal constructor(internal val name: String) {
    companion object {
        val Top = VerticalAlign("top")
        val Bottom = VerticalAlign("bottom")
        val Center = VerticalAlign("center")
        val Baseline = VerticalAlign("baseline")
    }
}

/** Defines how objects align horizontally in the chain */
@Immutable
class HorizontalAlign internal constructor(internal val name: String) {
    companion object {
        val Start = HorizontalAlign("start")
        val End = HorizontalAlign("end")
        val Center = HorizontalAlign("center")
    }
}

/** Defines how widgets are spaced in a chain */
@Immutable
class FlowStyle internal constructor(internal val name: String) {
    companion object {
        val Spread = FlowStyle("spread")
        val SpreadInside = FlowStyle("spread_inside")
        val Packed = FlowStyle("packed")
    }
}

/**
 * Defines how many rows and/or columns to skip, starting from the given position. For Grid, specify
 * the Skip with Skip(position, rows, columns) For Row/Column, specify the Skip with Skip(position,
 * size)
 *
 * @param description string to specify span. For Grid: "position:rowsxcolumns"; For Row/Columns:
 *   "position:size"
 * @constructor create a new Skip containing the position and size information of the skipped area
 */
@JvmInline
value class Skip private constructor(val description: String) {
    constructor(
        @IntRange(from = 0) position: Int,
        @IntRange(from = 1) rows: Int,
        @IntRange(from = 1) columns: Int
    ) : this("$position:${rows}x$columns")

    constructor(
        @IntRange(from = 0) position: Int,
        @IntRange(from = 1) size: Int
    ) : this("$position:$size")
}

/**
 * Defines the spanned area (that crosses multiple columns and/or rows) that a widget will take when
 * placed at the given position. For Grid, specify the Span with Span(position, rows, columns) For
 * Row/Column, specify the Span with Span(position, size)
 *
 * @param description string to specify skip. For Grid: "position:rowsxcolumns"; For Row/Columns:
 *   "position:size"
 * @constructor create a new Span containing the position and size information of the spanned area
 */
@JvmInline
value class Span(val description: String) {
    constructor(
        @IntRange(from = 0) position: Int,
        @IntRange(from = 1) rows: Int,
        @IntRange(from = 1) columns: Int
    ) : this("$position:${rows}x$columns")

    constructor(
        @IntRange(from = 0) position: Int,
        @IntRange(from = 1) size: Int
    ) : this("$position:$size")
}
