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

package androidx.compose.material3.ripple

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * The configuration for the ripple node created by [createRippleModifierNode].
 *
 * @sample androidx.compose.material3.ripple.samples.CreateRippleModifierNodeSample
 * @param isBounded if true, ripples are clipped by the bounds of the target layout. Unbounded
 *   ripples always animate from the target layout center, bounded ripples animate from the touch
 *   position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the main color of the ripple. This color is usually the same color used by the text
 *   or iconography in the component. This color will then have the various interaction type
 *   configurations applied to calculate the final color used to draw the ripple.
 * @param pressConfiguration the configuration for the visual representation of a press.
 * @param focusConfiguration the configuration for the visual representation of focus.
 * @param hoverConfiguration the configuration for the visual representation of hover.
 * @param dragConfiguration the configuration for the visual representation of drag.
 */
@Immutable
public class RippleNodeConfiguration(
    public val isBounded: Boolean,
    public val radius: Dp,
    public val color: ColorProducer,
    public val pressConfiguration: PressConfiguration,
    public val focusConfiguration: FocusConfiguration,
    public val hoverConfiguration: HoverConfiguration,
    public val dragConfiguration: DragConfiguration,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleNodeConfiguration) return false

        if (isBounded != other.isBounded) return false
        if (radius != other.radius) return false
        if (color != other.color) return false
        if (pressConfiguration != other.pressConfiguration) return false
        if (focusConfiguration != other.focusConfiguration) return false
        if (hoverConfiguration != other.hoverConfiguration) return false
        if (dragConfiguration != other.dragConfiguration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isBounded.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + pressConfiguration.hashCode()
        result = 31 * result + focusConfiguration.hashCode()
        result = 31 * result + hoverConfiguration.hashCode()
        result = 31 * result + dragConfiguration.hashCode()
        return result
    }

    /** Represents the configuration for the visual representation of a press */
    public sealed interface PressConfiguration {
        /** No press visual - the created ripple will not show anything for press. */
        public object None : PressConfiguration

        /**
         * An opacity-based press visual - the created ripple will show a layer with the given
         * [alpha] on a press.
         *
         * @param alpha the alpha to apply to the layer.
         */
        public class Opacity(@param:FloatRange(0.0, 1.0) public val alpha: Float) :
            PressConfiguration {
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

        private object NonExhaustive : PressConfiguration
    }

    /** Represents the configuration for the visual representation of focus */
    public sealed interface FocusConfiguration {
        /** No focus visual - the created ripple will not show anything for focus. */
        public object None : FocusConfiguration

        /**
         * An opacity-based focus visual.
         *
         * @param alpha the alpha to apply to the layer.
         */
        public class Opacity(@param:FloatRange(0.0, 1.0) public val alpha: Float) :
            FocusConfiguration {
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
         * An inset ring focus visual - the created ripple will show an inset focus ring.
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
        public class InsetRing(
            public val shape: Shape,
            public val outerStrokeInset: Dp,
            public val outerStrokeWidth: Dp,
            public val outerStrokeColor: ColorProducer,
            public val innerStrokeInset: Dp,
            public val innerStrokeWidth: Dp,
            public val innerStrokeColor: ColorProducer,
            public val focusingAnimationSpec: FiniteAnimationSpec<Float>,
            public val unfocusingAnimationSpec: FiniteAnimationSpec<Float>,
        ) : FocusConfiguration {
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

        private object NonExhaustive : FocusConfiguration
    }

    /** Represents the configuration for the visual representation of hover */
    public sealed interface HoverConfiguration {
        /** No hover visual - the created ripple will not show anything for hover. */
        public object None : HoverConfiguration

        /**
         * An opacity-based hover visual.
         *
         * @param alpha the alpha to apply to the layer.
         */
        public class Opacity(@param:FloatRange(0.0, 1.0) public val alpha: Float) :
            HoverConfiguration {
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

        private object NonExhaustive : HoverConfiguration
    }

    /** Represents the configuration for the visual representation of drag */
    public sealed interface DragConfiguration {
        /** No drag visual - the created ripple will not show anything for drag. */
        public object None : DragConfiguration

        /**
         * An opacity-based drag visual.
         *
         * @param alpha the alpha to apply to the layer.
         */
        public class Opacity(@param:FloatRange(0.0, 1.0) public val alpha: Float) :
            DragConfiguration {
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

        private object NonExhaustive : DragConfiguration
    }
}
