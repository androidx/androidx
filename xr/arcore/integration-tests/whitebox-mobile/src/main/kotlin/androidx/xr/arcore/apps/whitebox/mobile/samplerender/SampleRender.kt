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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * A SampleRender context.
 *
 * @param glSurfaceView Android GLSurfaceView
 * @param renderer Renderer implementation to receive callbacks
 * @param assetManager AssetManager for loading Android resources
 */
public class SampleRender(
    glSurfaceView: GLSurfaceView,
    renderer: Renderer,
    private val assetManager: AssetManager,
) {

    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1

    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.setRenderer(
            object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                    GLES30.glEnable(GLES30.GL_BLEND)
                    maybeThrowGLException("Failed to enable blending", "glEnable")
                    renderer.onSurfaceCreated(this@SampleRender)
                }

                override fun onSurfaceChanged(gl: GL10, w: Int, h: Int) {
                    Log.d(TAG, "onSurfaceChanged: $w x $h")
                    viewportWidth = w
                    viewportHeight = h
                    renderer.onSurfaceChanged(this@SampleRender, w, h)
                }

                override fun onDrawFrame(gl: GL10) {
                    clear(framebuffer = null, 0f, 0f, 0f, 1f)
                    renderer.onDrawFrame(this@SampleRender)
                }
            }
        )
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)
    }

    /** Draw a [Mesh] with the specified [Shader]. */
    public fun draw(mesh: Mesh, shader: Shader) {
        draw(mesh, shader, framebuffer = null)
    }

    /**
     * Draw a [Mesh with the specified [Shader to the given [Framebuffer].
     *
     * The [framebuffer] argument may be null, in which case the default framebuffer is used.
     */
    public fun draw(mesh: Mesh, shader: Shader, framebuffer: Framebuffer?) {
        useFramebuffer(framebuffer)
        shader.lowLevelUse()
        mesh.lowLevelDraw()
    }

    /**
     * Clear the given framebuffer.
     *
     * The framebuffer argument may be null, in which case the default framebuffer is cleared.
     */
    public fun clear(framebuffer: Framebuffer?, r: Float, g: Float, b: Float, a: Float) {
        useFramebuffer(framebuffer)
        GLES30.glClearColor(r, g, b, a)
        maybeThrowGLException("Failed to set clear color", "glClearColor")
        GLES30.glDepthMask(true)
        maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        maybeThrowGLException("Failed to clear framebuffer", "glClear")
    }

    internal fun getAssets(): AssetManager {
        return assetManager
    }

    private fun useFramebuffer(framebuffer: Framebuffer?) {
        val framebufferId: Int
        val viewportWidth: Int
        val viewportHeight: Int
        if (framebuffer == null) {
            framebufferId = 0
            viewportWidth = this.viewportWidth
            viewportHeight = this.viewportHeight
        } else {
            framebufferId = framebuffer.getFramebufferId()
            viewportWidth = framebuffer.width
            viewportHeight = framebuffer.height
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
        maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        maybeThrowGLException("Failed to set viewport dimensions", "glViewport")
    }

    companion object {
        private const val TAG: String = "SampleRender"

        /** Interface to be implemented for rendering callbacks. */
        public interface Renderer {
            /**
             * Called by [SampleRender] when the GL render surface is created.
             *
             * See [GLSurfaceView.Renderer.onSurfaceCreated].
             */
            public fun onSurfaceCreated(render: SampleRender)

            /**
             * Called by [SampleRender] when the GL render surface dimensions are changed.
             *
             * See [GLSurfaceView.Renderer.onSurfaceChanged].
             */
            public fun onSurfaceChanged(render: SampleRender, width: Int, height: Int)

            /**
             * Called by [SampleRender] when a GL frame is to be rendered.
             *
             * See [GLSurfaceView.Renderer.onDrawFrame].
             */
            public fun onDrawFrame(render: SampleRender)
        }
    }
}
