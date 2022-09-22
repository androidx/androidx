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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.widgets.ConstraintWidget

/**
 * Common scope for [ConstraintLayoutScope] and [ConstraintSetScope], the content being shared
 * between the inline DSL API and the ConstraintSet-based API.
 */
abstract class ConstraintLayoutBaseScope {
    protected val tasks = mutableListOf<(State) -> Unit>()

    fun applyTo(state: State): Unit = tasks.forEach { it(state) }

    open fun reset() {
        tasks.clear()
        helperId = HelpersStartId
        helpersHashCode = 0
    }

    @PublishedApi
    internal var helpersHashCode: Int = 0

    private fun updateHelpersHashCode(value: Int) {
        helpersHashCode = (helpersHashCode * 1009 + value) % 1000000007
    }

    private val HelpersStartId = 1000
    private var helperId = HelpersStartId
    private fun createHelperId() = helperId++

    /**
     * Represents a vertical anchor (e.g. start/end of a layout, guideline) that layouts
     * can link to in their `Modifier.constrainAs` or `constrain` blocks.
     *
     * @param reference The [LayoutReference] that this anchor belongs to.
     */
    @Stable
    data class VerticalAnchor internal constructor(
        internal val id: Any,
        internal val index: Int,
        val reference: LayoutReference
    )

