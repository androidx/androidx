/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun EnterExitTransitionConfigSample() {
    var visible by remember { mutableStateOf(true) }

    // A custom transition container that uses enter/exit transition configurations to build custom
    // transition behavior.
    val CustomTransitionBox =
        @Composable { enter: EnterTransition, exit: ExitTransition, content: @Composable () -> Unit
            ->
            val activeConfig = if (visible) enter.config else exit.config

            // Inspect the configuration details at runtime
            val fadeConfig = activeConfig.fade
            val slideConfig = activeConfig.slide

            // Perform custom animation layout, drawing, or state changes using the retrieved
            // configs
            Box { content() }
        }

    Column {
        Button(onClick = { visible = !visible }) { Text("Toggle visibility") }

        CustomTransitionBox(
            fadeIn(animationSpec = tween(durationMillis = 500), initialAlpha = 0.2f),
            fadeOut(animationSpec = tween(durationMillis = 300), targetAlpha = 0f),
        ) {
            Box(Modifier.size(200.dp).background(Color.Blue))
        }
    }
}
