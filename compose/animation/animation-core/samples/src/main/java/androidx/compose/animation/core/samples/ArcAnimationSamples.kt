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

@file:OptIn(ExperimentalAnimationSpecApi::class)

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ArcMode.Companion.ArcAbove
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.geometry.Offset

@Sampled
fun OffsetArcAnimationSpec() {
    // Will interpolate the Offset in arcs such that the curve of the quarter of an Ellipse is above
    // the center.
    ArcAnimationSpec<Offset>(mode = ArcAbove)
}

@Sampled
fun OffsetKeyframesWithArcsBuilder() {
    keyframes<Offset> {
        // Animate for 1.2 seconds
        durationMillis = 1200

        // Animate to Offset(100f, 100f) at 50% of the animation using LinearEasing then, animate
        // using ArcAbove for the rest of the animation
        Offset(100f, 100f) atFraction 0.5f using LinearEasing using ArcAbove
    }
}