    /**
     * Represents a horizontal anchor (e.g. top/bottom of a layout, guideline) that layouts
     * can link to in their `Modifier.constrainAs` or `constrain` blocks.
     *
     * @param reference The [LayoutReference] that this anchor belongs to.
     */
    @Stable
    data class HorizontalAnchor internal constructor(
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
    data class BaselineAnchor internal constructor(
        internal val id: Any,
        val reference: LayoutReference
    )

    /**
     * Specifies additional constraints associated to the horizontal chain identified with [ref].
     */
    fun constrain(
        ref: HorizontalChainReference,
        constrainBlock: HorizontalChainScope.() -> Unit
    ): HorizontalChainScope = HorizontalChainScope(ref.id).apply {
        constrainBlock()
        this@ConstraintLayoutBaseScope.tasks.addAll(this.tasks)
    }

    /**
     * Specifies additional constraints associated to the vertical chain identified with [ref].
     */
    fun constrain(
        ref: VerticalChainReference,
        constrainBlock: VerticalChainScope.() -> Unit
    ): VerticalChainScope = VerticalChainScope(ref.id).apply {
        constrainBlock()
        this@ConstraintLayoutBaseScope.tasks.addAll(this.tasks)
    }

    /**
     * Specifies the constraints associated to the layout identified with [ref].
     */
    fun constrain(
        ref: ConstrainedLayoutReference,
        constrainBlock: ConstrainScope.() -> Unit
    ): ConstrainScope = ConstrainScope(ref.id).apply {
        constrainBlock()
        this@ConstraintLayoutBaseScope.tasks.addAll(this.tasks)
    }

    /**
     * Creates a guideline at a specific offset from the start of the [ConstraintLayout].
     */
    fun createGuidelineFromStart(offset: Dp): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.verticalGuideline(id).apply {
                if (state.layoutDirection == LayoutDirection.Ltr) start(offset) else end(offset)
            }
        }
        updateHelpersHashCode(1)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a specific offset from the left of the [ConstraintLayout].
     */
    fun createGuidelineFromAbsoluteLeft(offset: Dp): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.verticalGuideline(id).apply { start(offset) } }
        updateHelpersHashCode(2)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a specific offset from the start of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the start of the [ConstraintLayout], while 1f will
     * correspond to the end.
     */
    fun createGuidelineFromStart(fraction: Float): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.verticalGuideline(id).apply {
                if (state.layoutDirection == LayoutDirection.Ltr) {
                    percent(fraction)
                } else {
                    percent(1f - fraction)
                }
            }
        }
        updateHelpersHashCode(3)
        updateHelpersHashCode(fraction.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a width fraction from the left of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the left of the [ConstraintLayout], while 1f will
     * correspond to the right.
     */
    // TODO(popam, b/157781990): this is not really percenide
    fun createGuidelineFromAbsoluteLeft(fraction: Float): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.verticalGuideline(id).apply { percent(fraction) } }
        updateHelpersHashCode(4)
        updateHelpersHashCode(fraction.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a specific offset from the end of the [ConstraintLayout].
     */
    fun createGuidelineFromEnd(offset: Dp): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.verticalGuideline(id).apply {
                if (state.layoutDirection == LayoutDirection.Ltr) end(offset) else start(offset)
            }
        }
        updateHelpersHashCode(5)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a specific offset from the right of the [ConstraintLayout].
     */
    fun createGuidelineFromAbsoluteRight(offset: Dp): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.verticalGuideline(id).apply { end(offset) } }
        updateHelpersHashCode(6)
        updateHelpersHashCode(offset.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a width fraction from the end of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the end of the [ConstraintLayout], while 1f will
     * correspond to the start.
     */
    fun createGuidelineFromEnd(fraction: Float): VerticalAnchor {
        return createGuidelineFromStart(1f - fraction)
    }

    /**
     * Creates a guideline at a width fraction from the right of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the right of the [ConstraintLayout], while 1f will
     * correspond to the left.
     */
    fun createGuidelineFromAbsoluteRight(fraction: Float): VerticalAnchor {
        return createGuidelineFromAbsoluteLeft(1f - fraction)
    }

    /**
     * Creates a guideline at a specific offset from the top of the [ConstraintLayout].
     */
    fun createGuidelineFromTop(offset: Dp): HorizontalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.horizontalGuideline(id).apply { start(offset) } }
        updateHelpersHashCode(7)
        updateHelpersHashCode(offset.hashCode())
        return HorizontalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a height percenide from the top of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the top of the [ConstraintLayout], while 1f will
     * correspond to the bottom.
     */
    fun createGuidelineFromTop(fraction: Float): HorizontalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.horizontalGuideline(id).apply { percent(fraction) } }
        updateHelpersHashCode(8)
        updateHelpersHashCode(fraction.hashCode())
        return HorizontalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a specific offset from the bottom of the [ConstraintLayout].
     */
    fun createGuidelineFromBottom(offset: Dp): HorizontalAnchor {
        val id = createHelperId()
        tasks.add { state -> state.horizontalGuideline(id).apply { end(offset) } }
        updateHelpersHashCode(9)
        updateHelpersHashCode(offset.hashCode())
        return HorizontalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates a guideline at a height percenide from the bottom of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the bottom of the [ConstraintLayout], while 1f will
     * correspond to the top.
     */
    fun createGuidelineFromBottom(fraction: Float): HorizontalAnchor {
        return createGuidelineFromTop(1f - fraction)
    }

    /**
     * Creates and returns a start barrier, containing the specified elements.
     */
    fun createStartBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            val direction = if (state.layoutDirection == LayoutDirection.Ltr) {
                SolverDirection.LEFT
            } else {
                SolverDirection.RIGHT
            }
            state.barrier(id, direction).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(10)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates and returns a left barrier, containing the specified elements.
     */
    fun createAbsoluteLeftBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.barrier(id, SolverDirection.LEFT).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(11)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates and returns a top barrier, containing the specified elements.
     */
    fun createTopBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): HorizontalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.barrier(id, SolverDirection.TOP).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(12)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return HorizontalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates and returns an end barrier, containing the specified elements.
     */
    fun createEndBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            val direction = if (state.layoutDirection == LayoutDirection.Ltr) {
                SolverDirection.RIGHT
            } else {
                SolverDirection.LEFT
            }
            state.barrier(id, direction).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(13)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates and returns a right barrier, containing the specified elements.
     */
    fun createAbsoluteRightBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.barrier(id, SolverDirection.RIGHT).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(14)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return VerticalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * Creates and returns a bottom barrier, containing the specified elements.
     */
    fun createBottomBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): HorizontalAnchor {
        val id = createHelperId()
        tasks.add { state ->
            state.barrier(id, SolverDirection.BOTTOM).apply {
                add(*(elements.map { it.id }.toTypedArray()))
            }.margin(state.convertDimension(margin))
        }
        updateHelpersHashCode(15)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(margin.hashCode())
        return HorizontalAnchor(id, 0, LayoutReferenceImpl(id))
    }

    /**
     * This creates a flow helper
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     * [flowVertically] if set to true aranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * [verticalGap] defines the gap between views in the y axis
     * [horizontalGap] defines the gap between views in the x axis
     * [maxElement] defines the maximum element on a row before it if the
     * [padding] sets padding around the content
     * [wrapMode] sets the way reach maxElements is handled
     * Flow.WRAP_NONE (default) -- no wrap behavior,
     * Flow.WRAP_CHAIN - create additional chains
     * [verticalAlign] set the way elements are aligned vertically. Center is default
     * [horizontalAlign] set the way elements are aligned horizontally. Center is default
     * [horizontalFlowBias] set the way elements are aligned vertically Center is default
     * [verticalFlowBias] sets the top bottom bias of the vertical chain
     * [verticalStyle] sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * [horizontalStyle] set the style of the horizontal chain (Spread, Packed, or SpreadInside)
     */
    fun createFlow(
        vararg elements: LayoutReference?,
        flowVertically: Boolean = false,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        maxElement: Int = 0,
        padding: Dp = 0.dp,
        wrapMode: Wrap = Wrap.None,
        verticalAlign: VerticalAlign = VerticalAlign.Center,
        horizontalAlign: HorizontalAlign = HorizontalAlign.Center,
        horizontalFlowBias: Float = 0.0f,
        verticalFlowBias: Float = 0.0f,
        verticalStyle: FlowStyle = FlowStyle.Packed,
        horizontalStyle: FlowStyle = FlowStyle.Packed,
    ): ConstrainedLayoutReference {
        val id = createHelperId()
        tasks.add { state ->
            state.getFlow(id, flowVertically).apply {
                add(*(elements.map { it?.id }.toTypedArray()))
                horizontalChainStyle = horizontalStyle.style
                setVerticalChainStyle(verticalStyle.style)
                verticalBias(verticalFlowBias)
                horizontalBias(horizontalFlowBias)
                setHorizontalAlign(horizontalAlign.mode)
                setVerticalAlign(verticalAlign.mode)
                setWrapMode(wrapMode.mode)
                paddingLeft = state.convertDimension(padding)
                paddingTop = state.convertDimension(padding)
                paddingRight = state.convertDimension(padding)
                paddingBottom = state.convertDimension(padding)
                maxElementsWrap = maxElement
                setHorizontalGap(state.convertDimension(horizontalGap))
                setVerticalGap(state.convertDimension(verticalGap))
            }
        }
        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }

        return ConstrainedLayoutReference(id)
    }

    /**
     * This creates a flow helper
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     * [flowVertically] if set to true aranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * [verticalGap] defines the gap between views in the y axis
     * [horizontalGap] defines the gap between views in the x axis
     * [maxElement] defines the maximum element on a row before it if the
     * [paddingHorizontal] sets paddingLeft and paddingRight of the content
     * [paddingVertical] sets paddingTop and paddingBottom of the content
     * [wrapMode] sets the way reach maxElements is handled
     * Flow.WRAP_NONE (default) -- no wrap behavior,
     * Flow.WRAP_CHAIN - create additional chains
     * [verticalAlign] set the way elements are aligned vertically. Center is default
     * [horizontalAlign] set the way elements are aligned horizontally. Center is default
     * [horizontalFlowBias] set the way elements are aligned vertically Center is default
     * [verticalFlowBias] sets the top bottom bias of the vertical chain
     * [verticalStyle] sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * [horizontalStyle] set the style of the horizontal chain (Spread, Packed, or SpreadInside)
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
        val id = createHelperId()
        tasks.add { state ->
            state.getFlow(id, flowVertically).apply {
                add(*(elements.map { it?.id }.toTypedArray()))
                horizontalChainStyle = horizontalStyle.style
                setVerticalChainStyle(verticalStyle.style)
                verticalBias(verticalFlowBias)
                horizontalBias(horizontalFlowBias)
                setHorizontalAlign(horizontalAlign.mode)
                setVerticalAlign(verticalAlign.mode)
                setWrapMode(wrapMode.mode)
                paddingLeft = state.convertDimension(paddingHorizontal)
                paddingTop = state.convertDimension(paddingVertical)
                paddingRight = state.convertDimension(paddingHorizontal)
                paddingBottom = state.convertDimension(paddingVertical)
                maxElementsWrap = maxElement
                setHorizontalGap(state.convertDimension(horizontalGap))
                setVerticalGap(state.convertDimension(verticalGap))
            }
        }
        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }

        return ConstrainedLayoutReference(id)
    }

    /**
     * This creates a flow helper
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     * [flowVertically] if set to true aranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * [verticalGap] defines the gap between views in the y axis
     * [horizontalGap] defines the gap between views in the x axis
     * [maxElement] defines the maximum element on a row before it if the
     * [paddingLeft] sets paddingLeft of the content
     * [paddingTop] sets paddingTop of the content
     * [paddingRight] sets paddingRight of the content
     * [paddingBottom] sets paddingBottom of the content
     * [wrapMode] sets the way reach maxElements is handled
     * Flow.WRAP_NONE (default) -- no wrap behavior,
     * Flow.WRAP_CHAIN - create additional chains
     * [verticalAlign] set the way elements are aligned vertically. Center is default
     * [horizontalAlign] set the way elements are aligned horizontally. Center is default
     * [horizontalFlowBias] set the way elements are aligned vertically Center is default
     * [verticalFlowBias] sets the top bottom bias of the vertical chain
     * [verticalStyle] sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * [horizontalStyle] set the style of the horizontal chain (Spread, Packed, or SpreadInside)
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
        val id = createHelperId()
        tasks.add { state ->
            state.getFlow(id, flowVertically).apply {
                add(*(elements.map { it?.id }.toTypedArray()))
                horizontalChainStyle = horizontalStyle.style
                setVerticalChainStyle(verticalStyle.style)
                verticalBias(verticalFlowBias)
                horizontalBias(horizontalFlowBias)
                setHorizontalAlign(horizontalAlign.mode)
                setVerticalAlign(verticalAlign.mode)
                setWrapMode(wrapMode.mode)
                setPaddingLeft(state.convertDimension(paddingLeft))
                setPaddingTop(state.convertDimension(paddingTop))
                setPaddingRight(state.convertDimension(paddingRight))
                setPaddingBottom(state.convertDimension(paddingBottom))
                maxElementsWrap = maxElement
                setHorizontalGap(state.convertDimension(horizontalGap))
                setVerticalGap(state.convertDimension(verticalGap))
            }
        }
        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }

         return ConstrainedLayoutReference(id)
    }

    /**
     * Creates a horizontal chain including the referenced layouts.
     *
     * Use [constrain] with the resulting [HorizontalChainReference] to modify the start/left and
     * end/right constraints of this chain.
     */
    // TODO(popam, b/157783937): this API should be improved
    fun createHorizontalChain(
        vararg elements: LayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): HorizontalChainReference {
        val id = createHelperId()
        tasks.add { state ->
            val helper = state.helper(
                id,
                androidx.constraintlayout.core.state.State.Helper.HORIZONTAL_CHAIN
            ) as androidx.constraintlayout.core.state.helpers.HorizontalChainReference
            helper.add(*(elements.map { it.id }.toTypedArray()))
            helper.style(chainStyle.style)
            helper.apply()
            if (chainStyle.bias != null) {
                state.constraints(elements[0].id).horizontalBias(chainStyle.bias)
            }
        }
        updateHelpersHashCode(16)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(chainStyle.hashCode())
        return HorizontalChainReference(id)
    }

    /**
     * Creates a vertical chain including the referenced layouts.
     *
     * Use [constrain] with the resulting [VerticalChainReference] to modify the top and
     * bottom constraints of this chain.
     */
    // TODO(popam, b/157783937): this API should be improved
    fun createVerticalChain(
        vararg elements: LayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): VerticalChainReference {
        val id = createHelperId()
        tasks.add { state ->
            val helper = state.helper(
                id,
                androidx.constraintlayout.core.state.State.Helper.VERTICAL_CHAIN
            ) as androidx.constraintlayout.core.state.helpers.VerticalChainReference
            helper.add(*(elements.map { it.id }.toTypedArray()))
            helper.style(chainStyle.style)
            helper.apply()
            if (chainStyle.bias != null) {
                state.constraints(elements[0].id).verticalBias(chainStyle.bias)
            }
        }
        updateHelpersHashCode(17)
        elements.forEach { updateHelpersHashCode(it.hashCode()) }
        updateHelpersHashCode(chainStyle.hashCode())
        return VerticalChainReference(id)
    }
}

