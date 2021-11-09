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

package androidx.glance.layout

/**
 * A class used to specify the position of a sized box inside an available space. This is often used
 * to specify how a parent layout should place its children.
 */

public class Alignment(public val horizontal: Horizontal, public val vertical: Vertical) {
    /**
     * Specifies how a parent should lay its children out horizontally, if the child has a width
     * smaller than the parent.
     */
    @Suppress("INLINE_CLASS_DEPRECATED")
    public inline class Horizontal private constructor (private val value: Int) {
        public companion object {
            public val Start: Horizontal = Horizontal(0)
            public val CenterHorizontally: Horizontal = Horizontal(1)
            public val End: Horizontal = Horizontal(2)
        }
    }

    /**
     * Specifies how a parent should lay its children out vertically, if the child has a height
     * smaller than the parent.
     */
    @Suppress("INLINE_CLASS_DEPRECATED")
    public inline class Vertical private constructor (private val value: Int) {
        public companion object {
            public val Top: Vertical = Vertical(0)
            public val CenterVertically: Vertical = Vertical(1)
            public val Bottom: Vertical = Vertical(2)
        }
    }

    /** Common [Alignment] options used in layouts. */
    public companion object {
        public val TopStart: Alignment = Alignment(Horizontal.Start, Vertical.Top)
        public val TopCenter: Alignment = Alignment(Horizontal.CenterHorizontally, Vertical.Top)
        public val TopEnd: Alignment = Alignment(Horizontal.End, Vertical.Top)

        public val CenterStart: Alignment = Alignment(Horizontal.Start, Vertical.CenterVertically)
        public val Center: Alignment =
            Alignment(Horizontal.CenterHorizontally, Vertical.CenterVertically)
        public val CenterEnd: Alignment = Alignment(Horizontal.End, Vertical.CenterVertically)

        public val BottomStart: Alignment = Alignment(Horizontal.Start, Vertical.Bottom)
        public val BottomCenter: Alignment =
            Alignment(Horizontal.CenterHorizontally, Vertical.Bottom)
        public val BottomEnd: Alignment = Alignment(Horizontal.End, Vertical.Bottom)

        public val Top: Vertical = Vertical.Top
        public val CenterVertically: Vertical = Vertical.CenterVertically
        public val Bottom: Vertical = Vertical.Bottom

        public val Start: Horizontal = Horizontal.Start
        public val CenterHorizontally: Horizontal = Horizontal.CenterHorizontally
        public val End: Horizontal = Horizontal.End
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Alignment

        if (horizontal != other.horizontal) return false
        if (vertical != other.vertical) return false

        return true
    }

    override fun hashCode(): Int {
        var result = horizontal.hashCode()
        result = 31 * result + vertical.hashCode()
        return result
    }

    override fun toString(): String {
        return "Alignment(horizontal=$horizontal, vertical=$vertical)"
    }
}
