/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:JvmName("BrushBehaviorSamples")

package androidx.ink.brush.samples

import androidx.annotation.Sampled
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.behavior.DampingNode
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target

/**
 * Creates a brush behavior that maps stylus pressure to a tip size multiplier, with some
 * distance-based damping applied.
 */
@Sampled
public fun createPressureToSizeBehavior(): BrushBehavior =
    BrushBehavior(
        TargetNode(
            // Modify the size of the brush tip, anywhere from 50% of normal up to 150% of normal.
            target = Target.SIZE_MULTIPLIER,
            targetModifierRangeStart = 0.5f,
            targetModifierRangeEnd = 1.5f,
            input =
                DampingNode(
                    // Apply distance-based damping to the input pressure, so that sudden changes in
                    // pressure
                    // will not result in immediate changes to tip size. Instead, the input value to
                    // the
                    // `TargetNode` will fade towards the current value of the `SourceNode` over a
                    // distance of
                    // approximately 75% of the base brush diameter.
                    dampingSource = ProgressDomain.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE,
                    strength = 0.75f,
                    input =
                        SourceNode(
                            // This behavior is based on stylus pressure, with minimum (0) pressure
                            // mapping to a
                            // 50% size multiplier, and maximum (1) pressure mapping to a 150% size
                            // multiplier.
                            source = Source.NORMALIZED_PRESSURE,
                            sourceValueRangeStart = 0.0f,
                            sourceValueRangeEnd = 1.0f,
                        ),
                ),
        )
    )
