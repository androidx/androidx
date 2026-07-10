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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.animation.core.tween
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.demo.AlwaysOnDisplay
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.SmartSpace

fun SceneTransitionsBuilder.alwaysOnDisplayTransitions() {
    to(Scenes.AlwaysOnDisplay) { defaultTransition() }

    // Given that ShadeTransitions.kt defines a generic from(Shade) transition, make sure we use
    // this transition when going from Shade to AOD.
    from(Scenes.Shade, to = Scenes.AlwaysOnDisplay) { defaultTransition() }

    // Given that SplitShadeTransitions.kt defines a generic from(SplitShade) transition, make sure
    // we use this transition when going from SplitShade to AOD.
    from(Scenes.SplitShade, to = Scenes.AlwaysOnDisplay) { defaultTransition() }
}

val AodBackgroundEndProgress = 0.5f

private fun TransitionBuilder.defaultTransition() {
    spec = tween(durationMillis = 500)

    fractionRange(end = AodBackgroundEndProgress) { fade(AlwaysOnDisplay.Elements.Background) }
    fractionRange(start = AodBackgroundEndProgress) {
        fade(Clock.Elements.Clock)
        fade(SmartSpace.Elements.SmartSpace)
    }
}
