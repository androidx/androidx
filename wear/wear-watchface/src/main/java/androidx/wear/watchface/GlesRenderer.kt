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

package androidx.wear.watchface

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.wear.watchface.style.UserStyleRepository

import java.nio.ByteBuffer

internal val EGL_CONFIG_ATTRIB_LIST = intArrayOf(
    EGL14.EGL_RENDERABLE_TYPE,
    EGL14.EGL_OPENGL_ES2_BIT,
    EGL14.EGL_RED_SIZE,
    8,
    EGL14.EGL_GREEN_SIZE,
    8,
    EGL14.EGL_BLUE_SIZE,
    8,
    EGL14.EGL_ALPHA_SIZE,
    8,
    EGL14.EGL_NONE
)

private val EGL_CONTEXT_ATTRIB_LIST =
    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)

internal val EGL_SURFACE_ATTRIB_LIST = intArrayOf(EGL14.EGL_NONE)

/**
 * Watch faces that require [GLES20] rendering should extend their [Renderer] from this
 * class.
 */
public abstract class GlesRenderer @JvmOverloads constructor(
    /** The [SurfaceHolder] that [render] will draw into. */
    surfaceHolder: SurfaceHolder,

    /** The associated [UserStyleRepository]. */
    userStyleRepository: UserStyleRepository,

    /** The associated [WatchState]. */
    watchState: WatchState,

    /** Attributes for [EGL14.eglChooseConfig]. By default this selects an RGBAB8888 back buffer. */
    private val eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,

    /** The attributes to be passed to [EGL14.eglCreateWindowSurface]. By default this is empty. */
    private val eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST
) : Renderer(surfaceHolder, userStyleRepository, watchState) {
    /** @hide */
    private companion object {
        private const val TAG = "Gles2WatchFace"
    }

    private var eglDisplay: EGLDisplay? = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY).apply {
        if (this == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay returned EGL_NO_DISPLAY")
        }
        // Initialize the display. The major and minor version numbers are passed back.
        val version = IntArray(2)
        if (!EGL14.eglInitialize(this, version, 0, version, 1)) {
            throw RuntimeException("eglInitialize failed")
        }
    }

    private var eglConfig: EGLConfig = chooseEglConfig(eglDisplay!!)

    @SuppressWarnings("SyntheticAccessor")
    private var eglContext: EGLContext? = EGL14.eglCreateContext(
        eglDisplay,
        eglConfig,
        EGL14.EGL_NO_CONTEXT,
        EGL_CONTEXT_ATTRIB_LIST,
        0
    )

    init {
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext failed")
        }
    }

    private var eglSurface: EGLSurface? = null
    private var calledOnGlContextCreated = false

    /**
     * Chooses the EGLConfig to use.
     * @throws RuntimeException if [EGL14.eglChooseConfig] fails
     */
    private fun chooseEglConfig(eglDisplay: EGLDisplay): EGLConfig {
        val numEglConfigs = IntArray(1)
        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                eglConfigAttribList,
                0,
                eglConfigs,
                0,
                eglConfigs.size,
                numEglConfigs,
                0
            )
        ) {
            throw RuntimeException("eglChooseConfig failed")
        }
        if (numEglConfigs[0] == 0) {
            throw RuntimeException("no matching EGL configs")
        }
        return eglConfigs[0]!!
    }

    private fun createWindowSurface(width: Int, height: Int) {
        if (eglSurface != null) {
            if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                Log.w(TAG, "eglDestroySurface failed")
            }
        }
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            eglConfig,
            surfaceHolder.surface,
            eglSurfaceAttribList,
            0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("eglCreateWindowSurface failed")
        }

        makeContextCurrent()
        GLES20.glViewport(0, 0, width, height)
        if (!calledOnGlContextCreated) {
            calledOnGlContextCreated = true
            onGlContextCreated()
        }
        onGlSurfaceCreated(width, height)
    }

    @CallSuper
    override fun onDestroy() {
        if (eglSurface != null) {
            if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                Log.w(TAG, "eglDestroySurface failed")
            }
            eglSurface = null
        }
        if (eglContext != null) {
            if (!EGL14.eglDestroyContext(eglDisplay, eglContext)) {
                Log.w(TAG, "eglDestroyContext failed")
            }
            eglContext = null
        }
        if (eglDisplay != null) {
            if (!EGL14.eglTerminate(eglDisplay)) {
                Log.w(TAG, "eglTerminate failed")
            }
            eglDisplay = null
        }
    }

    /**
     * Sets our GL context to be the current one. This method *must* be called before any
     * OpenGL APIs are used.
     */
    private fun makeContextCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    internal override fun onPostCreate() {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("SyntheticAccessor")
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                createWindowSurface(width, height)
            }

            @SuppressLint("SyntheticAccessor")
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                    Log.w(TAG, "eglDestroySurface failed")
                }
                eglSurface = null
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
            }
        })

        createWindowSurface(
            surfaceHolder.surfaceFrame.width(),
            surfaceHolder.surfaceFrame.height()
        )
    }

    /** Called when a new GL context is created. It's safe to use GL APIs in this method. */
    @UiThread
    public open fun onGlContextCreated() {}

    /**
     * Called when a new GL surface is created. It's safe to use GL APIs in this method.
     *
     * @param width width of surface in pixels
     * @param height height of surface in pixels
     */
    @UiThread
    public open fun onGlSurfaceCreated(width: Int, height: Int) {}

    internal override fun renderInternal(
        calendar: Calendar
    ) {
        makeContextCurrent()
        render(calendar)
        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            Log.w(TAG, "eglSwapBuffers failed")
        }
    }

    /** {@inheritDoc} */
    internal override fun takeScreenshot(
        calendar: Calendar,
        renderParameters: RenderParameters
    ): Bitmap {
        val width = screenBounds.width()
        val height = screenBounds.height()
        val pixelBuf = ByteBuffer.allocateDirect(width * height * 4)
        makeContextCurrent()
        val prevRenderParameters = this.renderParameters
        this.renderParameters = renderParameters
        render(calendar)
        this.renderParameters = prevRenderParameters
        GLES20.glFinish()
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf)
        // The image is flipped when using read pixels because the first pixel in the OpenGL buffer
        // is in bottom left.
        verticalFlip(pixelBuf, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(pixelBuf)
        return bitmap
    }

    private fun verticalFlip(
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        var i = 0
        val tmp = ByteArray(width * 4)
        while (i++ < height / 2) {
            buffer[tmp]
            System.arraycopy(
                buffer.array(),
                buffer.limit() - buffer.position(),
                buffer.array(),
                buffer.position() - width * 4,
                width * 4
            )
            System.arraycopy(tmp, 0, buffer.array(), buffer.limit() - buffer.position(), width * 4)
        }
        buffer.rewind()
    }

    /**
     * Sub-classes should override this to implement their rendering logic which should respect
     * the current [DrawMode]. For correct functioning watch faces must use the supplied
     * [Calendar] and avoid using any other ways of getting the time.
     *
     * @param calendar The current [Calendar]
     */
    @UiThread
    public abstract fun render(calendar: Calendar)
}
