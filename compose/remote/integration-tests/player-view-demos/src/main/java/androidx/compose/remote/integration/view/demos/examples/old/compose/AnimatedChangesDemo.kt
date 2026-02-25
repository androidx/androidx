/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old.compose

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
fun AnimatedChangesDemo() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize(),
        verticalArrangement = RemoteArrangement.Center,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize().background(Color.White)) {
            val width = remote.component.width
            val height = remote.component.height
            val centerX = width / 2f
            val centerY = height / 2f
            val rad = width.min(height) / 4f

            val beat = remote.time.ContinuousSec() * 2f
            val anim = remote.animateFloat(beat, duration = 0.5f)

            translate(0.rf, anim * 100f) {
                drawCircle(
                    paint = RemotePaint().apply { color = android.graphics.Color.RED },
                    radius = rad,
                    center = RemoteOffset(centerX, centerY),
                )
            }

            run {
                val color = android.graphics.Color.rgb(0x7B, 0x00, 0xFF)
                val len = centerY
                drawLine(
                    paint = RemotePaint().apply { this.color = color },
                    start = RemoteOffset(centerX, 0f),
                    end = RemoteOffset(centerX, len),
                )
                drawCircle(
                    paint = RemotePaint().apply { this.color = color },
                    radius = rad,
                    center = RemoteOffset(centerX, len),
                )
            }
        }
    }
}
