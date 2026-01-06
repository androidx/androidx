/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.view

import android.content.Context
import androidx.compose.ui.unit.Density
import com.android.mechanics.spec.builder.MaterialSprings
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spring.SpringParameters
import com.android.mechanics.view.ViewMaterialSprings.Default

/**
 * Creates a [MotionBuilderContext] using the **standard** motion spec.
 *
 * See go/motion-system.
 *
 * @param context The context to derive the density from.
 */
fun standardViewMotionBuilderContext(context: Context): MotionBuilderContext {
    return standardViewMotionBuilderContext(context.resources.displayMetrics.density)
}

/**
 * Creates a [MotionBuilderContext] using the **standard** motion spec.
 *
 * See go/motion-system.
 *
 * @param density The density of the display, as a scaling factor for the dp to px conversion.
 */
fun standardViewMotionBuilderContext(density: Float): MotionBuilderContext {
    return with(ViewMaterialSprings.Default) {
        ViewMotionBuilderContext(Spatial, Effects, Density(density))
    }
}

/**
 * Creates a [MotionBuilderContext] using the **expressive** motion spec.
 *
 * See go/motion-system.
 *
 * @param context The context to derive the density from.
 */
fun expressiveViewMotionBuilderContext(context: Context): MotionBuilderContext {
    return expressiveViewMotionBuilderContext(context.resources.displayMetrics.density)
}

/**
 * Creates a [MotionBuilderContext] using the **expressive** motion spec.
 *
 * See go/motion-system.
 *
 * @param density The density of the display, as a scaling factor for the dp to px conversion.
 */
fun expressiveViewMotionBuilderContext(density: Float): MotionBuilderContext {
    return with(ViewMaterialSprings.Expressive) {
        ViewMotionBuilderContext(Spatial, Effects, Density(density))
    }
}

/**
 * Material motion system spring definitions.
 *
 * See go/motion-system.
 *
 * NOTE: These are only defined here since material spring parameters are not available for View
 * based APIs. There might be a delay in updating these values, should the material tokens be
 * updated in the future.
 *
 * @see rememberMotionBuilderContext for Compose
 */
object ViewMaterialSprings {
    object Default {
        val Spatial =
            MaterialSprings(
                SpringParameters(700.0f, 0.9f),
                SpringParameters(1400.0f, 0.9f),
                SpringParameters(300.0f, 0.9f),
                MotionBuilderContext.StableThresholdSpatial,
            )

        val Effects =
            MaterialSprings(
                SpringParameters(1600.0f, 1.0f),
                SpringParameters(3800.0f, 1.0f),
                SpringParameters(800.0f, 1.0f),
                MotionBuilderContext.StableThresholdEffects,
            )
    }

    object Expressive {
        val Spatial =
            MaterialSprings(
                SpringParameters(380.0f, 0.8f),
                SpringParameters(800.0f, 0.6f),
                SpringParameters(200.0f, 0.8f),
                MotionBuilderContext.StableThresholdSpatial,
            )

        val Effects =
            MaterialSprings(
                SpringParameters(1600.0f, 1.0f),
                SpringParameters(3800.0f, 1.0f),
                SpringParameters(800.0f, 1.0f),
                MotionBuilderContext.StableThresholdEffects,
            )
    }
}

internal class ViewMotionBuilderContext(
    override val spatial: MaterialSprings,
    override val effects: MaterialSprings,
    density: Density,
) : MotionBuilderContext, Density by density
