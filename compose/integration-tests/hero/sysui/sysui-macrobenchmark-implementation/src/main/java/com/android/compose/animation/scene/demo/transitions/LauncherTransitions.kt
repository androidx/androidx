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
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.demo.Bouncer
import com.android.compose.animation.scene.demo.Launcher
import com.android.compose.animation.scene.demo.Scenes

fun SceneTransitionsBuilder.launcherTransitions() {
    from(Scenes.Bouncer, to = Scenes.Launcher) {
        spec = tween(durationMillis = 500)

        translate(Bouncer.Elements.Content, y = (-150).dp)
        fractionRange(end = 0.5f) { fade(Bouncer.Elements.Content) }
        fractionRange(start = 0.5f) {
            fade(Bouncer.Elements.Background)
            scaleDraw(Launcher.Elements.Scene, 0.5f, 0.5f)
        }
    }
}
