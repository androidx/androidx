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

package androidx.wear.compose.material

import androidx.compose.animation.core.CubicBezierEasing

// See Wear Motion durations: https://carbon.googleplex.com/wear-os-3/pages/durations
internal const val FLASH = 75
internal const val RAPID = 150
internal const val QUICK = 250
internal const val STANDARD = 300
internal const val CASUAL = 400

// See Wear Motion easings: https://carbon.googleplex.com/wear-os-3/pages/easings
internal val STANDARD_IN = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
internal val STANDARD_OUT = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
