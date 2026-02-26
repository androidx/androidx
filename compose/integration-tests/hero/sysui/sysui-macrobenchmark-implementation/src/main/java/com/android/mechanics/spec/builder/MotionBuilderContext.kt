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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.spec.builder

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.android.mechanics.spring.SpringParameters

/**
 * Device / scheme specific context for building motion specs.
 *
 * See go/motion-system.
 *
 * @see rememberMotionBuilderContext for Compose (in composition)
 * @see motionBuilderContext for Compose (in Modifier.Node)
 * @see standardViewMotionBuilderContext for Views
 * @see expressiveViewMotionBuilderContext for Views
 */
interface MotionBuilderContext : Density {
    /**
     * Spatial spring tokens.
     *
     * Used for animations that move something on screen, for example the x and y position,
     * rotation, size, rounded corners.
     *
     * See go/motion-system#b99b0d12-e9c8-4605-96dd-e3f17bfe9538
     */
    val spatial: MaterialSprings

    /**
     * Effects spring tokens.
     *
     * Used to animate properties such as color and opacity animations.
     *
     * See go/motion-system#142c8835-7474-4f74-b2eb-e1187051ec1f
     */
    val effects: MaterialSprings

    companion object {
        /** Default threshold for effect springs. */
        const val StableThresholdEffects = 0.01f
        /**
         * Default threshold for spatial springs.
         *
         * Cuts off when remaining oscillations are below 1px
         */
        const val StableThresholdSpatial = 1f
    }
}

/** Material spring tokens, see go/motion-system##63b14c00-d049-4d3e-b8b6-83d8f524a8db for usage. */
data class MaterialSprings(
    val default: SpringParameters,
    val fast: SpringParameters,
    val slow: SpringParameters,
    val stabilityThreshold: Float,
)

/** [MotionBuilderContext] based on the current [Density] and [MotionScheme]. */
@Composable
fun rememberMotionBuilderContext(): MotionBuilderContext {
    val density = LocalDensity.current
    val motionScheme = MaterialTheme.LocalMaterialTheme.current.motionScheme
    return remember(density, motionScheme) { ComposeMotionBuilderContext(motionScheme, density) }
}

/**
 * [MotionBuilderContext] for building motion specs in a [androidx.compose.ui.Modifier.Node].
 *
 * This should be read when the node is attached.
 */
fun CompositionLocalConsumerModifierNode.motionBuilderContext(): ComposeMotionBuilderContext {
    return ComposeMotionBuilderContext(
        motionScheme = currentValueOf(MaterialTheme.LocalMaterialTheme).motionScheme,
        density = currentValueOf(LocalDensity),
    )
}

/**
 * [MotionBuilderContext] based on Material 3 MotionScheme.
 *
 * Note: This will throw if the MotionScheme uses AnimationSpecs other than SpringSpecs.
 */
class ComposeMotionBuilderContext(motionScheme: MotionScheme, density: Density) :
    MotionBuilderContext, Density by density {

    override val spatial =
        MaterialSprings(
            SpringParameters(motionScheme.defaultSpatialSpec<Float>()),
            SpringParameters(motionScheme.fastSpatialSpec<Float>()),
            SpringParameters(motionScheme.slowSpatialSpec<Float>()),
            MotionBuilderContext.StableThresholdSpatial,
        )
    override val effects =
        MaterialSprings(
            SpringParameters(motionScheme.defaultEffectsSpec<Float>()),
            SpringParameters(motionScheme.fastEffectsSpec<Float>()),
            SpringParameters(motionScheme.slowEffectsSpec<Float>()),
            MotionBuilderContext.StableThresholdEffects,
        )
}