/**
 * Represents a [ConstraintLayout] item that requires a unique identifier. Typically a layout or a
 * helper such as barriers, guidelines or chains.
 */
@Stable
abstract class LayoutReference internal constructor(internal open val id: Any) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayoutReference

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Basic implementation of [LayoutReference], used as fallback for items that don't fit other
 * implementations of [LayoutReference], such as [ConstrainedLayoutReference].
 */
@Stable
internal class LayoutReferenceImpl internal constructor(id: Any) : LayoutReference(id)

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
    @Stable
    val start = ConstraintLayoutBaseScope.VerticalAnchor(id, -2, this)

    /**
     * The left anchor of this layout.
     */
    @Stable
    val absoluteLeft = ConstraintLayoutBaseScope.VerticalAnchor(id, 0, this)

    /**
     * The top anchor of this layout.
     */
    @Stable
    val top = ConstraintLayoutBaseScope.HorizontalAnchor(id, 0, this)

    /**
     * The end anchor of this layout. Represents right in LTR layout direction, or left in RTL.
     */
    @Stable
    val end = ConstraintLayoutBaseScope.VerticalAnchor(id, -1, this)

    /**
     * The right anchor of this layout.
     */
    @Stable
    val absoluteRight = ConstraintLayoutBaseScope.VerticalAnchor(id, 1, this)

    /**
     * The bottom anchor of this layout.
     */
    @Stable
    val bottom = ConstraintLayoutBaseScope.HorizontalAnchor(id, 1, this)

    /**
     * The baseline anchor of this layout.
     */
    @Stable
    val baseline = ConstraintLayoutBaseScope.BaselineAnchor(id, this)
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
    @Stable
    val start = ConstraintLayoutBaseScope.VerticalAnchor(id, -2, this)

    /**
     * The left anchor of the first element in the chain.
     */
    @Stable
    val absoluteLeft = ConstraintLayoutBaseScope.VerticalAnchor(id, 0, this)

    /**
     * The end anchor of the last element in the chain.
     *
     * Represents right in LTR layout direction, or left in RTL.
     */
    @Stable
    val end = ConstraintLayoutBaseScope.VerticalAnchor(id, -1, this)

    /**
     * The right anchor of the last element in the chain.
     */
    @Stable
    val absoluteRight = ConstraintLayoutBaseScope.VerticalAnchor(id, 1, this)
}

