/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.desktop

import com.jogamp.opengl.GL
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.FPSAnimator
import org.jetbrains.skija.BackendRenderTarget
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Context
import org.jetbrains.skija.JNI
import org.jetbrains.skija.Surface
import java.nio.IntBuffer
import javax.swing.JFrame

private class SkijaState {
    var context: Context? = null
    var renderTarget: BackendRenderTarget? = null
    var surface: Surface? = null
    var canvas: Canvas? = null
    var textureId: Int = 0
    val intBuf1 = IntBuffer.allocate(1)

    fun clear() {
        if (surface != null) {
            surface!!.close()
        }
        if (renderTarget != null) {
            renderTarget!!.close()
        }
        textureId = 0
    }
}

interface SkiaRenderer {
    fun onInit()
    fun onRender(canvas: Canvas, width: Int, height: Int)
    fun onReshape(width: Int, height: Int)
    fun onDispose()
}

class SkiaWindow(
    width: Int,
    height: Int,
    fps: Int = 0
) : JFrame() {
    companion object {
        init {
            JNI.loadLibrary("/", "skija")
        }
    }

    val glCanvas: GLCanvas
    var animator: FPSAnimator? = null

    var renderer: SkiaRenderer? = null

    init {
        val profile = GLProfile.get(GLProfile.GL3)
        val capabilities = GLCapabilities(profile)
        glCanvas = GLCanvas(capabilities)
        capabilities.doubleBuffered = true
        val skijaState = SkijaState()
        glCanvas.autoSwapBufferMode = true

        glCanvas.addGLEventListener(object : GLEventListener {
            override fun reshape(
                drawable: GLAutoDrawable?,
                x: Int,
                y: Int,
                width: Int,
                height: Int
            ) {
                initSkija(glCanvas, skijaState, false)
                renderer!!.onReshape(width, height)
            }

            override fun init(drawable: GLAutoDrawable?) {
                initSkija(glCanvas, skijaState, true)
                renderer!!.onInit()
            }

            override fun dispose(drawable: GLAutoDrawable?) {
                renderer!!.onDispose()
            }

            override fun display(drawable: GLAutoDrawable?) {
                skijaState.apply {
                    val gl = drawable!!.gl!!
                    gl.glBindTexture(GL.GL_TEXTURE_2D, textureId)
                    canvas!!.clear(0xFFFFFFFF)
                    renderer!!.onRender(
                        canvas!!, glCanvas.width, glCanvas.height
                    )
                    context!!.flush()
                    gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
                    textureId = intBuf1[0]
                }
            }
        })

        glCanvas.setSize(width, height)

        setFps(fps)

        contentPane.add(glCanvas)
        size = contentPane.preferredSize
    }

    fun setFps(fps: Int) {
        animator?.stop()
        animator = if (fps > 0) {
            FPSAnimator(fps).also {
                it.add(glCanvas)
                it.start()
            }
        } else {
            null
        }
    }

    private fun initSkija(glCanvas: GLCanvas, skijaState: SkijaState, reinitTexture: Boolean) {
        with(skijaState) {
            val width = glCanvas.width
            val height = glCanvas.height
            val dpi = glCanvas.width.toFloat() / width
            skijaState.clear()
            val intBuf1 = IntBuffer.allocate(1)
            glCanvas.gl.glGetIntegerv(GL.GL_DRAW_FRAMEBUFFER_BINDING, intBuf1)
            val fbId = intBuf1[0]
            renderTarget = BackendRenderTarget.newGL(
                (width * dpi).toInt(),
                (height * dpi).toInt(),
                0,
                8,
                fbId.toLong(),
                BackendRenderTarget.FramebufferFormat.GR_GL_RGBA8.toLong()
            )
            context = Context.makeGL()
            surface = Surface.makeFromBackendRenderTarget(
                context,
                renderTarget,
                Surface.Origin.BOTTOM_LEFT,
                Surface.ColorType.RGBA_8888
            )
            canvas = surface!!.canvas
            canvas!!.scale(dpi, dpi)
            if (reinitTexture) {
                glCanvas.gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
                skijaState.textureId = intBuf1[0]
            }
        }
    }
}
