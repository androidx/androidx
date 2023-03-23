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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLElement
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString
import androidx.constraintlayout.core.state.ConstraintSetParser

/**
 * Common scope for [ConstraintLayoutScope] and [ConstraintSetScope], the content being shared
 * between the inline DSL API and the ConstraintSet-based API.
 */
abstract class ConstraintLayoutBaseScope internal constructor(extendFrom: CLObject?) {
    @Suppress("unused") // Needed to maintain binary compatibility
    constructor() : this(null)

    @Deprecated("Tasks is unused, it breaks the immutability promise.")
    protected val tasks = mutableListOf<(State) -> Unit>()

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
    ): HorizontalChainScope =
        HorizontalChainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /**
     * Specifies additional constraints associated to the vertical chain identified with [ref].
     */
    fun constrain(
        ref: VerticalChainReference,
        constrainBlock: VerticalChainScope.() -> Unit
    ): VerticalChainScope = VerticalChainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /**
     * Specifies the constraints associated to the layout identified with [ref].
     */
    fun constrain(
        ref: ConstrainedLayoutReference,
        constrainBlock: ConstrainScope.() -> Unit
    ): ConstrainScope = ConstrainScope(ref.id, ref.asCLContainer()).apply(constrainBlock)

    /**
     * Convenient way to apply the same constraints to multiple [ConstrainedLayoutReference]s.
     */
    fun constrain(
        vararg refs: ConstrainedLayoutReference,
        constrainBlock: ConstrainScope.() -> Unit
    ) {
        refs.forEach { ref ->
            constrain(ref, constrainBlock)
        }
    }

    /**
     * Creates a guideline at a specific offset from the start of the [ConstraintLayout].
     */
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

    /**
     * Creates a guideline at a specific offset from the left of the [ConstraintLayout].
     */
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
     * Creates a guideline at a specific offset from the start of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the start of the [ConstraintLayout], while 1f will
     * correspond to the end.
     */
    fun createGuidelineFromStart(fraction: Float): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val percentParams = CLArray(charArrayOf()).apply {
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
     * Creates a guideline at a width fraction from the left of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the left of the [ConstraintLayout], while 1f will
     * correspond to the right.
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

    /**
     * Creates a guideline at a specific offset from the end of the [ConstraintLayout].
     */
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

    /**
     * Creates a guideline at a specific offset from the right of the [ConstraintLayout].
     */
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
     * Creates a guideline at a width fraction from the end of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the end of the [ConstraintLayout], while 1f will
     * correspond to the start.
     */
    fun createGuidelineFromEnd(fraction: Float): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val percentParams = CLArray(charArrayOf()).apply {
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
     * Creates a guideline at a height fraction from the top of the [ConstraintLayout].
     * A [fraction] of 0f will correspond to the top of the [ConstraintLayout], while 1f will
     * correspond to the bottom.
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

    /**
     * Creates a guideline at a specific offset from the bottom of the [ConstraintLayout].
     */
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
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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

    /**
     * Creates and returns a left barrier, containing the specified elements.
     */
    fun createAbsoluteLeftBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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

    /**
     * Creates and returns a top barrier, containing the specified elements.
     */
    fun createTopBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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

    /**
     * Creates and returns an end barrier, containing the specified elements.
     */
    fun createEndBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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

    /**
     * Creates and returns a right barrier, containing the specified elements.
     */
    fun createAbsoluteRightBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): VerticalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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

    /**
     * Creates and returns a bottom barrier, containing the specified elements.
     */
    fun createBottomBarrier(
        vararg elements: LayoutReference,
        margin: Dp = 0.dp
    ): HorizontalAnchor {
        val ref = LayoutReferenceImpl(createHelperId())

        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }

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
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true arranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param padding sets padding around the content
     * @param wrapMode sets the way reach maxElements is handled
     * [Wrap.None] (default) -- no wrap behavior,
     * [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or SpreadInside)
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
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true aranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param paddingHorizontal sets paddingLeft and paddingRight of the content
     * @param paddingVertical sets paddingTop and paddingBottom of the content
     * @param wrapMode sets the way reach maxElements is handled
     * [Wrap.None] (default) -- no wrap behavior,
     * [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or SpreadInside)
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
     * Flow helpers allows a long sequence of Composable widgets to wrap onto
     * multiple rows or columns.
     *
     * @param elements [LayoutReference]s to be laid out by the Flow helper
     * @param flowVertically if set to true aranges the Composables from top to bottom.
     * Normally they are arranged from left to right.
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param maxElement defines the maximum element on a row before it if the
     * @param paddingLeft sets paddingLeft of the content
     * @param paddingTop sets paddingTop of the content
     * @param paddingRight sets paddingRight of the content
     * @param paddingBottom sets paddingBottom of the content
     * @param wrapMode sets the way reach maxElements is handled
     * [Wrap.None] (default) -- no wrap behavior,
     * [Wrap.Chain] - create additional chains
     * @param verticalAlign set the way elements are aligned vertically. Center is default
     * @param horizontalAlign set the way elements are aligned horizontally. Center is default
     * @param horizontalFlowBias set the way elements are aligned vertically Center is default
     * @param verticalFlowBias sets the top bottom bias of the vertical chain
     * @param verticalStyle sets the style of a vertical chain (Spread,Packed, or SpreadInside)
     * @param horizontalStyle set the style of the horizontal chain (Spread, Packed, or SpreadInside)
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
        val paddingArray = CLArray(charArrayOf()).apply {
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
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val weights = intArrayOf(3, 3, 2, 2, 1)
     *      val g1 = createRow(
     *          a, b, c, d, e,
     *          skips = arrayOf(Skip(1, 1), Skip(3, 2)),
     *          spans = arrayOf(Span(1, 2)),
     *          horizontalGap = 10.dp,
     *          columnWeights = weights,
     *          padding = 10.dp,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param skips specify area(s) in a Row to be skipped - format: Skip(index, size)
     * @param spans specify area(s) in a Row to be spanned - format: Span(index, size)
     * @param horizontalGap defines the gap between views in the x axis
     * @param columnWeights defines the weight of each column
     * @param padding sets padding around the content
     */
    fun createRow(
        vararg elements: LayoutReference,
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        horizontalGap: Dp = 0.dp,
        columnWeights: IntArray = intArrayOf(),
        padding: Dp = 0.dp,
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            rows = 1,
            skips = skips,
            spans = spans,
            horizontalGap = horizontalGap,
            columnWeights = columnWeights,
            paddingStart = padding,
            paddingTop = padding,
            paddingEnd = padding,
            paddingBottom = padding,
        )
    }

    /**
     * Creates a Grid based helper that lays out its elements in a single Row.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val weights = intArrayOf(3, 3, 2, 2, 1)
     *      val g1 = createRow(
     *          a, b, c, d, e,
     *          skips = arrayOf(Skip(1, 1), Skip(3, 2)),
     *          spans = arrayOf(Span(1, 2)),
     *          horizontalGap = 10.dp,
     *          columnWeights = weights,
     *          paddingHorizontal = 10.dp,
     *          paddingVertical = 10.dp,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *       }
     *   }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param skips specify area(s) in a Row to be skipped - format: Skip(index, size)
     * @param spans specify area(s) in a Row to be spanned - format: Span(index, size)
     * @param horizontalGap defines the gap between views in the y axis
     * @param columnWeights defines the weight of each column
     * @param paddingHorizontal sets paddingStart and paddingEnd of the content
     * @param paddingVertical sets paddingTop and paddingBottom of the content
     */
    fun createRow(
        vararg elements: LayoutReference,
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        horizontalGap: Dp = 0.dp,
        columnWeights: IntArray = intArrayOf(),
        paddingHorizontal: Dp = 0.dp,
        paddingVertical: Dp = 0.dp,
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            rows = 1,
            skips = skips,
            spans = spans,
            horizontalGap = horizontalGap,
            columnWeights = columnWeights,
            paddingStart = paddingHorizontal,
            paddingTop = paddingVertical,
            paddingEnd = paddingHorizontal,
            paddingBottom = paddingVertical,
        )
    }

    /**
     * Creates a Grid based helper that lays out its elements in a single Column.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val weights = intArrayOf(3, 3, 2, 2, 1)
     *      val g1 = createColumn(
     *          a, b, c, d, e,
     *          skips = arrayOf(Skip(1, 1), Skip(3, 2)),
     *          spans = arrayOf(Span(1, 2)),
     *          verticalGap = 10.dp,
     *          rowWeights = weights,
     *          padding = 10.dp,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param spans specify area(s) in a Column to be spanned - format: Span(index, size)
     * @param skips specify area(s) in a Column to be skipped - format: Skip(index, size)
     * @param verticalGap defines the gap between views in the y axis
     * @param rowWeights defines the weight of each row
     * @param padding sets padding around the content
     */
    fun createColumn(
        vararg elements: LayoutReference,
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        rowWeights: IntArray = intArrayOf(),
        verticalGap: Dp = 0.dp,
        padding: Dp = 0.dp,
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            columns = 1,
            skips = skips,
            spans = spans,
            verticalGap = verticalGap,
            rowWeights = rowWeights,
            paddingStart = padding,
            paddingTop = padding,
            paddingEnd = padding,
            paddingBottom = padding,
        )
    }

    /**
     * Creates a Grid based helper that lays out its elements in a single Column.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val weights = intArrayOf(3, 3, 2, 2, 1)
     *      val g1 = createColumn(
     *          a, b, c, d, e,
     *          skips = arrayOf(Skip(1, 1), Skip(3, 2)),
     *          spans = arrayOf(Span(1, 2)),
     *          verticalGap = 10.dp,
     *          rowWeights = weights,
     *          padding = 10.dp,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param skips specify area(s) in a Column to be skipped - format: Skip(index, size)
     * @param spans specify area(s) in a Column to be spanned - format: Span(index, size)
     * @param verticalGap defines the gap between views in the y axis
     * @param rowWeights defines the weight of each row
     * @param paddingHorizontal sets paddingStart and paddingEnd of the content
     * @param paddingVertical sets paddingTop and paddingBottom of the content
     */
    fun createColumn(
        vararg elements: LayoutReference,
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        verticalGap: Dp = 0.dp,
        rowWeights: IntArray = intArrayOf(),
        paddingHorizontal: Dp = 0.dp,
        paddingVertical: Dp = 0.dp,
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            columns = 1,
            skips = skips,
            spans = spans,
            verticalGap = verticalGap,
            rowWeights = rowWeights,
            paddingStart = paddingHorizontal,
            paddingTop = paddingVertical,
            paddingEnd = paddingHorizontal,
            paddingBottom = paddingVertical,
        )
    }

    /**
     * Creates a Grid representation with a Grid Helper.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val f = createRefFor("6")
     *      val g = createRefFor("7")
     *      val h = createRefFor("8")
     *      val i = createRefFor("9")
     *      val j = createRefFor("0")
     *      val k = createRefFor("box")
     *      val weights = intArrayOf(3, 3, 2, 2)
     *      val flags = arrayOf("SubGridByColRow", "SpansRespectWidgetOrder")
     *      val g1 = createGrid(
     *          k, a, b, c, d, e, f, g, h, i, j, k,
     *          rows = 5,
     *          columns = 3,
     *          verticalGap = 25.dp,
     *          horizontalGap = 25.dp,
     *          skips = arrayOf(Skip(12, 1, 1)),
     *          spans = arrayOf(Span(0, 1, 3)),
     *          rowWeights = weights,
     *          paddingHorizontal = 10.dp,
     *          paddingVertical = 10.dp,
     *          flags = flags,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *      }
     *      Box(
     *          modifier = Modifier.background(Color.Gray).layoutId("box"),
     *          Alignment.BottomEnd
     *       ) {
     *          Text("100", fontSize = 80.sp)
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param orientation 0 if horizontal and 1 if vertical
     * @param rows sets the number of rows in Grid
     * @param columns sets the number of columns in Grid
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param rowWeights defines the weight of each row
     * @param columnWeights defines the weight of each column
     * @param skips defines the positions in a Grid to be skipped
     *        the format: Skip(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to skip
     *        coloumns - the number of columns to skip
     * @param spans defines the spanned area(s) in Grid
     *        the format: Span(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to span
     *        coloumns - the number of columns to span
     * @param padding sets padding around the content
     * @param flags set different flags to be enabled (not case-sensitive), including
     *          SubGridByColRow: reverse the width and height specification for spans/skips.
     *              Original - Position:HeightxWidth; with the flag - Position:WidthxHeight
     *          SpansRespectWidgetOrder: spans would respect the order of the widgets.
     *              Original - the widgets in the front of the widget list would be
     *              assigned to the spanned area; with the flag - all the widges will be arranged
     *              based on the given order. For example, for a layout with 1 row and 3 columns.
     *              If we have two widgets: w1, w2 with a span as 1:1x2, the original layout would
     *              be [w2 w1 w1]. Since w1 is in the front of the list, it would be assigned to
     *              the spanned area. With the flag, the layout would be [w1 w2 w2] that respects
     *              the order of the widget list.
     */
    fun createGrid(
        vararg elements: LayoutReference,
        orientation: Int = 0,
        rows: Int = 0,
        columns: Int = 0,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        rowWeights: IntArray = intArrayOf(),
        columnWeights: IntArray = intArrayOf(),
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        padding: Dp = 0.dp,
        flags: Array<GridFlag> = arrayOf(),
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            orientation = orientation,
            rows = rows,
            columns = columns,
            horizontalGap = horizontalGap,
            verticalGap = verticalGap,
            rowWeights = rowWeights,
            columnWeights = columnWeights,
            skips = skips,
            spans = spans,
            paddingStart = padding,
            paddingTop = padding,
            paddingEnd = padding,
            paddingBottom = padding,
            flags = flags,
        )
    }

    /**
     * Creates a Grid representation with a Grid Helper.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val f = createRefFor("6")
     *      val g = createRefFor("7")
     *      val h = createRefFor("8")
     *      val i = createRefFor("9")
     *      val j = createRefFor("0")
     *      val k = createRefFor("box")
     *      val weights = intArrayOf(3, 3, 2, 2)
     *      val flags = arrayOf("SubGridByColRow", "SpansRespectWidgetOrder")
     *      val g1 = createGrid(
     *          k, a, b, c, d, e, f, g, h, i, j, k,
     *          rows = 5,
     *          columns = 3,
     *          verticalGap = 25.dp,
     *          horizontalGap = 25.dp,
     *          skips = arrayOf(Skip(12, 1, 1)),
     *          spans = arrayOf(Span(0, 1, 3)),
     *          rowWeights = weights,
     *          paddingHorizontal = 10.dp,
     *          paddingVertical = 10.dp,
     *          flags = flags,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *      }
     *      Box(
     *          modifier = Modifier.background(Color.Gray).layoutId("box"),
     *          Alignment.BottomEnd
     *       ) {
     *          Text("100", fontSize = 80.sp)
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param rowWeights defines the weight of each row
     * @param rows sets the number of rows in Grid
     * @param columns sets the number of columns in Grid
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param columnWeights defines the weight of each column
     * @param orientation 0 if horizontal and 1 if vertical
     * @param skips defines the positions in a Grid to be skipped
     *        the format: Skip(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to skip
     *        coloumns - the number of columns to skip
     * @param spans defines the spanned area(s) in Grid
     *        the format: Span(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to span
     *        coloumns - the number of columns to span
     * @param paddingHorizontal sets paddingStart and paddingEnd of the content
     * @param paddingVertical sets paddingTop and paddingBottom of the content
     * @param flags set different flags to be enabled (not case-sensitive), including
     *          SubGridByColRow: reverse the width and height specification for spans/skips.
     *              Original - Position:HeightxWidth; with the flag - Position:WidthxHeight
     *          SpansRespectWidgetOrder: spans would respect the order of the widgets.
     *              Original - the widgets in the front of the widget list would be
     *              assigned to the spanned area; with the flag - all the widges will be arranged
     *              based on the given order. For example, for a layout with 1 row and 3 columns.
     *              If we have two widgets: w1, w2 with a span as 1:1x2, the original layout would
     *              be [w2 w1 w1]. Since w1 is in the front of the list, it would be assigned to
     *              the spanned area. With the flag, the layout would be [w1 w2 w2] that respects
     *              the order of the widget list.
     */
    fun createGrid(
        vararg elements: LayoutReference,
        orientation: Int = 0,
        rows: Int = 0,
        columns: Int = 0,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        rowWeights: IntArray = intArrayOf(),
        columnWeights: IntArray = intArrayOf(),
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        paddingHorizontal: Dp = 0.dp,
        paddingVertical: Dp = 0.dp,
        flags: Array<GridFlag> = arrayOf(),
    ): ConstrainedLayoutReference {
        return createGrid(
            elements = elements,
            rowWeights = rowWeights,
            columnWeights = columnWeights,
            orientation = orientation,
            rows = rows,
            columns = columns,
            horizontalGap = horizontalGap,
            verticalGap = verticalGap,
            skips = skips,
            spans = spans,
            paddingStart = paddingHorizontal,
            paddingTop = paddingVertical,
            paddingEnd = paddingHorizontal,
            paddingBottom = paddingVertical,
            flags = flags
        )
    }

    /**
     * Creates a Grid representation with a Grid Helper.
     * Example:
     * ConstraintLayout(
     *  ConstraintSet {
     *      val a = createRefFor("1")
     *      val b = createRefFor("2")
     *      val c = createRefFor("3")
     *      val d = createRefFor("4")
     *      val e = createRefFor("5")
     *      val f = createRefFor("6")
     *      val g = createRefFor("7")
     *      val h = createRefFor("8")
     *      val i = createRefFor("9")
     *      val j = createRefFor("0")
     *      val k = createRefFor("box")
     *      val weights = intArrayOf(3, 3, 2, 2)
     *      val flags = arrayOf("SubGridByColRow", "SpansRespectWidgetOrder")
     *      val g1 = createGrid(
     *          k, a, b, c, d, e, f, g, h, i, j, k,
     *          rows = 5,
     *          columns = 3,
     *          verticalGap = 25.dp,
     *          horizontalGap = 25.dp,
     *          skips = arrayOf(Skip(12, 1, 1)),
     *          spans = arrayOf(Span(0, 1, 3)),
     *          rowWeights = weights,
     *          paddingStart = 10.dp,
     *          paddingTop = 10.dp,
     *          paddingEnd = 10.dp,
     *          paddingBottom = 10.dp,
     *          flags = flags,
     *      )
     *      constrain(g1) {
     *          width = Dimension.matchParent
     *          height = Dimension.matchParent
     *      },
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      val numArray = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
     *      for (num in numArray) {
     *          Button(
     *              modifier = Modifier.layoutId(num).width(120.dp),
     *              onClick = {},
     *          ) {
     *              Text(text = String.format("btn%s", num))
     *          }
     *      }
     *      Box(
     *          modifier = Modifier.background(Color.Gray).layoutId("box"),
     *          Alignment.BottomEnd
     *       ) {
     *          Text("100", fontSize = 80.sp)
     *       }
     *    }
     *
     * @param elements [LayoutReference]s to be laid out by the Grid helper
     * @param orientation 0 if horizontal and 1 if vertical
     * @param rows sets the number of rows in Grid
     * @param columns sets the number of columns in Grid
     * @param verticalGap defines the gap between views in the y axis
     * @param horizontalGap defines the gap between views in the x axis
     * @param rowWeights defines the weight of each row
     * @param columnWeights defines the weight of each column
     * @param skips defines the positions in a Grid to be skipped
     *        the format: Skip(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to skip
     *        coloumns - the number of columns to skip
     * @param spans defines the spanned area(s) in Grid
     *        the format: Span(position, rows, columns)
     *        position - the index of the starting position
     *        rows - the number of rows to span
     *        coloumns - the number of columns to span
     * @param paddingStart sets paddingStart of the content
     * @param paddingTop sets paddingTop of the content
     * @param paddingEnd sets paddingEnd of the content
     * @param paddingBottom sets paddingBottom of the content
     * @param flags set different flags to be enabled (not case-sensitive), including
     *          SubGridByColRow: reverse the width and height specification for spans/skips.
     *              Original - Position:HeightxWidth; with the flag - Position:WidthxHeight
     *          SpansRespectWidgetOrder: spans would respect the order of the widgets.
     *              Original - the widgets in the front of the widget list would be
     *              assigned to the spanned area; with the flag - all the widges will be arranged
     *              based on the given order. For example, for a layout with 1 row and 3 columns.
     *              If we have two widgets: w1, w2 with a span as 1:1x2, the original layout would
     *              be [w2 w1 w1]. Since w1 is in the front of the list, it would be assigned to
     *              the spanned area. With the flag, the layout would be [w1 w2 w2] that respects
     *              the order of the widget list.
     */
    fun createGrid(
        vararg elements: LayoutReference,
        orientation: Int = 0,
        rows: Int = 0,
        columns: Int = 0,
        verticalGap: Dp = 0.dp,
        horizontalGap: Dp = 0.dp,
        rowWeights: IntArray = intArrayOf(),
        columnWeights: IntArray = intArrayOf(),
        skips: Array<Skip> = arrayOf(),
        spans: Array<Span> = arrayOf(),
        paddingStart: Dp = 0.dp,
        paddingTop: Dp = 0.dp,
        paddingEnd: Dp = 0.dp,
        paddingBottom: Dp = 0.dp,
        flags: Array<GridFlag> = arrayOf(),
    ): ConstrainedLayoutReference {
        val ref = ConstrainedLayoutReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        val flagArray = CLArray(charArrayOf())
        elements.forEach {
            elementArray.add(CLString.from(it.id.toString()))
        }
        val paddingArray = CLArray(charArrayOf()).apply {
            add(CLNumber(paddingStart.value))
            add(CLNumber(paddingTop.value))
            add(CLNumber(paddingEnd.value))
            add(CLNumber(paddingBottom.value))
        }
        flags.forEach {
            flagArray.add(CLString.from(it.name))
        }
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
            putNumber("orientation", orientation.toFloat())
            putNumber("rows", rows.toFloat())
            putNumber("columns", columns.toFloat())
            putNumber("vGap", verticalGap.value)
            putNumber("hGap", horizontalGap.value)
            put("padding", paddingArray)
            putString("rowWeights", strRowWeights)
            putString("columnWeights", strColumnWeights)
            putString("skips", strSkips)
            putString("spans", strSpans)
            put("flags", flagArray)
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
            val elementContent: CLElement = if (chainParams != null) {
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
     * Use [constrain] with the resulting [VerticalChainReference] to modify the top and
     * bottom constraints of this chain.
     */
    fun createVerticalChain(
        vararg elements: LayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): VerticalChainReference {
        val ref = VerticalChainReference(createHelperId())
        val elementArray = CLArray(charArrayOf())
        elements.forEach {
            val chainParams = it.getHelperParams<ChainParams>()
            val elementContent: CLElement = if (chainParams != null) {
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
     * Use weight to distribute available space to each widget when their dimensions are not
     * fixed.
     *
     * &nbsp;
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in
     * Chains.
     *
     * Since margins are only for widgets within the chain: Top, Start and End, Bottom margins are
     * ignored when the widget is the first or the last element in the chain, respectively.
     *
     * @param startMargin Added space from the start of this widget to the previous widget
     * @param topMargin Added space from the top of this widget to the previous widget
     * @param endMargin Added space from the end of this widget to the next widget
     * @param bottomMargin Added space from the bottom of this widget to the next widget
     * @param startGoneMargin Added space from the start of this widget when the previous widget has [Visibility.Gone]
     * @param topGoneMargin Added space from the top of this widget when the previous widget has [Visibility.Gone]
     * @param endGoneMargin Added space from the end of this widget when the next widget has [Visibility.Gone]
     * @param bottomGoneMargin Added space from the bottom of this widget when the next widget has [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     * layout when the corresponding dimension is not a fixed value.
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
     * &nbsp;
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in
     * Chains.
     *
     * Since margins are only for widgets within the chain: Start and End margins are
     * ignored when the widget is the first or the last element in the chain, respectively.
     *
     * @param startMargin Added space from the start of this widget to the previous widget
     * @param endMargin Added space from the end of this widget to the next widget
     * @param startGoneMargin Added space from the start of this widget when the previous widget has [Visibility.Gone]
     * @param endGoneMargin Added space from the end of this widget when the next widget has [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     * layout when the width is not a fixed dimension.
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
     * &nbsp;
     *
     * Similarly named parameters available from [ConstrainScope.linkTo] are ignored in
     * Chains.
     *
     * Since margins are only for widgets within the chain: Top and Bottom margins are
     * ignored when the widget is the first or the last element in the chain, respectively.
     *
     * @param topMargin Added space from the top of this widget to the previous widget
     * @param bottomMargin Added space from the bottom of this widget to the next widget
     * @param topGoneMargin Added space from the top of this widget when the previous widget has [Visibility.Gone]
     * @param bottomGoneMargin Added space from the bottom of this widget when the next widget has [Visibility.Gone]
     * @param weight Defines the proportion of space (relative to the total weight) occupied by this
     * layout when the height is not a fixed dimension.
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
        if (javaClass != other?.javaClass) return false

        other as LayoutReference

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    internal fun setHelperParams(helperParams: HelperParams) {
        // Use the class name to force one instance per implementation
        helperParamsMap[helperParams.javaClass.simpleName] = helperParams
    }

    /**
     * Returns the [HelperParams] that corresponds to the class type [T]. Null if no instance of
     * type [T] has been set.
     */
    internal inline fun <reified T> getHelperParams(): T? where T : HelperParams {
        return helperParamsMap[T::class.java.simpleName] as? T
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
        internal val Default = ChainParams(
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
    internal val name: String,
    internal val bias: Float? = null
) {
    companion object {
        /**
         * A chain style that evenly distributes the contained layouts.
         */
        @Stable
        val Spread = ChainStyle("spread")

        /**
         * A chain style where the first and last layouts are affixed to the constraints
         * on each end of the chain and the rest are evenly distributed.
         */
        @Stable
        val SpreadInside = ChainStyle("spread_inside")

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
        fun Packed(bias: Float) = ChainStyle("packed", bias)
    }
}

/**
 * The overall visibility of a widget in a [ConstraintLayout].
 */
@Immutable
class Visibility internal constructor(
    internal val name: String
) {
    companion object {
        /**
         * Indicates that the widget will be painted in the [ConstraintLayout]. All render-time
         * transforms will apply normally.
         */
        @Stable
        val Visible = Visibility("visible")

        /**
         * The widget will not be painted in the [ConstraintLayout] but its dimensions and constraints
         * will still apply.
         *
         * Equivalent to forcing the alpha to 0.0.
         */
        @Stable
        val Invisible = Visibility("invisible")

        /**
         * Like [Invisible], but the dimensions of the widget will collapse to (0,0), the
         * constraints will still apply.
         */
        @Stable
        val Gone = Visibility("gone")
    }
}

/**
 * GridFlag defines the available flags of Grid
 * SubGridByColRow: reverse the width and height specification for spans/skips.
 *   Original - Position:HeightxWidth; with the flag - Position:WidthxHeight
 * SpansRespectWidgetOrder: spans would respect the order of the widgets.
 *   Original - the widgets in the front of the widget list would be
 *              assigned to the spanned area; with the flag - all the widges will be arranged
 *              based on the given order. For example, for a layout with 1 row and 3 columns.
 *              If we have two widgets: w1, w2 with a span as 1:1x2, the original layout would
 *              be [w2 w1 w1]. Since w1 is in the front of the list, it would be assigned to
 *              the spanned area. With the flag, the layout would be [w1 w2 w2] that respects
 *              the order of the widget list.
 */
@Immutable
class GridFlag internal constructor(
    internal val name: String
) {
    companion object {
        val SpansRespectWidgetOrder = GridFlag("spansrespectwidgetorder")
        val SubGridByColRow = GridFlag("subgridbycolrow")
    }
}

/**
 * Wrap defines the type of chain
 */
@Immutable
class Wrap internal constructor(
    internal val name: String
) {
    companion object {
        val None = Wrap("none")
        val Chain = Wrap("chain")
        val Aligned = Wrap("aligned")
    }
}

/**
 * Defines how objects align vertically within the chain
 */
@Immutable
class VerticalAlign internal constructor(
    internal val name: String
) {
    companion object {
        val Top = VerticalAlign("top")
        val Bottom = VerticalAlign("bottom")
        val Center = VerticalAlign("center")
        val Baseline = VerticalAlign("baseline")
    }
}

/**
 * Defines how objects align horizontally in the chain
 */
@Immutable
class HorizontalAlign internal constructor(
    internal val name: String
) {
    companion object {
        val Start = HorizontalAlign("start")
        val End = HorizontalAlign("end")
        val Center = HorizontalAlign("center")
    }
}

/**
 * Defines how widgets are spaced in a chain
 */
@Immutable
class FlowStyle internal constructor(
    internal val name: String
) {
    companion object {
        val Spread = FlowStyle("spread")
        val SpreadInside = FlowStyle("spread_inside")
        val Packed = FlowStyle("packed")
    }
}

@JvmInline
value class Skip(val description: String) {
    constructor(position: Int, rows: Int, columns: Int) : this("$position:${rows}x$columns")
    constructor(position: Int, size: Int) : this("$position:$size")
}

@JvmInline
value class Span(val description: String) {
    constructor(position: Int, rows: Int, columns: Int) : this("$position:${rows}x$columns")
    constructor(position: Int, size: Int) : this("$position:$size")
}