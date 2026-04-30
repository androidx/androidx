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

package androidx.compose.material3.internal.ripple

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * The configuration for the ripple node created by [createRippleModifierNode].
 *
 * @param press the configuration for the press indication.
 * @param focus the configuration for the focus indication.
 * @param hover the configuration for the hover indication.
 * @param drag the configuration for the drag indication.
 */
internal class RippleNodeConfig(
    public val press: Press,
    public val focus: Focus,
    public val hover: Hover,
    public val drag: Drag,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleNodeConfig) return false

        if (press != other.press) return false
        if (focus != other.focus) return false
        if (hover != other.hover) return false
        if (drag != other.drag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = press.hashCode()
        result = 31 * result + focus.hashCode()
        result = 31 * result + hover.hashCode()
        result = 31 * result + drag.hashCode()
        return result
    }

    /** The possible configurations for the press indication. */
    internal abstract class Press private constructor() {
        /** No press indication - the created ripple will not show anything for press. */
        internal object None : Press()

        /**
         * An opacity-based press indication - the created ripple will show a layer with the given
         * [alpha] on a press.
         */
        internal class Opacity(val alpha: Float) : Press() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                if (alpha != other.alpha) return false
                return true
            }

            override fun hashCode(): Int {
                return alpha.hashCode()
            }
        }
    }

    /** The possible configurations for the focus indication. */
    internal abstract class Focus private constructor() {
        /** No focus indication - the created ripple will not show anything for press. */
        internal object None : Focus()

        /**
         * An opacity-based focus indication.
         *
         * @param alpha the alpha to apply to the layer.
         */
        internal class Opacity(val alpha: Float) : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                if (alpha != other.alpha) return false
                return true
            }

            override fun hashCode(): Int {
                return alpha.hashCode()
            }
        }

        /**
         * An inset ring focus indication - the created ripple will show an inset focus ring.
         *
         * @param shape the shape of the focus ring.
         * @param outerStrokeInset the inset from the edge of the shape's outline to the outer edge
         *   of the outer stroke.
         * @param outerStrokeWidth the width of the outer stroke.
         * @param outerStrokeColor the color of the outer stroke.
         * @param innerStrokeInset the inset from the edge of the shape's outline to the outer edge
         *   of the inner stroke.
         * @param innerStrokeWidth the width of the inner stroke.
         * @param innerStrokeColor the color of the inner stroke.
         * @param focusingAnimationSpec the animation spec used when gaining focus.
         * @param unfocusingAnimationSpec the animation spec used when losing focus.
         */
        internal class InsetRing(
            val shape: Shape,
            val outerStrokeInset: Dp,
            val outerStrokeWidth: Dp,
            val outerStrokeColor: ColorProducer,
            val innerStrokeInset: Dp,
            val innerStrokeWidth: Dp,
            val innerStrokeColor: ColorProducer,
            val focusingAnimationSpec: FiniteAnimationSpec<Float>,
            val unfocusingAnimationSpec: FiniteAnimationSpec<Float>,
        ) : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is InsetRing) return false

                if (shape != other.shape) return false
                if (outerStrokeInset != other.outerStrokeInset) return false
                if (outerStrokeWidth != other.outerStrokeWidth) return false
                if (outerStrokeColor != other.outerStrokeColor) return false
                if (innerStrokeInset != other.innerStrokeInset) return false
                if (innerStrokeWidth != other.innerStrokeWidth) return false
                if (innerStrokeColor != other.innerStrokeColor) return false
                if (focusingAnimationSpec != other.focusingAnimationSpec) return false
                if (unfocusingAnimationSpec != other.unfocusingAnimationSpec) return false

                return true
            }

            override fun hashCode(): Int {
                var result = shape.hashCode()
                result = 31 * result + outerStrokeInset.hashCode()
                result = 31 * result + outerStrokeWidth.hashCode()
                result = 31 * result + outerStrokeColor.hashCode()
                result = 31 * result + innerStrokeInset.hashCode()
                result = 31 * result + innerStrokeWidth.hashCode()
                result = 31 * result + innerStrokeColor.hashCode()
                result = 31 * result + focusingAnimationSpec.hashCode()
                result = 31 * result + unfocusingAnimationSpec.hashCode()
                return result
            }
        }
    }

    /** The possible configurations for the hover indication. */
    internal abstract class Hover private constructor() {
        /** No hover indication - the created ripple will not show anything for hover. */
        internal object None : Hover()

        /**
         * An opacity-based hover indication.
         *
         * @param alpha the alpha to apply to the layer.
         */
        internal class Opacity(val alpha: Float) : Hover() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                if (alpha != other.alpha) return false
                return true
            }

            override fun hashCode(): Int {
                return alpha.hashCode()
            }
        }
    }

    /** The possible configurations for the drag indication. */
    internal abstract class Drag private constructor() {
        /** No hover indication - the created ripple will not show anything for hover. */
        internal object None : Drag()

        /**
         * An opacity-based drag indication.
         *
         * @param alpha the alpha to apply to the layer.
         */
        internal class Opacity(val alpha: Float) : Drag() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                if (alpha != other.alpha) return false
                return true
            }

            override fun hashCode(): Int {
                return alpha.hashCode()
            }
        }
    }
}
