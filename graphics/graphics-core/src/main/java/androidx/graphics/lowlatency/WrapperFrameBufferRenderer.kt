/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.FrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec

/**
 * Wrapper Renderer around [FrameBufferRenderer] that skips rendering on a given condition
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class WrapperFrameBufferRenderer<T>(
    private val frameBufferRenderer: FrameBufferRenderer,
    private val shouldRender: () -> Boolean
) : GLRenderer.RenderCallback {

    override fun onSurfaceCreated(
        spec: EGLSpec,
        config: EGLConfig,
        surface: Surface,
        width: Int,
        height: Int
    ): EGLSurface? = frameBufferRenderer.onSurfaceCreated(spec, config, surface, width, height)

    override fun onDrawFrame(eglManager: EGLManager) {
        if (shouldRender()) {
            frameBufferRenderer.onDrawFrame(eglManager)
        }
    }
}