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
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
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
    }
}

interface SkiaRenderer {
    fun onInit()
    fun onRender(canvas: Canvas, width: Int, height: Int)
    fun onReshape(canvas: Canvas, width: Int, height: Int)
    fun onDispose()

    fun onMouseClicked(x: Int, y: Int, modifiers: Int)
    fun onMousePressed(x: Int, y: Int, modifiers: Int)
    fun onMouseReleased(x: Int, y: Int, modifiers: Int)
    fun onMouseDragged(x: Int, y: Int, modifiers: Int)

    fun onKeyTyped(char: Char)
    fun onKeyPressed(code: Int, char: Char)
    fun onKeyReleased(code: Int, char: Char)
}

class SkiaWindow(
    width: Int,
    height: Int,
    fps: Int = 0
) : JFrame() {
    companion object {
        init {
            JNI.loadLibrary("/", "skija")
            // Until https://github.com/Kotlin/kotlinx.coroutines/issues/2039 is resolved
            // we have to set this property manually for coroutines to work.
            System.getProperties().setProperty("kotlinx.coroutines.fast.service.loader", "false")
        }
    }

    val glCanvas: GLCanvas
    var animator: FPSAnimator? = null

    var renderer: SkiaRenderer? = null
    val VSYNC = false

    init {
        val profile = GLProfile.get(GLProfile.GL3)
        val capabilities = GLCapabilities(profile)
        // We cannot rely on double buffering.
        capabilities.doubleBuffered = false
        glCanvas = GLCanvas(capabilities)
        val skijaState = SkijaState()

        glCanvas.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                renderer!!.onMouseClicked(event.x, event.y, event.getModifiersEx())
            }
            override fun mousePressed(event: MouseEvent) {
                renderer!!.onMousePressed(event.x, event.y, event.getModifiersEx())
            }
            override fun mouseReleased(event: MouseEvent) {
                renderer!!.onMouseReleased(event.x, event.y, event.getModifiersEx())
            }
        })

        glCanvas.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                renderer!!.onMouseDragged(event.x, event.y, event.getModifiersEx())
            }
        })

        glCanvas.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                renderer!!.onKeyPressed(event.keyCode, event.keyChar)
            }
            override fun keyReleased(event: KeyEvent) {
                renderer!!.onKeyReleased(event.keyCode, event.keyChar)
            }
            override fun keyTyped(event: KeyEvent) {
                renderer!!.onKeyTyped(event.keyChar)
            }
        })

        glCanvas.addGLEventListener(object : GLEventListener {
            override fun reshape(
                drawable: GLAutoDrawable?,
                x: Int,
                y: Int,
                width: Int,
                height: Int
            ) {
                initSkija(glCanvas, skijaState, false)
                renderer!!.onReshape(skijaState.canvas!!, width, height)
            }

            override fun init(drawable: GLAutoDrawable?) {
                skijaState.context = Context.makeGL()
                initSkija(glCanvas, skijaState, false)
                renderer!!.onInit()
            }

            override fun dispose(drawable: GLAutoDrawable?) {
                renderer!!.onDispose()
            }

            override fun display(drawable: GLAutoDrawable?) {
                skijaState.apply {
                    val gl = drawable!!.gl!!
                    // drawable.swapBuffers()
                    canvas!!.clear(0xFFFFFFFFL)
                    gl.glBindTexture(GL.GL_TEXTURE_2D, textureId)
                    renderer!!.onRender(
                        canvas!!, glCanvas.width, glCanvas.height
                    )
                    context!!.flush()
                    gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
                    textureId = intBuf1[0]
                    if (VSYNC) gl.glFinish()
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
            val dpiX = glCanvas.nativeSurface.surfaceWidth.toFloat() / width
            val dpiY = glCanvas.nativeSurface.surfaceHeight.toFloat() / height
            if (VSYNC) glCanvas.gl.setSwapInterval(1)
            skijaState.clear()
            val intBuf1 = IntBuffer.allocate(1)
            glCanvas.gl.glGetIntegerv(GL.GL_DRAW_FRAMEBUFFER_BINDING, intBuf1)
            val fbId = intBuf1[0]
            renderTarget = BackendRenderTarget.newGL(
                (width * dpiX).toInt(),
                (height * dpiY).toInt(),
                0,
                8,
                fbId.toLong(),
                BackendRenderTarget.FramebufferFormat.GR_GL_RGBA8.toLong()
            )
            surface = Surface.makeFromBackendRenderTarget(
                context,
                renderTarget,
                Surface.Origin.BOTTOM_LEFT,
                Surface.ColorType.RGBA_8888
            )
            canvas = surface!!.canvas
            canvas!!.scale(dpiX, dpiY)
            if (reinitTexture) {
                glCanvas.gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
                skijaState.textureId = intBuf1[0]
            }
        }
    }
}
