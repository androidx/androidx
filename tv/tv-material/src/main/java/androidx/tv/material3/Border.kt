/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.tokens.ShapeTokens

/**
 * Defines the border for a TV component.
 * @param border configures the width and brush for the border
 * @param inset defines how far (in dp) should the border be from the component it's applied to
 * @param shape defines the [Shape] of the border
 */
@ExperimentalTvMaterial3Api
@Immutable
class Border(
    val border: BorderStroke,
    val inset: Dp = 0.dp,
    val shape: Shape = ShapeTokens.BorderDefaultShape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Border

        if (border != other.border) return false
        if (inset != other.inset) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = border.hashCode()
        result = 31 * result + inset.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun toString(): String {
        return "Border(border=$border, inset=$inset, shape=$shape)"
    }

    fun copy(
        border: BorderStroke? = null,
        inset: Dp? = null,
        shape: Shape? = null
    ): Border = Border(
        border = border ?: this.border,
        inset = inset ?: this.inset,
        shape = shape ?: this.shape
    )

    companion object {
        /**
         * Signifies the absence of a border. Use this if you do not want to display a border
         * indication in any of the TV Components.
         */
        val None = Border(
            border = BorderStroke(width = 0.dp, color = Color.Transparent),
            inset = 0.dp,
            shape = RectangleShape
        )
    }
}
