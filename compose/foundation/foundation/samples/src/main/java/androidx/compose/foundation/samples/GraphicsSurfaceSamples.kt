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

package androidx.compose.foundation.samples

import android.graphics.Rect
import androidx.annotation.Sampled
import androidx.compose.foundation.EmbeddedGraphicsSurface
import androidx.compose.foundation.GraphicsSurface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Sampled
@Composable
fun GraphicsSurfaceColors() {
    GraphicsSurface(
        modifier = Modifier.fillMaxWidth().height(400.dp)
    ) {
        // Resources can be initialized/cached here

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->
            var w = width
            var h = height

            // Initial draw to avoid a black frame
            surface.lockCanvas(Rect(0, 0, w, h)).apply {
                drawColor(Color.Blue.toArgb())
                surface.unlockCanvasAndPost(this)
            }

            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                w = newWidth
                h = newHeight
            }

            // Cleanup if needed
            surface.onDestroyed {
            }

            // Render loop, automatically cancelled by GraphicsSurface
            // on surface destruction
            while (true) {
                withFrameNanos { time ->
                    surface.lockCanvas(Rect(0, 0, w, h)).apply {
                        val timeMs = time / 1_000_000L
                        val t = 0.5f + 0.5f * sin(timeMs / 1_000.0f)
                        drawColor(lerp(Color.Blue, Color.Green, t).toArgb())
                        surface.unlockCanvasAndPost(this)
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun EmbeddedGraphicsSurfaceColors() {
    EmbeddedGraphicsSurface(
        modifier = Modifier.fillMaxWidth().height(400.dp)
    ) {
        // Resources can be initialized/cached here

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->
            var w = width
            var h = height

            // Initial draw to avoid a black frame
            surface.lockCanvas(Rect(0, 0, w, h)).apply {
                drawColor(Color.Yellow.toArgb())
                surface.unlockCanvasAndPost(this)
            }

            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                w = newWidth
                h = newHeight
            }

            // Cleanup if needed
            surface.onDestroyed {
            }

            // Render loop, automatically cancelled by EmbeddedGraphicsSurface
            // on surface destruction
            while (true) {
                withFrameNanos { time ->
                    surface.lockCanvas(Rect(0, 0, w, h)).apply {
                        val timeMs = time / 1_000_000L
                        val t = 0.5f + 0.5f * sin(timeMs / 1_000.0f)
                        drawColor(lerp(Color.Yellow, Color.Red, t).toArgb())
                        surface.unlockCanvasAndPost(this)
                    }
                }
            }
        }
    }
}
