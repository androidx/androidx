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

import android.util.Log
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString

@JvmDefaultWithCompatibility
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

@JvmDefaultWithCompatibility
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

@JvmDefaultWithCompatibility
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
    private val containerObject: CLObject,
    index: Int
) : VerticalAnchorable {
    private val anchorName: String = AnchorFunctions.verticalAnchorIndexToAnchorName(index)

    final override fun linkTo(
        anchor: ConstraintLayoutBaseScope.VerticalAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        val targetAnchorName = AnchorFunctions.verticalAnchorIndexToAnchorName(anchor.index)
        val constraintArray = CLArray(charArrayOf()).apply {
            add(CLString.from(anchor.id.toString()))
            add(CLString.from(targetAnchorName))
            add(CLNumber(margin.value))
            add(CLNumber(goneMargin.value))
        }
        containerObject.put(anchorName, constraintArray)
    }
}

internal abstract class BaseHorizontalAnchorable(
    private val containerObject: CLObject,
    index: Int
) : HorizontalAnchorable {
    private val anchorName: String = AnchorFunctions.horizontalAnchorIndexToAnchorName(index)

    final override fun linkTo(
        anchor: ConstraintLayoutBaseScope.HorizontalAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        val targetAnchorName = AnchorFunctions.horizontalAnchorIndexToAnchorName(anchor.index)
        val constraintArray = CLArray(charArrayOf()).apply {
            add(CLString.from(anchor.id.toString()))
            add(CLString.from(targetAnchorName))
            add(CLNumber(margin.value))
            add(CLNumber(goneMargin.value))
        }
        containerObject.put(anchorName, constraintArray)
    }
}

internal object AnchorFunctions {

    fun horizontalAnchorIndexToAnchorName(index: Int): String =
        when (index) {
            0 -> "top"
            1 -> "bottom"
            else -> {
                Log.e("CCL", "horizontalAnchorIndexToAnchorName: Unknown horizontal index")
                "top"
            }
        }

    fun verticalAnchorIndexToAnchorName(index: Int): String =
        when (index) {
            -2 -> "start"
            -1 -> "end"
            0 -> "left"
            1 -> "right"
            else -> {
                Log.e("CCL", "verticalAnchorIndexToAnchorName: Unknown vertical index")
                "start"
            }
        }
}