/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical

/**
 * Defines the visual style of emphasis marks used with [EmphasisSpan].
 *
 * Each style corresponds to a specific Unicode character pair (filled and open variants).
 */
public sealed class EmphasisStyle
protected constructor(
    /** The integer representation of this style, used for serialization. */
    @JvmField public val value: Int
) {

    public companion object {
        /** Emphasis mark is a small circle. The filled dot is U+2022, open dot is U+25E6. */
        @JvmField public val Dot: EmphasisStyle = DotImpl
        /** Emphasis mark is a large circle. The filled dot is U+25CF, open dot is U+25CB. */
        @JvmField public val Circle: EmphasisStyle = CircleImpl
        /** Emphasis mark is a double circle. The filled dot is U+25C9, open dot is U+25CE. */
        @JvmField public val DoubleCircle: EmphasisStyle = DoubleCircleImpl
        /** Emphasis mark is a triangle. The filled dot is U+25B2, open dot is U+25B3. */
        @JvmField public val Triangle: EmphasisStyle = TriangleImpl
        /** Emphasis mark is a sesame. The filled dot is U+FE45, open dot is U+FE46. */
        @JvmField public val Sesame: EmphasisStyle = SesameImpl

        @JvmStatic
        internal fun fromInt(value: Int): EmphasisStyle =
            when (value) {
                Dot.value -> Dot
                Circle.value -> Circle
                DoubleCircle.value -> DoubleCircle
                Triangle.value -> Triangle
                Sesame.value -> Sesame
                else -> Dot
            }
    }

    // Prevents exhaustive `when` usage for Kotlin consumers, making it safe
    // to add new types in the future (go/android-api-guidelines#classes-sealed)
    private object Hidden : EmphasisStyle(Int.MAX_VALUE)
}

private object DotImpl : EmphasisStyle(1)

private object CircleImpl : EmphasisStyle(2)

private object DoubleCircleImpl : EmphasisStyle(3)

private object TriangleImpl : EmphasisStyle(4)

private object SesameImpl : EmphasisStyle(5)
