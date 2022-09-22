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

import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.AnchorFunctions.verticalAnchorFunctions
import androidx.constraintlayout.core.state.ConstraintReference

/**
 * Represents a vertical side of a layout (i.e start and end) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
interface VerticalAnchorable {
    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.VerticalAnchor].
     */
    fun linkTo(
        anchor: ConstraintLayoutBaseScope.VerticalAnchor,
        margin: Dp = 0.dp,
        goneMargin: Dp = 0.dp
    )
}

/**
 * Represents a horizontal side of a layout (i.e top and bottom) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
interface HorizontalAnchorable {
    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.HorizontalAnchor].
     */
    fun linkTo(
        anchor: ConstraintLayoutBaseScope.HorizontalAnchor,
        margin: Dp = 0.dp,
        goneMargin: Dp = 0.dp
    )
}

/**
 * Represents the [FirstBaseline] of a layout that can be anchored
 * using [linkTo] in their `Modifier.constrainAs` blocks.
 */
interface BaselineAnchorable {
    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.BaselineAnchor].
     */
    fun linkTo(
        anchor: ConstraintLayoutBaseScope.BaselineAnchor,
        margin: Dp = 0.dp,
        goneMargin: Dp = 0.dp
    )
}

internal abstract class BaseVerticalAnchorable(
    private val tasks: MutableList<(State) -> Unit>,
    private val index: Int
) : VerticalAnchorable {
    abstract fun getConstraintReference(state: State): ConstraintReference

    final override fun linkTo(
        anchor: ConstraintLayoutBaseScope.VerticalAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        tasks.add { state ->
            val layoutDirection = state.layoutDirection
            val index1 =
                AnchorFunctions.verticalAnchorIndexToFunctionIndex(index, layoutDirection)
            val index2 = AnchorFunctions.verticalAnchorIndexToFunctionIndex(
                anchor.index,
                layoutDirection
            )
            with(getConstraintReference(state)) {
                verticalAnchorFunctions[index1][index2]
                    .invoke(this, anchor.id, state.layoutDirection)
                    .margin(margin)
                    .marginGone(goneMargin)
            }
        }
    }
}

internal abstract class BaseHorizontalAnchorable(
    private val tasks: MutableList<(State) -> Unit>,
    private val index: Int
) : HorizontalAnchorable {
    abstract fun getConstraintReference(state: State): ConstraintReference

    final override fun linkTo(
        anchor: ConstraintLayoutBaseScope.HorizontalAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        tasks.add { state ->
            with(getConstraintReference(state)) {
                AnchorFunctions.horizontalAnchorFunctions[index][anchor.index]
                    .invoke(this, anchor.id)
                    .margin(margin)
                    .marginGone(goneMargin)
            }
        }
    }
}

internal object AnchorFunctions {
    val verticalAnchorFunctions:
        Array<Array<ConstraintReference.(Any, LayoutDirection) -> ConstraintReference>> =
        arrayOf(
            arrayOf(
                { other, layoutDirection ->
                    clearLeft(layoutDirection); leftToLeft(other)
                },
                { other, layoutDirection ->
                    clearLeft(layoutDirection); leftToRight(other)
                }
            ),
            arrayOf(
                { other, layoutDirection ->
                    clearRight(layoutDirection); rightToLeft(other)
                },
                { other, layoutDirection ->
                    clearRight(layoutDirection); rightToRight(other)
                }
            )
        )

    private fun ConstraintReference.clearLeft(layoutDirection: LayoutDirection) {
        leftToLeft(null)
        leftToRight(null)
        when (layoutDirection) {
            LayoutDirection.Ltr -> {
                startToStart(null); startToEnd(null)
            }
            LayoutDirection.Rtl -> {
                endToStart(null); endToEnd(null)
            }
        }
    }

    private fun ConstraintReference.clearRight(layoutDirection: LayoutDirection) {
        rightToLeft(null)
        rightToRight(null)
        when (layoutDirection) {
            LayoutDirection.Ltr -> {
                endToStart(null); endToEnd(null)
            }
            LayoutDirection.Rtl -> {
                startToStart(null); startToEnd(null)
            }
        }
    }

    /**
     * Converts the index (-2 -> start, -1 -> end, 0 -> left, 1 -> right) to an index in
     * the arrays above (0 -> left, 1 -> right).
     */
    // TODO(popam, b/157886946): this is temporary until we can use CL's own RTL handling
    fun verticalAnchorIndexToFunctionIndex(index: Int, layoutDirection: LayoutDirection) =
        when {
            index >= 0 -> index // already left or right
            layoutDirection == LayoutDirection.Ltr -> 2 + index // start -> left, end -> right
            else -> -index - 1 // start -> right, end -> left
        }

    val horizontalAnchorFunctions:
        Array<Array<ConstraintReference.(Any) -> ConstraintReference>> = arrayOf(
        arrayOf(
            { other -> topToBottom(null); baselineToBaseline(null); topToTop(other) },
            { other -> topToTop(null); baselineToBaseline(null); topToBottom(other) }
        ),
        arrayOf(
            { other -> bottomToBottom(null); baselineToBaseline(null); bottomToTop(other) },
            { other -> bottomToTop(null); baselineToBaseline(null); bottomToBottom(other) }
        )
    )
    val baselineAnchorFunction: ConstraintReference.(Any) -> ConstraintReference =
        { other ->
            topToTop(null)
            topToBottom(null)
            bottomToTop(null)
            bottomToBottom(null)
            baselineToBaseline(other)
        }
}