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

package androidx.tv.material3.tokens

import androidx.compose.animation.core.CubicBezierEasing

internal object SurfaceScaleTokens {
    const val focusDuration: Int = 300
    const val unFocusDuration: Int = 500
    const val pressedDuration: Int = 120
    const val releaseDuration: Int = 300
    val enterEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
}
