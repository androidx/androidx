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
import androidx.compose.ui.layout.Measurable
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.state.ConstraintSetParser
import androidx.constraintlayout.core.state.ConstraintSetParser.LayoutVariables
import androidx.constraintlayout.core.state.Transition

@JvmDefaultWithCompatibility
/**
 * Immutable description of the constraints used to layout the children of a [ConstraintLayout].
 *
 * Layout items are defined with [ConstraintSetScope.createRefFor], each layout item must be defined
 * with a unique ID from other items in the same scope:
 *
 * ```
 * val textRef = createRefFor("text")
 * val imageRef = createRefFor("image")
 * ```
 *
 * You may also use [ConstraintSetScope.createRefsFor] to declare up to 16 items at a time using the
 * destructuring declaration pattern:
 *
 * ```
 * val (textRef, imageRef) = createRefsFor("text", "image")
 * ```
 *
 * Individual constraints are defined with [ConstraintSetScope.constrain]. Where you can tell each
 * layout reference how to constrain to other references including the **`parent`**:
 *
 * ```
 * constrain(textRef) {
 *     centerTo(parent)
 * }
 * constrain(imageRef) {
 *     centerVerticallyTo(textRef)
 *     start.linkTo(textRef.end, margin = 8.dp)
 * }
 * ```
 *
 * Here, we constrain the *textRef* to the center of the *parent*, while the image is centered
 * vertically to *textRef* and is horizontally placed to the right of its end anchor with a
 * margin (keep in mind, when using `center...`, `start` or `end` the layout direction will
 * automatically change in RTL locales).
 *
 * See [ConstrainScope] to learn more about how to constrain elements together.
 *
 * &nbsp;
 *
 * In the ConstraintLayout or MotionLayout Composables, the children must be bound using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 *
 *
 * So, the whole snippet with ConstraintLayout would look like this:
 *
 *
 * ```
 * val textId = "text"
 * val imageId = "image"
 * ConstraintLayout(
 *     constraintSet = ConstraintSet {
 *         val (textRef, imageRef) = createRefsFor(textId, imageId)
 *         constrain(textRef) {
 *             centerTo(parent)
 *         }
 *         constrain(imageRef) {
 *             centerVerticallyTo(textRef)
 *             start.linkTo(textRef.end, margin = 8.dp)
 *         }
 *     },
 *     modifier = Modifier.fillMaxSize()
 * ) {
 *     Text(
 *         modifier = Modifier.layoutId(textId),
 *         text = "Hello, World!"
 *     )
 *     Image(
 *         modifier = Modifier.layoutId(imageId),
 *         imageVector = Icons.Default.Android,
 *         contentDescription = null
 *     )
 * }
 * ```
 *
 * ## Helpers
 * You may also use helpers, a set of virtual (not shown on screen) components that provide special
 * layout behaviors, you may find these in the [ConstraintSetScope] with the '`create...`' prefix,
 * a few of these are **Guidelines**, **Chains** and **Barriers**.
 *
 * &nbsp;
 *
 * ### Guidelines
 * Lines to which other [ConstrainedLayoutReference]s may be constrained to, these are defined at
 * either a fixed or percent position from an anchor of the ConstraintLayout parent (top, bottom,
 * start, end, absoluteLeft, absoluteRight).
 *
 * &nbsp;
 *
 * Example:
 * ```
 * val (textRef) = createRefsFor(textId)
 * val vG = createGuidelineFromStart(fraction = 0.3f)
 *
 * constrain(textRef) {
 *     centerVerticallyTo(parent)
 *     centerAround(vG)
 * }
 * ```
 *
 * See
 * - [ConstraintSetScope.createGuidelineFromTop]
 * - [ConstraintSetScope.createGuidelineFromBottom]
 * - [ConstraintSetScope.createGuidelineFromStart]
 * - [ConstraintSetScope.createGuidelineFromEnd]
 * - [ConstraintSetScope.createGuidelineFromAbsoluteLeft]
 * - [ConstraintSetScope.createGuidelineFromAbsoluteRight]
 *
 * ### Chains
 * Chains may be either horizontal or vertical, these, take a set of [ConstrainedLayoutReference]s
 * and create bi-directional constraints on each of them at the same orientation of the chain in the
 * given order, meaning that an horizontal chain will create constraints between the start and end anchors.
 *
 * The result, a layout that evenly distributes the space within its elements.
 *
 * &nbsp;
 *
 * For example, to make a layout with three text elements distributed so that the spacing between
 * them (and around them) is equal:
 * ```
 * val (textRef0, textRef1, textRef2) = createRefsFor("text0", "text1", "text2")
 * createHorizontalChain(textRef0, textRef1, textRef2, chainStyle = ChainStyle.Spread)
 * ```
 *
 * You may set margins within elements in a chain with [ConstraintLayoutScope.withChainParams]:
 *
 * ```
 * val (textRef0, textRef1, textRef2) = createRefsFor("text0", "text1", "text2")
 * createHorizontalChain(
 *     textRef0,
 *     textRef1.withChainParams(startMargin = 100.dp, endMargin = 100.dp),
 *     textRef2,
 *     chainStyle = ChainStyle.Spread
 * )
 * ```
 *
 * You can also change the way space is distributed, as chains have three different styles:
 * - [ChainStyle.Spread]  Layouts are evenly distributed after margins are accounted for (the space
 * around and between each item is even). This is the **default** style for chains.
 * - [ChainStyle.SpreadInside]  The first and last layouts are affixed to each end of the chain,
 * and the rest of the items are evenly distributed (after margins are accounted for).
 * I.e.: Items are spread from the inside, distributing the space between them with no space around
 * the first and last items.
 * - [ChainStyle.Packed] The layouts are packed together after margins are accounted for, by
 * default, they're packed together at the middle, you can change this behavior with the **bias**
 * parameter of [ChainStyle.Packed].
 * - Alternatively, you can make every Layout in the chain to be [Dimension.fillToConstraints] and
 * then set a particular weight to each of them to create a **weighted chain**.
 *
 * #### Weighted Chain
 * Weighted chains are useful when you want the size of the elements to depend on the remaining size
 * of the chain. As opposed to just distributing the space around and/or in-between the items.
 *
 * &nbsp;
 *
 * For example, to create a layout with three text elements in a row where each element takes the
 * exact same size regardless of content, you can use a simple weighted chain where each item has the
 * same weight:
 *
 * ```
 * val (textRef0, textRef1, textRef2) = createRefsFor("text0", "text1", "text2")
 * createHorizontalChain(
 *     textRef0.withChainParams(weight = 1f),
 *     textRef1.withChainParams(weight = 1f),
 *     textRef2.withChainParams(weight = 1f),
 *     chainStyle = ChainStyle.Spread
 * )
 *
 * constrain(textRef0, textRef1, textRef2) {
 *     width = Dimension.fillToConstraints
 * }
 * ```
 *
 * This way, the texts will horizontally occupy the same space even if one of them is significantly
 * larger than the others.
 *
 * Also note that when using [ConstraintSetScope] you can apply the same constrains to multiple
 * references at a time.
 *
 * &nbsp;
 *
 * Keep in mind that chains have a relatively high performance cost. For example, if you plan on
 * having multiple chains one below the other, consider instead, applying just one chain and using
 * it as a reference to constrain all other elements to the ones that match their position in that
 * one chain. It may provide increased performance with no significant changes in the layout output.
 *
 * Alternatively, consider if other helpers such as [ConstraintSetScope.createGrid] can
 * accomplish the same layout.
 *
 * &nbsp;
 *
 * See
 * - [ConstraintSetScope.createHorizontalChain]
 * - [ConstraintSetScope.createVerticalChain]
 * - [ConstraintSetScope.withChainParams]
 *
 * ### Barriers
 * Barriers take a set of [ConstrainedLayoutReference]s and creates the most further point in a
 * given direction where other [ConstrainedLayoutReference] can constrain to.
 *
 * &nbsp;
 *
 * This is useful in situations where elements in a layout may have different sizes but you want to
 * always constrain to the largest item, for example, if you have a text element on top of another
 * and want an image to always be constrained to the end of them:
 *
 * ```
 * val (textRef0, textRef1, imageRef) = createRefsFor("text0", "text1", "image")
 *
 * // Creates a point at the furthest end anchor from the elements in the barrier
 * val endTextsBarrier = createEndBarrier(textRef0, textRef1)
 *
 * constrain(textRef0) {
 *     centerTo(parent)
 * }
 * constrain(textRef1) {
 *     top.linkTo(textRef0.bottom)
 *     start.linkTo(textRef0.start)
 * }
 *
 * constrain(imageRef) {
 *     top.linkTo(textRef0.top)
 *     bottom.linkTo(textRef1.bottom)
 *
 *     // Image will always be at the end of both texts, regardless of their size
 *     start.linkTo(endTextsBarrier, margin = 8.dp)
 * }
 * ```
 *
 * Be careful not to constrain a [ConstrainedLayoutReference] to a barrier that references it or
 * that depends on it indirectly. This creates a cyclic dependency that results in unsupported
 * layout behavior.
 *
 * See
 * - [ConstraintSetScope.createTopBarrier]
 * - [ConstraintSetScope.createBottomBarrier]
 * - [ConstraintSetScope.createStartBarrier]
 * - [ConstraintSetScope.createEndBarrier]
 * - [ConstraintSetScope.createAbsoluteLeftBarrier]
 * - [ConstraintSetScope.createAbsoluteRightBarrier]
 */