/**
 * Represents a vertical chain within a [ConstraintLayout].
 *
 * The anchors correspond to the first and last elements in the chain.
 */
@Stable
class VerticalChainReference internal constructor(id: Any) : LayoutReference(id) {
    /**
     * The top anchor of the first element in the chain.
     */
    @Stable
    val top = ConstraintLayoutBaseScope.HorizontalAnchor(id, 0, this)

    /**
     * The bottom anchor of the last element in the chain.
     */
    @Stable
    val bottom = ConstraintLayoutBaseScope.HorizontalAnchor(id, 1, this)
}

/**
 * The style of a horizontal or vertical chain.
 */
@Immutable
class ChainStyle internal constructor(
    internal val style: SolverChain,
    internal val bias: Float? = null
) {
    companion object {
        /**
         * A chain style that evenly distributes the contained layouts.
         */
        @Stable
        val Spread = ChainStyle(SolverChain.SPREAD)

        /**
         * A chain style where the first and last layouts are affixed to the constraints
         * on each end of the chain and the rest are evenly distributed.
         */
        @Stable
        val SpreadInside = ChainStyle(SolverChain.SPREAD_INSIDE)

        /**
         * A chain style where the contained layouts are packed together and placed to the
         * center of the available space.
         */
        @Stable
        val Packed = Packed(0.5f)

        /**
         * A chain style where the contained layouts are packed together and placed in
         * the available space according to a given [bias].
         */
        @Stable
        fun Packed(bias: Float) = ChainStyle(SolverChain.PACKED, bias)
    }
}

