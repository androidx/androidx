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
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.layout.Alignment
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Preview
@Composable
@RemoteComposable
fun SimplePath() {
    RemoteRow(
        modifier = RemoteModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().fillMaxHeight()) {
            val rec =
                Rect(
                    0f,
                    0f,
                    remote.component.width.internalAsFloat(),
                    remote.component.height.internalAsFloat(),
                )
            drawRect(Color.DarkGray, rec.topLeft, rec.size)

            val path =
                Path().apply {
                    addArc(Rect(20f, 20f, 240f, 240f), 240f, 360f)
                    close()
                }

            val textPaint =
                Paint()
                    .apply {
                        color = Color.Red
                        style = PaintingStyle.Fill
                    }
                    .asFrameworkPaint()
                    .apply {
                        textSize = with(density) { 32f }
                        typeface = Typeface.DEFAULT
                        color = Color.Red.toArgb()

                        // TODO why?
                        color = Color.White.toArgb()
                    }

            drawPath(path, color = Color.Green, style = Stroke(4f))
            val canvas = drawScope.drawContext.canvas.nativeCanvas
            if (canvas is RecordingCanvas) {
                canvas.drawTextOnPath("10:10", path.asAndroidPath(), 20f, 0f, textPaint)
            } else {
                canvas.drawTextOnPath("10:10", path.asAndroidPath(), 20f, 0f, textPaint)
            }
        }
    }
}
