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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
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
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.wear.watchface.Renderer.CanvasRenderer
import androidx.wear.watchface.Renderer.GlesRenderer
import androidx.wear.watchface.style.UserStyleRepository
import java.nio.ByteBuffer

/**
 * Describes the type of [Canvas] a [CanvasRenderer] should request from a [SurfaceHolder].
 *
 * @hide
 */
@IntDef(
    value = [
        CanvasType.SOFTWARE,
        CanvasType.HARDWARE
    ]
)
public annotation class CanvasType {
    public companion object {
        /** A software canvas will be requested. */
        public const val SOFTWARE: Int = 0

        /**
         * A hardware canvas will be requested. This is usually faster than software rendering,
         * however it can sometimes increase battery usage by rendering at a higher frame rate.
         *
         * NOTE this is only supported on API level 26 and above. On lower API levels we fall back
         * to a software canvas.
         */
        public const val HARDWARE: Int = 1
    }
}

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

/** The base class for [CanvasRenderer] and [GlesRenderer]. */
public sealed class Renderer(
    /** The [SurfaceHolder] that [renderInternal] will draw into. */
    public val surfaceHolder: SurfaceHolder,

    /** The associated [UserStyleRepository]. */
    private val userStyleRepository: UserStyleRepository,

    /** The associated [WatchState]. */
    internal val watchState: WatchState,

    /**
     * The interval in milliseconds between frames in interactive [DrawMode]s. To render at 60hz
     * set to 16. Note when battery is low, the frame rate will be clamped to 10fps. Watch faces are
     * recommended to use lower frame rates if possible for better battery life. Variable frame
     * rates can also help preserve battery life, e.g. if a watch face has a short animation once
     * per second it can adjust the frame rate inorder to sleep when not animating.
     */
    @IntRange(from = 0, to = 10000)
    public var interactiveDrawModeUpdateDelayMillis: Long,
) {
    internal lateinit var watchFaceHostApi: WatchFaceHostApi

    init {
        surfaceHolder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    screenBounds = holder.surfaceFrame
                    centerX = screenBounds.exactCenterX()
                    centerY = screenBounds.exactCenterY()
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                }
            }
        )
    }

    /**
     * The bounds of the [SurfaceHolder] this Renderer renders into. Depending on the shape of the
     * device's screen not all of these pixels may be visible to the user (see
     * [WatchState.screenShape]).  Note also that API level 27+ devices draw indicators in the top
     * and bottom 24dp of the screen, avoid rendering anything important there.
     */
    public var screenBounds: Rect = surfaceHolder.surfaceFrame
        private set

    /** The center x coordinate of the [SurfaceHolder] this Renderer renders into. */
    @Px
    public var centerX: Float = screenBounds.exactCenterX()
        private set

    /** The center y coordinate of the [SurfaceHolder] this Renderer renders into. */
    @Px
    public var centerY: Float = screenBounds.exactCenterY()
        private set

    /** The current [RenderParameters]. Updated before every onDraw call. */
    public var renderParameters: RenderParameters = RenderParameters.DEFAULT_INTERACTIVE
        /** @hide */
        internal set(value) {
            if (value != field) {
                field = value
                onRenderParametersChanged(value)
            }
        }

    /** Allows the renderer to finalize init after the child class's constructor has finished. */
    internal open fun onPostCreate() {}

    /** Called when the Renderer is destroyed. */
    @UiThread
    public open fun onDestroy() {
    }

    /**
     * Renders the watch face into the [surfaceHolder] using the current [renderParameters]
     * with the user style specified by the [userStyleRepository].
     *
     * @param calendar The Calendar to use when rendering the watch face
     * @return A [Bitmap] containing a screenshot of the watch face
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun renderInternal(calendar: Calendar)

    /**
     * Renders the watch face into a Bitmap with the user style specified by the
     * [userStyleRepository].
     *
     * @param calendar The Calendar to use when rendering the watch face
     * @param renderParameters The [RenderParameters] to use when rendering the watch face
     * @return A [Bitmap] containing a screenshot of the watch face
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun takeScreenshot(
        calendar: Calendar,
        renderParameters: RenderParameters
    ): Bitmap

    /**
     * Called when the [RenderParameters] has been updated. Will always be called before the first
     * call to [CanvasRenderer.render] or [GlesRenderer.render].
     */
    @UiThread
    protected open fun onRenderParametersChanged(renderParameters: RenderParameters) {
    }

    /**
     * This method is used for accessibility support to describe the portion of the screen
     * containing  the main clock element. By default we assume this is contained in the central
     * half of the watch face. Watch faces should override this to return the correct bounds for
     * the main clock element.
     *
     * @return A [Rect] describing the bounds of the watch faces' main clock element
     */
    @UiThread
    public open fun getMainClockElementBounds(): Rect {
        val quarterX = centerX / 2
        val quarterY = centerY / 2
        return Rect(
            (centerX - quarterX).toInt(), (centerY - quarterY).toInt(),
            (centerX + quarterX).toInt(), (centerY + quarterY).toInt()
        )
    }

    /**
     * The system periodically (at least once per minute) calls onTimeTick() to trigger a display
     * update. If the watch face needs to animate with an interactive frame rate, calls to
     * invalidate must be scheduled. This method controls whether or not we should do that and if
     * shouldAnimate returns true we inhibit entering [DrawMode.AMBIENT].
     *
     * By default we remain at an interactive frame rate when the watch face is visible and we're
     * not in ambient mode. Watchfaces with animated transitions for entering ambient mode may
     * need to override this to ensure they play smoothly.
     *
     * @return Whether we should schedule an onDraw call to maintain an interactive frame rate
     */
    @UiThread
    public open fun shouldAnimate(): Boolean =
        watchState.isVisible.value && !watchState.isAmbient.value

    /**
     * Schedules a call to either [CanvasRenderer.render] or [GlesRenderer.render] to draw the next
     * frame.
     */
    @UiThread
    public fun invalidate() {
        if (this::watchFaceHostApi.isInitialized) {
            watchFaceHostApi.invalidate()
        }
    }

    /**
     * Posts a message to schedule a call to [renderInternal] to draw the next frame. Unlike
     * [invalidate], this method is thread-safe and may be called on any thread.
     */
    public fun postInvalidate() {
        if (this::watchFaceHostApi.isInitialized) {
            watchFaceHostApi.getHandler().post { watchFaceHostApi.invalidate() }
        }
    }

    /**
     * Watch faces that require [Canvas] rendering should extend their [Renderer] from this class.
     */
    public abstract class CanvasRenderer(
        /**
         * The [SurfaceHolder] from which a [Canvas] to will be obtained and passed into [render].
         */
        surfaceHolder: SurfaceHolder,

        /** The watch face's associated [UserStyleRepository]. */
        userStyleRepository: UserStyleRepository,

        /** The watch face's associated [WatchState]. */
        watchState: WatchState,

        /** The type of canvas to request. */
        @CanvasType private val canvasType: Int,

        /**
         * The interval in milliseconds between frames in interactive [DrawMode]s. To render at 60hz
         * set to 16. Note when battery is low, the frame rate will be clamped to 10fps. Watch faces
         * are recommended to use lower frame rates if possible for better battery life. Variable
         * frame  rates can also help preserve battery life, e.g. if a watch face has a short
         * animation once per second it can adjust the frame rate inorder to sleep when not
         * animating.
         */
        @IntRange(from = 0, to = 10000)
        interactiveDrawModeUpdateDelayMillis: Long
    ) : Renderer(
        surfaceHolder,
        userStyleRepository,
        watchState,
        interactiveDrawModeUpdateDelayMillis
    ) {

        @SuppressWarnings("UnsafeNewApiCall") // We check if the SDK is new enough.
        internal override fun renderInternal(
            calendar: Calendar
        ) {
            val canvas = (
                if (canvasType == CanvasType.HARDWARE && android.os.Build.VERSION.SDK_INT >= 26) {
                    surfaceHolder.lockHardwareCanvas() // Requires API level 26.
                } else {
                    surfaceHolder.lockCanvas()
                }
                ) ?: return
            try {
                if (watchState.isVisible.value) {
                    render(canvas, surfaceHolder.surfaceFrame, calendar)
                } else {
                    canvas.drawColor(Color.BLACK)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        /** {@inheritDoc} */
        internal override fun takeScreenshot(
            calendar: Calendar,
            renderParameters: RenderParameters
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(
                screenBounds.width(),
                screenBounds.height(),
                Bitmap.Config.ARGB_8888
            )
            val prevRenderParameters = this.renderParameters
            this.renderParameters = renderParameters
            render(Canvas(bitmap), screenBounds, calendar)
            this.renderParameters = prevRenderParameters
            return bitmap
        }

        /**
         * Sub-classes should override this to implement their rendering logic which should respect
         * the current [DrawMode]. For correct functioning the CanvasRenderer must use the supplied
         * [Calendar] in favor of any other ways of getting the time.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         *     the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun render(
            canvas: Canvas,
            bounds: Rect,
            calendar: Calendar
        )
    }

    /**
     * Watch faces that require [GLES20] rendering should extend their [Renderer] from this class.
     */
    public abstract class GlesRenderer @JvmOverloads constructor(
        /** The [SurfaceHolder] whose [android.view.Surface] [render] will draw into. */
        surfaceHolder: SurfaceHolder,

        /** The associated [UserStyleRepository]. */
        userStyleRepository: UserStyleRepository,

        /** The associated [WatchState]. */
        watchState: WatchState,

        /**
         * The interval in milliseconds between frames in interactive [DrawMode]s. To render at 60hz
         * set to 16. Note when battery is low, the frame rate will be clamped to 10fps. Watch faces
         * are recommended to use lower frame rates if possible for better battery life. Variable
         * frame rates can also help preserve battery life, e.g. if a watch face has a short
         * animation once per second it can adjust the frame rate inorder to sleep when not
         * animating.
         */
        @IntRange(from = 0, to = 10000)
        interactiveDrawModeUpdateDelayMillis: Long,

        /**
         * Attributes for [EGL14.eglChooseConfig]. By default this selects an RGBA8888 back buffer.
         */
        private val eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,

        /**
         * The attributes to be passed to [EGL14.eglCreateWindowSurface]. By default this is empty.
         */
        private val eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST
    ) : Renderer(
        surfaceHolder,
        userStyleRepository,
        watchState,
        interactiveDrawModeUpdateDelayMillis
    ) {
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
         * Sets our GL context to be the current one. This method *must* be called before any OpenGL
         * APIs are used.
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
        public open fun onGlContextCreated() {
        }

        /**
         * Called when a new GL surface is created. It's safe to use GL APIs in this method.
         *
         * @param width width of surface in pixels
         * @param height height of surface in pixels
         */
        @UiThread
        public open fun onGlSurfaceCreated(width: Int, height: Int) {
        }

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
            GLES20.glReadPixels(
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                pixelBuf
            )
            // The image is flipped when using read pixels because the first pixel in the OpenGL
            // buffer is in bottom left.
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
                System.arraycopy(
                    tmp,
                    0,
                    buffer.array(),
                    buffer.limit() - buffer.position(),
                    width * 4
                )
            }
            buffer.rewind()
        }

        /**
         * Sub-classes should override this to implement their rendering logic which should respect
         * the current [DrawMode]. For correct functioning the GlesRenderer must use the supplied
         * [Calendar] in favor of any other ways of getting the time.
         *
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun render(calendar: Calendar)
    }
}
