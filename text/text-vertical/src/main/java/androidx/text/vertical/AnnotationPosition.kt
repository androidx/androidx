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

/** Properties of a text annotation (i.e. ruby and emphasis marks). */
public sealed class AnnotationPosition protected constructor(@JvmField public val value: Int) {
    public companion object {
        /** The text annotation position is unknown. */
        @JvmField public val Unknown: AnnotationPosition = UnknownImpl
        /**
         * For horizontal text, the text annotation should be positioned above the base text.
         *
         * For vertical text, it should be positioned to the right. See the
         * [tts:rubyPosition](https://www.w3.org/TR/ttml2/#style-attribute-rubyPosition) attribute
         * in TTML2 for more information.
         */
        @JvmField public val Before: AnnotationPosition = BeforeImpl
        /**
         * For horizontal text, the text annotation should be positioned below the base text.
         *
         * For vertical text, it should be positioned to the left, See the
         * [tts:rubyPosition](https://www.w3.org/TR/ttml2/#style-attribute-rubyPosition) attribute
         * in TTML2 for more information.
         */
        @JvmField public val After: AnnotationPosition = AfterImpl

        @JvmStatic
        @JvmName("fromInt")
        public fun fromInt(value: Int): AnnotationPosition =
            when (value) {
                Before.value -> Before
                After.value -> After
                else -> Unknown
            }
    }

    // Prevents exhaustive `when` usage for Kotlin consumers, making it safe
    // to add new types in the future (go/android-api-guidelines#classes-sealed)
    private object Hidden : AnnotationPosition(Int.MAX_VALUE)
}

private object UnknownImpl : AnnotationPosition(-1)

private object BeforeImpl : AnnotationPosition(0)

private object AfterImpl : AnnotationPosition(1)
