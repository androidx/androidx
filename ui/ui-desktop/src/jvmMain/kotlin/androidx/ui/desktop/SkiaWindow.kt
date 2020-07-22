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

import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.runtime.dispatch.DesktopUiDispatcher
import androidx.compose.ui.text.platform.paragraphActualFactory
import androidx.compose.ui.text.platform.paragraphIntrinsicsActualFactory
import com.jogamp.opengl.GL
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import org.jetbrains.skija.BackendRenderTarget
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.ColorSpace
import org.jetbrains.skija.Context
import org.jetbrains.skija.FramebufferFormat
import org.jetbrains.skija.Library
import org.jetbrains.skija.Surface
import org.jetbrains.skija.SurfaceColorFormat
import org.jetbrains.skija.SurfaceOrigin
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.nio.IntBuffer
import javax.swing.JDialog
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

class Window : JFrame, SkiaFrame {
    companion object {
        init {
            initCompose()
        }
    }

    override val parent: AppFrame
    override val glCanvas: GLCanvas
    override var renderer: SkiaRenderer? = null
    override val vsync = true

    constructor(width: Int, height: Int, parent: AppFrame) : super() {
        this.parent = parent
        setSize(width, height)
    }

    init {
        glCanvas = initCanvas(this, vsync)
        glCanvas.setSize(width, height)
        contentPane.add(glCanvas)
        size = contentPane.preferredSize
    }
}

class Dialog : JDialog, SkiaFrame {
    @OptIn(androidx.compose.ui.text.android.InternalPlatformTextApi::class)
    companion object {
        init {
            Library.load("/", "skija")
            // Until https://github.com/Kotlin/kotlinx.coroutines/issues/2039 is resolved
            // we have to set this property manually for coroutines to work.
            System.getProperties().setProperty("kotlinx.coroutines.fast.service.loader", "false")

            @Suppress("DEPRECATION_ERROR")
            paragraphIntrinsicsActualFactory = ::DesktopParagraphIntrinsics
            @Suppress("DEPRECATION_ERROR")
            paragraphActualFactory = ::DesktopParagraph
        }
    }

    override val parent: AppFrame
    override val glCanvas: GLCanvas
    override var renderer: SkiaRenderer? = null
    override val vsync = true

    constructor(
        attached: JFrame?,
        width: Int,
        height: Int,
        parent: AppFrame
    ) : super(attached, true) {
        this.parent = parent
        setSize(width, height)
    }

    init {
        glCanvas = initCanvas(this, vsync)
        glCanvas.setSize(width, height)
        contentPane.add(glCanvas)
        size = contentPane.preferredSize
    }
}

internal interface SkiaFrame {
    val parent: AppFrame
    val glCanvas: GLCanvas
    var renderer: SkiaRenderer?
    val vsync: Boolean

    fun close() {
        glCanvas.destroy()
    }
}

private fun initSkija(
    glCanvas: GLCanvas,
    skijaState: SkijaState,
    vsync: Boolean,
    reinitTexture: Boolean
) {
    with(skijaState) {
        val width = glCanvas.width
        val height = glCanvas.height
        val dpiX = glCanvas.nativeSurface.surfaceWidth.toFloat() / width
        val dpiY = glCanvas.nativeSurface.surfaceHeight.toFloat() / height
        if (vsync) glCanvas.gl.setSwapInterval(1)
        skijaState.clear()
        val intBuf1 = IntBuffer.allocate(1)
        glCanvas.gl.glGetIntegerv(GL.GL_DRAW_FRAMEBUFFER_BINDING, intBuf1)
        val fbId = intBuf1[0]
        renderTarget = BackendRenderTarget.makeGL(
            (width * dpiX).toInt(),
            (height * dpiY).toInt(),
            0,
            8,
            fbId,
            FramebufferFormat.GR_GL_RGBA8
        )
        surface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB()
        )
        canvas = surface!!.canvas
        canvas!!.scale(dpiX, dpiY)
        if (reinitTexture) {
            glCanvas.gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
            skijaState.textureId = intBuf1[0]
        }
    }
}

