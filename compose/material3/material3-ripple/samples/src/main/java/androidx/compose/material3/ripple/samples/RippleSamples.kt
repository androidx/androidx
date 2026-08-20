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

package androidx.compose.material3.ripple.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple.RippleNodeConfiguration
import androidx.compose.material3.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun CreateRippleModifierNodeSample() {
    /** Example CompositionLocals for a custom design system theme. */
    val LocalCustomRippleColor: ProvidableCompositionLocal<Color> = compositionLocalOf { Color.Red }
    val LocalCustomRippleEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

    /**
     * A custom [Modifier.Node] that queries design system theme values and delegates to
     * [createRippleModifierNode].
     */
    class CustomRippleNode(
        private val interactionSource: InteractionSource,
        private val bounded: Boolean,
        private val radius: Dp,
        private val color: Color,
    ) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
        private var rippleNode: DelegatableNode? = null

        private val calculateColor = ColorProducer {
            if (color.isSpecified) {
                color
            } else {
                currentValueOf(LocalCustomRippleColor)
            }
        }

        private val rippleNodeConfiguration =
            RippleNodeConfiguration(
                isBounded = bounded,
                radius = radius,
                color = calculateColor,
                pressConfiguration =
                    RippleNodeConfiguration.PressConfiguration.Opacity(alpha = 0.24f),
                focusConfiguration =
                    RippleNodeConfiguration.FocusConfiguration.Opacity(alpha = 0.24f),
                hoverConfiguration =
                    RippleNodeConfiguration.HoverConfiguration.Opacity(alpha = 0.08f),
                dragConfiguration = RippleNodeConfiguration.DragConfiguration.Opacity(alpha = 0.16f),
            )

        override fun onAttach() {
            updateConfiguration()
        }

        override fun onObservedReadsChanged() {
            updateConfiguration()
        }

        private fun updateConfiguration() {
            observeReads {
                val isEnabled = currentValueOf(LocalCustomRippleEnabled)
                if (isEnabled) {
                    if (rippleNode == null) {
                        attachNewRipple()
                    }
                } else {
                    removeRipple()
                }
            }
        }

        private fun attachNewRipple() {
            rippleNode =
                delegate(
                    createRippleModifierNode(
                        interactionSource = interactionSource,
                        rippleNodeConfiguration = { rippleNodeConfiguration },
                    )
                )
        }

        private fun removeRipple() {
            rippleNode?.let { undelegate(it) }
            rippleNode = null
        }
    }

    /** A custom [IndicationNodeFactory] that creates a [CustomRippleNode]. */
    class CustomRipple(
        private val bounded: Boolean = true,
        private val radius: Dp = Dp.Unspecified,
        private val color: Color = Color.Unspecified,
    ) : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return CustomRippleNode(interactionSource, bounded, radius, color)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomRipple) return false
            if (bounded != other.bounded) return false
            if (radius != other.radius) return false
            if (color != other.color) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bounded.hashCode()
            result = 31 * result + radius.hashCode()
            result = 31 * result + color.hashCode()
            return result
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier.size(100.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = CustomRipple(color = Color.Blue),
                    onClick = {},
                )
    )
}
