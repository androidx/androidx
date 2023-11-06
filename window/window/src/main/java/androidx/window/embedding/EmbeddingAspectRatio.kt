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

package androidx.window.embedding

import androidx.annotation.FloatRange

/**
 * The aspect ratio of the parent window bound to allow embedding with the rule.
 *
 * @see SplitRule.maxAspectRatioInPortrait
 * @see SplitRule.maxAspectRatioInLandscape
 */
class EmbeddingAspectRatio private constructor(
    /**
     * The description of this `EmbeddingAspectRatio`.
     */
    internal val description: String,

    /**
     * Aspect ratio, expressed as (longer dimension / shorter dimension) in decimal form, of the
     * parent window bounds.
     * It will act as special identifiers for [ALWAYS_ALLOW] and [ALWAYS_DISALLOW].
     */
    internal val value: Float
) {
    override fun toString() = "EmbeddingAspectRatio($description)"

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is EmbeddingAspectRatio) return false
        return value == other.value && description == other.description
    }

    override fun hashCode() = description.hashCode() + 31 * value.hashCode()

    companion object {
        /**
         * For max aspect ratio, when the aspect ratio is greater than this value, it means to
         * disallow embedding.
         *
         * For min aspect ratio, when the aspect ratio is smaller than this value, it means to
         * disallow embedding.
         *
         * Values smaller than or equal to `1` are invalid.
         *
         * @param ratio the aspect ratio.
         * @return the [EmbeddingAspectRatio] representing the [ratio].
         *
         * @see ALWAYS_ALLOW for always allow embedding.
         * @see ALWAYS_DISALLOW for always disallow embedding.
         */
        @JvmStatic
        fun ratio(@FloatRange(from = 1.0, fromInclusive = false) ratio: Float):
            EmbeddingAspectRatio {
            require(ratio > 1) { "Ratio must be greater than 1." }
            return EmbeddingAspectRatio("ratio:$ratio", ratio)
        }

        /**
         * Gets the special [EmbeddingAspectRatio] to represent it always allows embedding.
         *
         * An example use case is to set it on [SplitRule.maxAspectRatioInLandscape] if the app
         * wants to always allow embedding as split when the parent window is in landscape.
         */
        @JvmField
        val ALWAYS_ALLOW = EmbeddingAspectRatio("ALWAYS_ALLOW", 0f)

        /**
         * Gets the special [EmbeddingAspectRatio] to represent it always disallows embedding.
         *
         * An example use case is to set it on [SplitRule.maxAspectRatioInPortrait] if the app
         * wants to disallow embedding as split when the parent window is in portrait.
         */
        @JvmField
        val ALWAYS_DISALLOW = EmbeddingAspectRatio("ALWAYS_DISALLOW", -1f)

        /**
         * Returns a [EmbeddingAspectRatio] with the given [value].
         */
        internal fun buildAspectRatioFromValue(value: Float): EmbeddingAspectRatio {
            return when (value) {
                ALWAYS_ALLOW.value -> {
                    ALWAYS_ALLOW
                }
                ALWAYS_DISALLOW.value -> {
                    ALWAYS_DISALLOW
                }
                else -> {
                    ratio(value)
                }
            }
        }
    }
}