// Simple FPS tracker for debug purposes
internal class FPSTracker {
    private var t0 = 0L
    private val times = DoubleArray(155)
    private var timesIdx = 0

    fun track() {
        val t1 = System.nanoTime()
        times[timesIdx] = (t1 - t0) / 1000000.0
        t0 = t1
        timesIdx = (timesIdx + 1) % times.size
        println("FPS: ${1000 / times.takeWhile { it > 0 }.average()}")
    }
}

private fun initCanvas(frame: SkiaFrame, vsync: Boolean = false): GLCanvas {
    val profile = GLProfile.get(GLProfile.GL3)
    val capabilities = GLCapabilities(profile)
    // We cannot rely on double buffering.
    capabilities.doubleBuffered = false
    val glCanvas = GLCanvas(capabilities)

    val skijaState = SkijaState()

    glCanvas.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            frame.renderer!!.onMouseClicked(event.x, event.y, event.getModifiersEx())
        }

        override fun mousePressed(event: MouseEvent) {
            frame.renderer!!.onMousePressed(event.x, event.y, event.getModifiersEx())
        }

        override fun mouseReleased(event: MouseEvent) {
            frame.renderer!!.onMouseReleased(event.x, event.y, event.getModifiersEx())
        }
    })
    glCanvas.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) {
            frame.renderer!!.onMouseDragged(event.x, event.y, event.getModifiersEx())
        }
    })
    glCanvas.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            frame.renderer!!.onKeyPressed(event.keyCode, event.keyChar)
        }

        override fun keyReleased(event: KeyEvent) {
            frame.renderer!!.onKeyReleased(event.keyCode, event.keyChar)
        }

        override fun keyTyped(event: KeyEvent) {
            frame.renderer!!.onKeyTyped(event.keyChar)
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
            initSkija(glCanvas, skijaState, vsync, false)
            frame.renderer!!.onReshape(skijaState.canvas!!, width, height)
        }

        override fun init(drawable: GLAutoDrawable?) {
            skijaState.context = Context.makeGL()
            initSkija(glCanvas, skijaState, vsync, false)
            frame.renderer!!.onInit()
        }

        override fun dispose(drawable: GLAutoDrawable?) {
            frame.renderer!!.onDispose()
            AppManager.removeWindow(frame.parent)
        }

        override fun display(drawable: GLAutoDrawable) {
            skijaState.apply {
                val gl = drawable.gl!!
                canvas!!.clear(0xFFFFFFF)
                gl.glBindTexture(GL.GL_TEXTURE_2D, textureId)
                frame.renderer!!.onRender(
                    canvas!!, glCanvas.width, glCanvas.height
                )
                context!!.flush()
                gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf1)
                textureId = intBuf1[0]
                if (vsync) gl.glFinish()
            }
        }
    })

    return glCanvas
}

internal class DesktopAnimationClock(fps: Int, val dispatcher: DesktopUiDispatcher) :
    BaseAnimationClock() {
    val delay = 1_000 / fps

    @Volatile
    private var scheduled = false
    private fun frameCallback(time: Long) {
        scheduled = false
        dispatchTime(time / 1000000)
    }

    override fun subscribe(observer: AnimationClockObserver) {
        super.subscribe(observer)
        scheduleIfNeeded()
    }

    override fun dispatchTime(frameTimeMillis: Long) {
        super.dispatchTime(frameTimeMillis)
        scheduleIfNeeded()
    }

    private fun scheduleIfNeeded() {
        when {
            scheduled -> return
            !hasObservers() -> return
            else -> {
                scheduled = true
                dispatcher.scheduleCallbackWithDelay(delay, ::frameCallback)
            }
        }
    }
}
