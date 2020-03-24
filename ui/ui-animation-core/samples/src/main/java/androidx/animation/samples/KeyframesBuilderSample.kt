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

package androidx.animation.samples

import androidx.animation.FastOutSlowInEasing
import androidx.animation.KeyframesBuilder
import androidx.annotation.Sampled

@Sampled
fun FloatKeyframesBuilder() {
    KeyframesBuilder<Float>().apply {
        0f at 0 // ms  // Optional
        duration = 375
        0.4f at 75 // ms
        0.4f at 225 // ms
        0f at 375 // ms  // Optional
    }
}

@Sampled
fun KeyframesBuilderWithEasing() {
    // Use FastOutSlowInEasing for the interval from 0 to 100 ms.
    KeyframesBuilder<Float>().apply {
        duration = 100
        0f at 0 with FastOutSlowInEasing
        1f at 100
    }
}
