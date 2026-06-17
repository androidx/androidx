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

import androidx.collection.MutableScatterMap
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

internal class CommonRippleNode(
    interactionSource: InteractionSource,
    rippleNodeConfiguration: () -> RippleNodeConfiguration,
) : RippleNode(interactionSource, rippleNodeConfiguration) {
    private val ripples = MutableScatterMap<PressInteraction.Press, RippleAnimation>()

    override fun addRipple(interaction: PressInteraction.Press, size: Size, targetRadius: Float) {
        // Finish existing ripples
        ripples.forEach { _, ripple -> ripple.finish() }
        val config = resolveRippleNodeConfiguration()
        val origin = if (config.isBounded) interaction.pressPosition else null
        val rippleAnimation =
            RippleAnimation(origin = origin, radius = targetRadius, bounded = config.isBounded)
        ripples[interaction] = rippleAnimation
        coroutineScope.launch {
            try {
                rippleAnimation.animate()
            } finally {
                ripples.remove(interaction)
                invalidateDraw()
            }
        }
        invalidateDraw()
    }

    override fun removeRipple(interaction: PressInteraction.Press) {
        ripples[interaction]?.finish()
    }

    override fun DrawScope.drawRipples() {
        val config = resolveRippleNodeConfiguration()
        val alpha =
            when (val pressConfig = config.pressConfiguration) {
                is RippleNodeConfiguration.PressConfiguration.Opacity -> pressConfig.alpha
                is RippleNodeConfiguration.PressConfiguration.None -> 0f
                else -> 0f
            }
        if (alpha != 0f) {
            ripples.forEach { _, ripple ->
                with(ripple) { draw(config.color().copy(alpha = alpha)) }
            }
        }
    }

    override fun onDetach() {
        ripples.clear()
        super.onDetach()
    }
}
