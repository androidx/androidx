/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.graphics.filters

import android.content.Context
import androidx.annotation.FloatRange
import androidx.media3.common.FrameProcessingException
import androidx.media3.common.util.Assertions
import androidx.media3.effect.GlEffect
import androidx.media3.effect.SingleFrameGlTextureProcessor

/**
 * A {@link androidx.media3.common.GlEffect} to apply a vignetting effect to image data. The
 * vignetting may be applied to color and/or alpha channels.
 *
 * Inner radius and outer radius are in [0,1], normalized by the distance from the center of the
 * image to a corner. A pixels within the inner radius are unaffected, while affected pixel values
 * outside the outer radius are set to 0. Pixels between the inner and outer radius are interpolated
 * linearly.
 *
 * @param innerRadius Radius in [0,1], normalized by the distance from image center to corner. All
 *   pixels within innerRadius are unaffected by the vignette.
 * @param outerRadius Radius in [0,1], normalized by the distance from image center to corner. All
 *   pixels outside of outerRadius are fully vignetted.
 * @param vignetteStyle Enum indicating to which channels vignetting should be applied.
 */
internal class Vignette(
    @FloatRange(from = 0.0, to = 1.0) innerRadius: Float,
    @FloatRange(from = 0.0, to = 1.0) outerRadius: Float,
    vignetteStyle: VignetteStyle = VignetteStyle.COLOR,
) : GlEffect {

    enum class VignetteStyle {
        COLOR, // Vignetting is applied to color channels only.
        ALPHA, // Vignetting is applied to alpha channel only.
        COLOR_AND_ALPHA // Vignetting is applied to color and alpha channels.
    }

    private val mInnerRadius: Float
    val innerRadius
        get() = this.mInnerRadius

    private val mOuterRadius: Float
    val outerRadius
        get() = this.mOuterRadius

    private val mVignetteStyle: VignetteStyle
    val vignetteStyle
        get() = this.mVignetteStyle

    init {
        Assertions.checkArgument(
            innerRadius in 0.0..1.0,
            "InnerRadius needs to be in the interval [0, 1]."
        )
        Assertions.checkArgument(
            outerRadius in innerRadius..1.0F,
            "InnerRadius needs to be in the interval [innerRadius, 1]."
        )

        this.mInnerRadius = innerRadius
        this.mOuterRadius = outerRadius
        this.mVignetteStyle = vignetteStyle
    }

    // media3 GlEffect does not annotate nullability of toGlTextureProcessor.  Cannot override
    // toGlTextureProcessor and satisfy API lint simultaneously.
    @Throws(FrameProcessingException::class)
    override fun toGlTextureProcessor(
        @Suppress("InvalidNullabilityOverride") // Remove when b/264908709 is resolved.
        context: Context,
        useHdr: Boolean
    ): SingleFrameGlTextureProcessor {
        return VignetteProcessor(context, this, useHdr)
    }
}
