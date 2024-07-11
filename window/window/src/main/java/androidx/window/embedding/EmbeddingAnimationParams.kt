/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.IntRange
import androidx.window.embedding.EmbeddingAnimationParams.AnimationSpec.Companion.DEFAULT

/**
 * Parameters to be used for window transition animations for embedding activities.
 *
 * @property animationBackground the animation background to use during the animation of the split
 *   involving this `EmbeddingAnimationParams` object if the animation requires a background. The
 *   default is to use the current theme window background color.
 * @property openAnimation the animation spec to use for open transitions (when starting/entering an
 *   activity or when an activity moves to front).
 * @property closeAnimation the animation spec to use for close transitions (when finishing/closing
 *   an activity or when an activity moves to back).
 * @property changeAnimation the animation spec to use for change transitions (when an activity
 *   resizes or moves).
 * @see SplitAttributes.animationParams
 * @see EmbeddingAnimationBackground
 * @see EmbeddingAnimationBackground.createColorBackground
 * @see EmbeddingAnimationBackground.DEFAULT
 */
class EmbeddingAnimationParams
@JvmOverloads
constructor(
    val animationBackground: EmbeddingAnimationBackground = EmbeddingAnimationBackground.DEFAULT,
    val openAnimation: AnimationSpec = DEFAULT,
    val closeAnimation: AnimationSpec = DEFAULT,
    val changeAnimation: AnimationSpec = DEFAULT,
) {

    /** The animation to use when an activity transitions (e.g. open, close, or change). */
    class AnimationSpec
    private constructor(
        /**
         * The unique integer value for the `splitAnimationSpec`. This can be used as an enum value
         * when defining `splitAnimationSpec` attributes in XML.
         */
        internal val value: Int,
    ) {

        /**
         * A string representation of this `AnimationSpec`.
         *
         * @return the string representation of the object.
         */
        override fun toString(): String =
            when (value) {
                0 -> "DEFAULT"
                1 -> "JUMP_CUT"
                else -> "Unknown value: $value"
            }

        /** Properties and methods. */
        companion object {
            /** Specifies the default animation defined by the system. */
            @JvmField val DEFAULT = AnimationSpec(0)
            /** Specifies an animation with zero duration. */
            @JvmField val JUMP_CUT = AnimationSpec(1)

            /** Returns `AnimationSpec` with the given integer `value`. */
            @JvmStatic
            internal fun getAnimationSpecFromValue(@IntRange(from = 0, to = 1) value: Int) =
                when (value) {
                    DEFAULT.value -> DEFAULT
                    JUMP_CUT.value -> JUMP_CUT
                    else -> throw IllegalArgumentException("Undefined value:$value")
                }
        }
    }

    /**
     * Returns a hash code for this `EmbeddingAnimationParams` object.
     *
     * @return the hash code for this object.
     */
    override fun hashCode(): Int {
        var result = animationBackground.hashCode()
        result = result * 31 + openAnimation.hashCode()
        result = result * 31 + closeAnimation.hashCode()
        result = result * 31 + changeAnimation.hashCode()
        return result
    }

    /**
     * Determines whether this object has the same animation parameters as the compared object.
     *
     * @param other the object to compare to this object.
     * @return true if the objects have the same animation parameters, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingAnimationParams) return false
        return animationBackground == other.animationBackground &&
            openAnimation == other.openAnimation &&
            closeAnimation == other.closeAnimation &&
            changeAnimation == other.changeAnimation
    }

    /**
     * A string representation of this `EmbeddingAnimationParams` object.
     *
     * @return the string representation of the object.
     */
    override fun toString(): String =
        "${EmbeddingAnimationParams::class.java.simpleName}:" +
            "{animationBackground=$animationBackground, openAnimation=$openAnimation, " +
            "closeAnimation=$closeAnimation, changeAnimation=$changeAnimation }"

    /** Builder for creating an instance of [EmbeddingAnimationParams]. */
    class Builder {
        private var animationBackground = EmbeddingAnimationBackground.DEFAULT
        private var openAnimation = DEFAULT
        private var closeAnimation = DEFAULT
        private var changeAnimation = DEFAULT

        /**
         * Sets the animation background.
         *
         * The default is to use the current theme window background color.
         *
         * @param background the animation background.
         * @return this `Builder`.
         * @see EmbeddingAnimationBackground
         */
        fun setAnimationBackground(background: EmbeddingAnimationBackground): Builder = apply {
            this.animationBackground = background
        }

        /**
         * Sets the open animation.
         *
         * The default is to use the system animation.
         *
         * @param spec the animation transition spec
         * @return this `Builder`.
         */
        fun setOpenAnimation(spec: AnimationSpec): Builder = apply { this.openAnimation = spec }

        /**
         * Sets the close animation.
         *
         * The default is to use the system animation.
         *
         * @param spec the animation transition spec
         * @return this `Builder`.
         */
        fun setCloseAnimation(spec: AnimationSpec): Builder = apply { this.closeAnimation = spec }

        /**
         * Sets the change (resize or move) animation.
         *
         * The default is to use the system animation.
         *
         * @param spec the animation spec
         * @return this `Builder`.
         */
        fun setChangeAnimation(spec: AnimationSpec): Builder = apply { this.changeAnimation = spec }

        /**
         * Builds an `EmbeddingAnimationParams` instance with the attributes specified by the
         * builder's setters.
         *
         * @return the new `EmbeddingAnimationParams` instance.
         */
        fun build(): EmbeddingAnimationParams =
            EmbeddingAnimationParams(
                animationBackground,
                openAnimation,
                closeAnimation,
                changeAnimation
            )
    }
}
