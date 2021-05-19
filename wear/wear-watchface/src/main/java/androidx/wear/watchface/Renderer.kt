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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.icu.util.Calendar
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.CallSuper
import androidx.annotation.DoNotInline
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.Renderer.CanvasRenderer
import androidx.wear.watchface.Renderer.GlesRenderer
import androidx.wear.watchface.style.CurrentUserStyleRepository
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

private val HIGHLIGHT_LAYER_COMPOSITE_PAINT = Paint().apply {
    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
}

/**
 * The base class for [CanvasRenderer] and [GlesRenderer].
 *
 * @param surfaceHolder The [SurfaceHolder] that [renderInternal] will draw into.
 * @param currentUserStyleRepository The associated [CurrentUserStyleRepository].
 * @param watchState The associated [WatchState].
 * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
 * interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame rate
 * will be clamped to 10fps. Watch faces are recommended to use lower frame rates if possible for
 * better battery life. Variable frame rates can also help preserve battery life, e.g. if a watch
 * face has a short animation once per second it can adjust the frame rate inorder to sleep when
 * not animating.
 */
public sealed class Renderer(
    public val surfaceHolder: SurfaceHolder,
    private val currentUserStyleRepository: CurrentUserStyleRepository,
    internal val watchState: WatchState,
    @IntRange(from = 0, to = 60000)
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
     * [Configuration.isScreenRound]).  Note also that API level 27+ devices draw indicators in the
     * top and bottom 24dp of the screen, avoid rendering anything important there.
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

    /**
     * Accessibility [ContentDescriptionLabel] for any rendered watch face elements other than the
     * time and [Complication]s which are generated automatically.
     *
     * The [Int] in the `Pair<Int, ContentDescriptionLabel>` is used to sort the
     * [ContentDescriptionLabel]s. Note the time piece has an accessibility traversal index of -1
     * and each complication's index is defined by its [Complication.accessibilityTraversalIndex].
     */
    public var additionalContentDescriptionLabels:
        Collection<Pair<Int, ContentDescriptionLabel>> = emptyList()
            set(value) {
                field = value
                for (pair in value) {
                    require(pair.first >= 0) {
                        "Each accessibility label index in additionalContentDescriptionLabels " +
                            "must be >= 0"
                    }
                }
                watchFaceHostApi.updateContentDescriptionLabels()
            }

    /** Called when the Renderer is destroyed. */
    @UiThread
    public open fun onDestroy() {
    }

    /**
     * Renders the watch face into the [surfaceHolder] using the current [renderParameters]
     * with the user style specified by the [currentUserStyleRepository].
     *
     * @param calendar The Calendar to use when rendering the watch face
     * @return A [Bitmap] containing a screenshot of the watch face
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun renderInternal(calendar: Calendar)

    /**
     * Renders the watch face into a Bitmap with the user style specified by the
     * [currentUserStyleRepository].
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
     * Posts a message to schedule a call to either [CanvasRenderer.render] or [GlesRenderer.render]
     * to draw the next frame. Unlike [invalidate], this method is thread-safe and may be called
     * on any thread.
     */
    public fun postInvalidate() {
        if (this::watchFaceHostApi.isInitialized) {
            watchFaceHostApi.getHandler().post { watchFaceHostApi.invalidate() }
        }
    }

    @UiThread
    internal abstract fun dump(writer: IndentingPrintWriter)

    /**
     * Watch faces that require [Canvas] rendering should extend their [Renderer] from this class.
     *
     * @param surfaceHolder The [SurfaceHolder] from which a [Canvas] to will be obtained and passed
     * into [render].
     * @param currentUserStyleRepository The watch face's associated [CurrentUserStyleRepository].
     * @param watchState The watch face's associated [WatchState].
     * @param canvasType The type of canvas to request.
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     * interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     * rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     * possible for better battery life. Variable frame rates can also help preserve battery
     * life, e.g. if a watch face has a short animation once per second it can adjust the frame
     * rate inorder to sleep when not animating.
     */
    public abstract class CanvasRenderer(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @CanvasType private val canvasType: Int,
        @IntRange(from = 0, to = 60000)
        interactiveDrawModeUpdateDelayMillis: Long
    ) : Renderer(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        interactiveDrawModeUpdateDelayMillis
    ) {
        @RequiresApi(26)
        private object Api26Impl {
            @JvmStatic
            @DoNotInline
            fun callLockHardwareCanvas(surfaceHolder: SurfaceHolder): Canvas? =
                surfaceHolder.lockHardwareCanvas()
        }

        internal override fun renderInternal(
            calendar: Calendar
        ) {
            val canvas = (
                if (canvasType == CanvasType.HARDWARE && Build.VERSION.SDK_INT >= 26) {
                    Api26Impl.callLockHardwareCanvas(surfaceHolder)
                } else {
                    surfaceHolder.lockCanvas()
                }
                ) ?: return
            try {
                if (watchState.isVisible.value) {
                    renderAndComposite(canvas, calendar)
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
        ): Bitmap = TraceEvent("CanvasRenderer.takeScreenshot").use {
            val bitmap = Bitmap.createBitmap(
                screenBounds.width(),
                screenBounds.height(),
                Bitmap.Config.ARGB_8888
            )
            val prevRenderParameters = this.renderParameters
            this.renderParameters = renderParameters
            renderAndComposite(Canvas(bitmap), calendar)
            this.renderParameters = prevRenderParameters
            return bitmap
        }

        private fun renderAndComposite(canvas: Canvas, calendar: Calendar) {
            // Usually renderParameters.watchFaceWatchFaceLayers will be non-empty.
            if (renderParameters.watchFaceLayers.isNotEmpty()) {
                render(canvas, screenBounds, calendar)

                // Render and composite the HighlightLayer
                if (renderParameters.highlightLayer != null) {
                    val highlightLayer = Bitmap.createBitmap(
                        screenBounds.width(),
                        screenBounds.height(),
                        Bitmap.Config.ARGB_8888
                    )
                    renderHighlightLayer(Canvas(highlightLayer), screenBounds, calendar)
                    canvas.drawBitmap(highlightLayer, 0f, 0f, HIGHLIGHT_LAYER_COMPOSITE_PAINT)
                    highlightLayer.recycle()
                }
            } else {
                require(renderParameters.highlightLayer != null) {
                    "We don't support empty renderParameters.watchFaceWatchFaceLayers without a " +
                        "non-null renderParameters.highlightLayer"
                }
                renderHighlightLayer(canvas, screenBounds, calendar)
            }
        }

        /**
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Any highlights due to
         * [RenderParameters.highlightLayer] should be rendered by [renderHighlightLayer] instead
         * where possible. For correct behavior this function must use the supplied [Calendar]
         * in favor of any other ways of getting the time.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         * the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun render(
            canvas: Canvas,
            bounds: Rect,
            calendar: Calendar
        )

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear [canvas] to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [Calendar] in favor of any other ways of getting the time.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         * the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            calendar: Calendar
        )

        internal override fun dump(writer: IndentingPrintWriter) {
            writer.println("CanvasRenderer:")
            writer.increaseIndent()
            writer.println("canvasType=$canvasType")
            writer.println("screenBounds=$screenBounds")
            writer.println(
                "interactiveDrawModeUpdateDelayMillis=$interactiveDrawModeUpdateDelayMillis"
            )
            writer.println("shouldAnimate=${shouldAnimate()}")
            renderParameters.dump(writer)
            writer.decreaseIndent()
        }
    }

    /**
     * Watch faces that require [GLES20] rendering should extend their [Renderer] from this class.
     * Before passing to the [WatchFace] constructor [initOpenGlContext] must be called.
     *
     * @param surfaceHolder The [SurfaceHolder] whose [android.view.Surface] [render] will draw
     * into.
     * @param currentUserStyleRepository The associated [CurrentUserStyleRepository].
     * @param watchState The associated [WatchState].
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     * interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     * rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     * possible for better battery life. Variable frame rates can also help preserve battery life,
     * e.g. if a watch face has a short animation once per second it can adjust the frame rate
     * inorder to sleep when not animating.
     * @param eglConfigAttribList Attributes for [EGL14.eglChooseConfig]. By default this selects an
     * RGBA8888 back buffer.
     * @param eglSurfaceAttribList The attributes to be passed to [EGL14.eglCreateWindowSurface]. By
     * default this is empty.
     * @throws [GlesException] If any GL calls fail during initialization.
     */
    public abstract class GlesRenderer @Throws(GlesException::class) @JvmOverloads constructor(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @IntRange(from = 0, to = 60000)
        interactiveDrawModeUpdateDelayMillis: Long,
        private val eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,
        private val eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST
    ) : Renderer(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        interactiveDrawModeUpdateDelayMillis
    ) {
        /** @hide */
        private companion object {
            private const val TAG = "Gles2WatchFace"
        }

        /** Exception thrown if a GL call fails */
        public class GlesException(message: String) : Exception(message)

        /** The GlesRenderer's [EGLDisplay]. */
        public var eglDisplay: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY).apply {
            if (this == EGL14.EGL_NO_DISPLAY) {
                throw GlesException("eglGetDisplay returned EGL_NO_DISPLAY")
            }
            // Initialize the display. The major and minor version numbers are passed back.
            val version = IntArray(2)
            if (!EGL14.eglInitialize(this, version, 0, version, 1)) {
                throw GlesException("eglInitialize failed")
            }
        }

        /** The GlesRenderer's [EGLConfig]. */
        public var eglConfig: EGLConfig = chooseEglConfig(eglDisplay)

        /** The GlesRenderer's [EGLContext]. */
        @SuppressWarnings("SyntheticAccessor")
        public var eglContext: EGLContext = EGL14.eglCreateContext(
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

        private lateinit var eglSurface: EGLSurface
        private var calledOnGlContextCreated = false
        private val renderBufferTexture by lazy {
            RenderBufferTexture(
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }
        internal var initDone = false

        /**
         * Chooses the EGLConfig to use.
         * @throws [GlesException] if [EGL14.eglChooseConfig] fails
         */
        @Throws(GlesException::class)
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
                throw GlesException("eglChooseConfig failed")
            }
            if (numEglConfigs[0] == 0) {
                throw GlesException("no matching EGL configs")
            }
            return eglConfigs[0]!!
        }

        @Throws(GlesException::class)
        private fun createWindowSurface(width: Int, height: Int) {
            if (this::eglSurface.isInitialized) {
                if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                    Log.w(TAG, "eglDestroySurface failed")
                }
            }
            eglSurface = if (watchState.isHeadless) {
                // Headless instances have a fake surfaceHolder so fall back to a Pbuffer.
                EGL14.eglCreatePbufferSurface(
                    eglDisplay,
                    eglConfig,
                    intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE),
                    0
                )
            } else {
                EGL14.eglCreateWindowSurface(
                    eglDisplay,
                    eglConfig,
                    surfaceHolder.surface,
                    eglSurfaceAttribList,
                    0
                )
            }
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                throw GlesException("eglCreateWindowSurface failed")
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
            if (this::eglSurface.isInitialized) {
                if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                    Log.w(TAG, "eglDestroySurface failed")
                }
            }
            if (!EGL14.eglDestroyContext(eglDisplay, eglContext)) {
                Log.w(TAG, "eglDestroyContext failed")
            }
            if (!EGL14.eglTerminate(eglDisplay)) {
                Log.w(TAG, "eglTerminate failed")
            }
        }

        /**
         * Sets our GL context to be the current one. The library does this on your behalf before
         * calling [onGlContextCreated] or [render]. If you need to make any OpenGL calls outside
         * those functions, this method *must* be called first.
         *
         * @throws [IllegalStateException] if the calls to [EGL14.eglMakeCurrent] fails
         */
        public fun makeContextCurrent() {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw IllegalStateException("eglMakeCurrent failed")
            }
        }

        /**
         * Initializes the GlesRenderer, and calls [onGlSurfaceCreated]. It is an error to construct
         * a [WatchFace] before this method has been called.
         *
         * @throws [GlesException] If any GL calls fail.
         */
        @UiThread
        @Throws(GlesException::class)
        public fun initOpenGlContext() {
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
                    if (this@GlesRenderer::eglSurface.isInitialized) {
                        if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                            Log.w(TAG, "eglDestroySurface failed")
                        }
                    }
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                }
            })

            // Note we have to call this after the derived class's init() method has run or it's
            // typically going to fail because members have not been initialized.
            createWindowSurface(
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )

            initDone = true
        }

        /**
         * Called when a new GL context is created. It's safe to use GL APIs in this method. Note
         * [makeContextCurrent] is called by the library before this method.
         */
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
        public open fun onGlSurfaceCreated(@Px width: Int, @Px height: Int) {
        }

        internal override fun renderInternal(
            calendar: Calendar
        ) {
            makeContextCurrent()
            renderAndComposite(calendar)
            if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                Log.w(TAG, "eglSwapBuffers failed")
            }
        }

        /** {@inheritDoc} */
        internal override fun takeScreenshot(
            calendar: Calendar,
            renderParameters: RenderParameters
        ): Bitmap = TraceEvent("GlesRenderer.takeScreenshot").use {
            val width = screenBounds.width()
            val height = screenBounds.height()
            val pixelBuf = ByteBuffer.allocateDirect(width * height * 4)
            makeContextCurrent()
            val prevRenderParameters = this.renderParameters
            this.renderParameters = renderParameters
            renderAndComposite(calendar)
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

        private fun renderAndComposite(calendar: Calendar) {
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)

            // Usually renderParameters.watchFaceWatchFaceLayers will be non-empty.
            if (renderParameters.watchFaceLayers.isNotEmpty()) {
                render(calendar)

                // Render and composite the HighlightLayer
                if (renderParameters.highlightLayer != null) {
                    renderBufferTexture.bindFrameBuffer()
                    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
                    renderHighlightLayer(calendar)
                    GLES20.glFlush()

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                    renderBufferTexture.compositeQuad()
                }
            } else {
                require(renderParameters.highlightLayer != null) {
                    "We don't support empty renderParameters.watchFaceWatchFaceLayers without a " +
                        "non-null renderParameters.highlightLayer"
                }
                renderHighlightLayer(calendar)
            }
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
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Any highlights due to
         * [RenderParameters.highlightLayer] should be rendered by [renderHighlightLayer] instead
         * where possible. For correct behavior this function must use the supplied [Calendar]
         * in favor of any other ways of getting the time. Note [makeContextCurrent] and
         * `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` are called by the library before this
         * method.
         *
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun render(calendar: Calendar)

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear the buffer to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [Calendar] in favor of any other ways of getting the time.
         * Note [makeContextCurrent] and `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` are
         * called by the library before this method.
         *
         * @param calendar The current [Calendar]
         */
        @UiThread
        public abstract fun renderHighlightLayer(calendar: Calendar)

        internal override fun dump(writer: IndentingPrintWriter) {
            writer.println("GlesRenderer:")
            writer.increaseIndent()
            writer.println("screenBounds=$screenBounds")
            writer.println(
                "interactiveDrawModeUpdateDelayMillis=$interactiveDrawModeUpdateDelayMillis"
            )
            writer.println("shouldAnimate=${shouldAnimate()}")
            renderParameters.dump(writer)
            writer.decreaseIndent()
        }
    }
}
