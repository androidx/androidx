/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Components are grouped into shape categories based on common features. These categories provide a
 * way to change multiple component values at once, by changing the category’s values.
 *
 */
@Immutable
public class Shapes(
    /**
     * Buttons and Chips use this shape
     */
    val small: CornerBasedShape = RoundedCornerShape(corner = CornerSize(50)),

    val medium: CornerBasedShape = RoundedCornerShape(4.dp),
    /**
     * Cards use this shape
     */
    val large: CornerBasedShape = RoundedCornerShape(24.dp),
) {

    /**
     * Returns a copy of this Shapes, optionally overriding some of the values.
     */
    public fun copy(
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
    ): Shapes = Shapes(
        small = small,
        medium = medium,
        large = large,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false

        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false

        return true
    }

    override fun hashCode(): Int {
        var result = small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shapes(small=$small, medium=$medium, large=$large)"
    }
}

/**
 * CompositionLocal used to specify the default shapes for the surfaces.
 */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