@Immutable
interface ConstraintSet {
    /**
     * Applies the [ConstraintSet] to a state.
     */
    fun applyTo(state: State, measurables: List<Measurable>)

    fun override(name: String, value: Float) = this
    fun applyTo(transition: Transition, type: Int) {
        // nothing here, used in MotionLayout
    }

    fun isDirty(measurables: List<Measurable>): Boolean = true
}

@JvmDefaultWithCompatibility
@Immutable
internal interface DerivedConstraintSet : ConstraintSet {
    /**
     * [ConstraintSet] that this instance will derive its constraints from.
     *
     * This means that the constraints from [extendFrom] will be applied before the constraints of
     * this [DerivedConstraintSet] instance.
     */
    val extendFrom: ConstraintSet?

    override fun applyTo(state: State, measurables: List<Measurable>) {
        extendFrom?.applyTo(state, measurables)
        applyToState(state)
    }

    /**
     * Inheritors should implement this function so that derived constraints are applied properly.
     */
    fun applyToState(state: State)
}

/**
 * [ConstraintSet] defined solely on the given [clObject]. Only meant to be used to extract a copy
 * of the underlying ConstraintSet of ConstraintLayout with the inline Modifier DSL.
 *
 * You likely don't mean to use this.
 */
@Immutable
@PublishedApi
internal class RawConstraintSet(private val clObject: CLObject) : ConstraintSet {
    private val layoutVariables = LayoutVariables()
    override fun applyTo(state: State, measurables: List<Measurable>) {
        ConstraintSetParser.populateState(
            clObject,
            state,
            layoutVariables
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawConstraintSet

        if (clObject != other.clObject) return false

        return true
    }

    override fun hashCode(): Int {
        return clObject.hashCode()
    }
}
