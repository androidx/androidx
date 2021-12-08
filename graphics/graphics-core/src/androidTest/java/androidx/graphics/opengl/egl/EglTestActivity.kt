/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.graphics.opengl.egl

import android.app.Activity
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import java.lang.IllegalStateException

class EglTestActivity : Activity() {

    private val eglManager = EglManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.takeSurface(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                eglManager.setup(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                eglManager.release()
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                // Fill the screen red
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                eglManager.swapAndFlushBuffers()
            }
        })
    }

    fun EglManager.setup(surface: Surface) {
        initialize()
        loadConfig(EglConfigAttributes8888)?.let {
            createContext(it)
            val attrs = EglConfigAttributes {
                EGL14.EGL_RENDER_BUFFER to EGL14.EGL_SINGLE_BUFFER
            }
            val eglSurface = eglSpec.eglCreateWindowSurface(it, surface, attrs)
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                makeCurrent(eglSurface)
            } else {
                throw IllegalStateException("Bad surface")
            }
        }
    }
}