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

package androidx.graphics.core.samples

import android.content.Context
import android.opengl.Matrix
import android.view.SurfaceView
import androidx.annotation.Sampled
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.opengl.GLFrameBufferRenderer
import androidx.graphics.opengl.egl.EGLManager

@Sampled
fun glFrameBufferSample(context: Context) {
    val surfaceView = SurfaceView(context)
    val renderer = GLFrameBufferRenderer.Builder(surfaceView,
        object : GLFrameBufferRenderer.Callback {

            val myMatrix = FloatArray(16)
            val result = FloatArray(16)

            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                Matrix.orthoM(
                    myMatrix, // matrix
                    0, // offset starting index into myMatrix
                    0f, // left
                    bufferInfo.width.toFloat(), // right
                    0f, // bottom
                    bufferInfo.width.toFloat(), // top
                    -1f, // near
                    1f // far
                )

                Matrix.multiplyMM(result, 0, myMatrix, 0, transform, 0)

                // pass result matrix as uniform to shader logic
            }
        }).build()
    renderer.render()
}
