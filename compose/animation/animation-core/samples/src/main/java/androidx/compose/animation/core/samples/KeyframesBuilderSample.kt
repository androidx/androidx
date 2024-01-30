/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.KeyframesSpecBaseConfig
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Sampled
fun FloatKeyframesBuilder() {
    KeyframesSpec(
        KeyframesSpec.KeyframesSpecConfig<Float>().apply {
            0f at 0 // ms  // Optional
            0.4f at 75 // ms
            0.4f at 225 // ms
            0f at 375 // ms  // Optional
            durationMillis = 375
        }
    )
}

@Sampled
fun FloatKeyframesBuilderInline() {
    keyframes {
        0f at 0 // ms  // Optional
        0.4f at 75 // ms
        0.4f at 225 // ms
        0f at 375 // ms  // Optional
        durationMillis = 375
    }
}

@Sampled
fun KeyframesBuilderWithEasing() {
    // Use FastOutSlowInEasing for the interval from 0 to 50 ms, and LinearOutSlowInEasing for the
    // time between 50 and 100ms
    keyframes<Float> {
        durationMillis = 100
        0f at 0 using FastOutSlowInEasing
        1.5f at 50 using LinearOutSlowInEasing
        1f at 100
    }
}

@Sampled
fun KeyframesBuilderForPosition() {
    // Use FastOutSlowInEasing for the interval from 0 to 50 ms, and LinearOutSlowInEasing for the
    // time between 50 and 100ms
    keyframes<DpOffset> {
        durationMillis = 200
        DpOffset(0.dp, 0.dp) at 0 using LinearEasing
        DpOffset(500.dp, 100.dp) at 100 using LinearOutSlowInEasing
        DpOffset(400.dp, 50.dp) at 150
    }
}

@Sampled
fun KeyframesSpecBaseConfig<Float, KeyframesSpec.KeyframeEntity<Float>>.floatAtSample() {
    0.8f at 150 // ms
}

@Sampled
fun KeyframesSpecBaseConfig<Float, KeyframesSpec.KeyframeEntity<Float>>.floatAtFractionSample() {
    // Make sure to set the duration before calling `atFraction` otherwise the keyframe will be set
    // based on the default duration
    durationMillis = 300
    0.8f atFraction 0.50f // half of the overall duration set
}
