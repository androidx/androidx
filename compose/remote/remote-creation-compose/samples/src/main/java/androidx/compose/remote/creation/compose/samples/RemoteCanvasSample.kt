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

package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteShaderBrush
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteCanvasSample() {
    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
        val paint = RemotePaint().apply { color = Color.Red.rc }
        drawCircle(paint = paint, radius = 50.rf)
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteCanvasAnimationSample() {
    // This sample uses ContinuousSec, but demonstrates how to create time-based
    // animations on the remote canvas.
    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
        val paint = RemotePaint().apply { color = Color.Blue.rc }

        val time = remote.time.ContinuousSec()

        // Oscillate radius between 10 and 40
        val sineValue = sin(time)
        val normalizedSine = (sineValue + 1f.rf) / 2f.rf
        val radius = 10f.rf + 30f.rf * normalizedSine

        drawCircle(paint = paint, radius = radius)
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteCanvasRectSample() {
    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
        val paint = RemotePaint().apply { color = Color.Blue.rc }
        drawRect(paint = paint)
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteCanvasShaderMatrixSample() {
    RemoteCanvas(modifier = RemoteModifier.size(200.rdp)) {
        val brush: RemoteShaderBrush =
            RemoteBrush.linearGradient(
                colors = listOf(Color.Red.rc, Color.Blue.rc),
                start = RemoteOffset.Zero,
                end = RemoteOffset(width, height),
            )
        val matrix = RemoteMatrix3x3.createRotate(45f.rf)
        val paint = RemotePaint()
        with(brush) { applyTo(paint, size, matrix) }
        drawRect(paint = paint)
    }
}