/**
 * The overall visibility of a widget in a [ConstraintLayout].
 */
@Immutable
class Visibility internal constructor(
    internal val solverValue: Int
) {
    companion object {
        /**
         * Indicates that the widget will be painted in the [ConstraintLayout]. All render-time
         * transforms will apply normally.
         */
        @Stable
        val Visible = Visibility(ConstraintWidget.VISIBLE)

        /**
         * The widget will not be painted in the [ConstraintLayout] but its dimensions and constraints
         * will still apply.
         *
         * Equivalent to forcing the alpha to 0.0.
         */
        @Stable
        val Invisible = Visibility(ConstraintWidget.INVISIBLE)

        /**
         * Like [Invisible], but the dimensions of the widget will collapse to (0,0), the
         * constraints will still apply.
         */
        @Stable
        val Gone = Visibility(ConstraintWidget.GONE)
    }
}

/**
 * Wrap defines the type of chain
 */
@Immutable
class Wrap internal constructor(
    internal val mode: Int
    ) {
    companion object {
        val None =
            Wrap(androidx.constraintlayout.core.widgets.Flow.WRAP_NONE)
        val Chain =
            Wrap(androidx.constraintlayout.core.widgets.Flow.WRAP_CHAIN)
        val Aligned =
            Wrap(androidx.constraintlayout.core.widgets.Flow.WRAP_ALIGNED)
    }
}

/**
 * Defines how objects align vertically within the chain
 */
@Immutable
class VerticalAlign internal constructor(
    internal val mode: Int
    ) {
    companion object {
        val Top = VerticalAlign(androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_TOP)
        val Bottom =
            VerticalAlign(androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_BOTTOM)
        val Center =
            VerticalAlign(androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_CENTER)
        val Baseline =
            VerticalAlign(androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_BASELINE)
    }
}

/**
 * Defines how objects align horizontally in the chain
 */
@Immutable
class HorizontalAlign internal constructor(
    internal val mode: Int
    ) {
    companion object {
        val Start =
            HorizontalAlign(androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_START)
        val End = HorizontalAlign(androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_END)
        val Center =
            HorizontalAlign(androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_CENTER)
    }
}

/**
 * Defines how widgets are spaced in a chain
 */
@Immutable
class FlowStyle internal constructor(
    internal val style: Int
    ) {
    companion object {
        val Spread = FlowStyle(0)
        val SpreadInside = FlowStyle(1)
        val Packed = FlowStyle(2)
    }
}