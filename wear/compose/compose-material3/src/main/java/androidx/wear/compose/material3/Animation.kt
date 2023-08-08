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

package androidx.wear.compose.material3

import androidx.compose.animation.core.CubicBezierEasing

// See Wear Motion durations: https://carbon.googleplex.com/wear-os-3/pages/speed
internal const val SHORT_1 = 50
internal const val SHORT_2 = 100
internal const val SHORT_3 = 150
internal const val SHORT_4 = 200

internal const val MEDIUM_1 = 250
internal const val MEDIUM_2 = 300
internal const val MEDIUM_3 = 350
internal const val MEDIUM_4 = 400

internal const val LONG_1 = 450
internal const val LONG_2 = 500
internal const val LONG_3 = 550
internal const val LONG_4 = 600

internal const val EXTRA_LONG_1 = 700
internal const val EXTRA_LONG_2 = 800
internal const val EXTRA_LONG_3 = 900
internal const val EXTRA_LONG_4 = 1000

// See Wear Motion easings: https://carbon.googleplex.com/wear-os-3/pages/easings
internal val STANDARD = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
internal val STANDARD_ACCELERATE = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
internal val STANDARD_DECELERATE = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
