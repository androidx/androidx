/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Typeface
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SimplePath() {
    RemoteRow(
        modifier = RemoteModifier.fillMaxSize(),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().fillMaxHeight()) {
            drawRect(
                Color.DarkGray.paint(),
                RemoteOffset(0f.rf, 0f.rf),
                RemoteSize(remote.component.width, remote.component.height),
            )

            val path =
                RemotePath().apply {
                    addArc(20f, 20f, 240f, 240f, 240f, 360f)
                    close()
                }

            val textPaint = RemotePaint {
                color = Color.Red.rc
                style = PaintingStyle.Fill
                textSize = 32f.rf
                typeface = Typeface.DEFAULT
                color = Color.White.rc
            }

            drawPath(
                path,
                paint = Color.Green.paint(style = PaintingStyle.Stroke, strokeWidth = 4f),
            )
            drawTextOnPath("10:10".rs, path, 20f.rf, 0f.rf, textPaint)
        }
    }
}

@Preview @Composable private fun SimplePathPreview() = RemotePreview { SimplePath() }
