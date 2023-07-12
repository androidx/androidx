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
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.utility.TraceEvent
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Describes the type of [Canvas] a [Renderer.CanvasRenderer] or [Renderer.CanvasRenderer2] should
 * request from a [SurfaceHolder].
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@IntDef(value = [CanvasType.SOFTWARE, CanvasType.HARDWARE])
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
         *
         * NOTE the system takes screenshots for use in the watch face picker UI and these will be
         * taken using software rendering. This means [Bitmap]s with [Bitmap.Config.HARDWARE] must
         * be avoided.
         */
        public const val HARDWARE: Int = 1
    }
}

internal val EGL_CONFIG_ATTRIB_LIST =
    intArrayOf(
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

private val HIGHLIGHT_LAYER_COMPOSITE_PAINT =
    Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER) }

/**
 * Flips a bitmap.
 *
 * @param buffer The rgba [ByteBuffer] containing the bitmap to flip
 * @param width The width of the bitmap in pixels
 * @param height The height of the bitmap in pixels
 */
internal fun verticalFlip(buffer: ByteBuffer, width: Int, height: Int) {
    val stride = width * 4
    val heightMinusOne = height - 1
    val halfHeight = height / 2
    val tmp = ByteArray(stride)
    for (i in 0 until halfHeight) {
        System.arraycopy(buffer.array(), i * stride, tmp, 0, stride)

        System.arraycopy(
            buffer.array(),
            (heightMinusOne - i) * stride,
            buffer.array(),
            i * stride,
            stride
        )

        System.arraycopy(tmp, 0, buffer.array(), (heightMinusOne - i) * stride, stride)
    }
    buffer.rewind()
}

/**
 * The base class for [CanvasRenderer], [CanvasRenderer2], [GlesRenderer], [GlesRenderer2]. Where
 * possible it is recommended to use [CanvasRenderer2] or [GlesRenderer2] which allow memory to be
 * saved during editing because there can be more than one watchface instance during editing.
 *
 * Renderers are constructed on a background thread but all rendering is done on the UiThread. There
 * is a memory barrier between construction and rendering so no special threading primitives are
 * required.
 *
 * It is recommended to set [watchfaceColors] with representative [WatchFaceColors] this is used by
 * compatible systems to influence the system's color scheme.
 *
 * Please note [android.graphics.drawable.AnimatedImageDrawable] and similar classes which rely on
 * [android.graphics.drawable.Drawable.Callback] do not animate properly out of the box unless you
 * register an implementation with [android.graphics.drawable.Drawable.setCallback] that calls
 * [invalidate]. Even then these classes are not recommend because the [ZonedDateTime] passed to
 * [Renderer.CanvasRenderer.render] or [Renderer.GlesRenderer.render] is not guaranteed to match the
 * system time (e.g. for taking screenshots). In addition care is needed when implementing
 * [android.graphics.drawable.Drawable.Callback] to ensure it does not animate in ambient mode which
 * could lead to a significant power regression.
 *
 * @param surfaceHolder The [SurfaceHolder] that [renderInternal] will draw into.
 * @param currentUserStyleRepository The associated [CurrentUserStyleRepository].
 * @param watchState The associated [WatchState].
 * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
 *   interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame rate
 *   will be clamped to 10fps. Watch faces are recommended to use lower frame rates if possible for
 *   better battery life. Variable frame rates can also help preserve battery life, e.g. if a watch
 *   face has a short animation once per second it can adjust the frame rate inorder to sleep when
 *   not animating. In ambient mode the watch face will be rendered once per minute.
 */
@Suppress("Deprecation")
public sealed class Renderer
@WorkerThread
constructor(
    surfaceHolder: SurfaceHolder,
    private val currentUserStyleRepository: CurrentUserStyleRepository,
    internal val watchState: WatchState,
    @IntRange(from = 0, to = 60000) public var interactiveDrawModeUpdateDelayMillis: Long,
) {
    /** The [SurfaceHolder] that [renderInternal] will draw into. */
    public var surfaceHolder: SurfaceHolder = surfaceHolder
        protected set

    @OptIn(WatchFaceExperimental::class) private var pendingWatchFaceColors: WatchFaceColors? = null
    private var pendingWatchFaceColorsSet = false

    // Protected by lock
    private var pendingSendPreviewImageNeedsUpdateRequest = false
    private val lock = Any()

    // Protected by lock. NB UI thread code doesn't need the lock.
    internal var watchFaceHostApi: WatchFaceHostApi? = null
        set(value) {
            val pendingSendPreviewImageNeedsUpdateRequestCopy =
                synchronized(lock) {
                    field = value
                    pendingSendPreviewImageNeedsUpdateRequest
                }
            if (pendingWatchFaceColorsSet) {
                @OptIn(WatchFaceExperimental::class)
                value?.onWatchFaceColorsChanged(pendingWatchFaceColors)
            }
            if (pendingSendPreviewImageNeedsUpdateRequestCopy) {
                value?.sendPreviewImageNeedsUpdateRequest()
            }
        }

    internal companion object {
        internal class SharedAssetsHolder {
            var sharedAssets: SharedAssets? = null
            var refCount: Int = 0

            // To avoid undefined behavior with SharedAssets we also need to share the contexts
            // between instances.
            lateinit var eglDisplay: EGLDisplay
            lateinit var eglConfig: EGLConfig
            lateinit var eglBackgroundThreadContext: EGLContext
            lateinit var eglUiThreadContext: EGLContext

            fun eglDisplayInitialized() = this::eglDisplay.isInitialized

            fun eglBackgroundThreadContextInitialized() =
                this::eglBackgroundThreadContext.isInitialized

            fun eglUiThreadContextInitialized() = this::eglUiThreadContext.isInitialized

            fun onDestroy() {
                if (this::eglUiThreadContext.isInitialized) {
                    if (!EGL14.eglDestroyContext(eglDisplay, eglUiThreadContext)) {
                        Log.w(GlesRenderer.TAG, "eglDestroyContext failed")
                    }
                    if (!EGL14.eglDestroyContext(eglDisplay, eglBackgroundThreadContext)) {
                        Log.w(GlesRenderer.TAG, "eglDestroyContext failed")
                    }
                    if (!EGL14.eglTerminate(eglDisplay)) {
                        Log.w(GlesRenderer.TAG, "eglTerminate failed")
                    }
                }
            }
        }

        private val sharedAssetsCache = HashMap<String, SharedAssetsHolder>()
        private val sharedAssetsCacheLock = Mutex()

        internal fun getOrCreateSharedAssetsHolder(renderer: Renderer): SharedAssetsHolder {
            val key = renderer::class.java.name
            synchronized(sharedAssetsCacheLock) {
                sharedAssetsCache.computeIfAbsent(key) { SharedAssetsHolder() }
                val holder = sharedAssetsCache[key]!!
                holder.refCount++
                return holder
            }
        }

        internal fun releaseSharedAssets(renderer: Renderer) {
            val key = renderer::class.java.name
            synchronized(sharedAssetsCacheLock) {
                sharedAssetsCache[key]?.let {
                    if (--it.refCount == 0) {
                        it.sharedAssets?.onDestroy()
                        it.onDestroy()
                        sharedAssetsCache.remove(key)
                    }
                }
            }
        }
    }

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

                override fun surfaceDestroyed(holder: SurfaceHolder) {}

                override fun surfaceCreated(holder: SurfaceHolder) {}
            }
        )
    }

    /**
     * The bounds of the [SurfaceHolder] this Renderer renders into. Depending on the shape of the
     * device's screen not all of these pixels may be visible to the user (see
     * [Configuration.isScreenRound]). Note also that API level 27+ devices draw indicators in the
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
        internal set(value) {
            if (value != field) {
                field = value
                onRenderParametersChanged(value)
            }
        }

    internal var sharedAssetsHolder = getOrCreateSharedAssetsHolder(this)

    /**
     * Accessibility [ContentDescriptionLabel] for any rendered watch face elements other than the
     * time and [ComplicationSlot]s which are generated automatically.
     *
     * The [Int] in the `Pair<Int, ContentDescriptionLabel>` is used to sort the
     * [ContentDescriptionLabel]s. Note the time piece has an accessibility traversal index of -1
     * and each [ComplicationSlot]'s index is defined by its
     * [ComplicationSlot.accessibilityTraversalIndex].
     */
    public var additionalContentDescriptionLabels: Collection<Pair<Int, ContentDescriptionLabel>> =
        emptyList()
        set(value) {
            field = value
            for (pair in value) {
                require(pair.first >= 0) {
                    "Each accessibility label index in additionalContentDescriptionLabels " +
                        "must be >= 0"
                }
            }
            watchFaceHostApi?.updateContentDescriptionLabels()
        }

    internal fun onDestroyInternal() {
        try {
            onDestroy()
        } finally {
            runBlocking { releaseSharedAssets(this@Renderer) }
        }
    }

    /** Called when the Renderer is destroyed. */
    @UiThread public open fun onDestroy() {}

    /**
     * Renders the watch face into the [surfaceHolder] using the current [renderParameters] with the
     * user style specified by the [currentUserStyleRepository].
     *
     * @param zonedDateTime The [ZonedDateTime] to use when rendering the watch face
     * @return A [Bitmap] containing a screenshot of the watch face
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun renderInternal(zonedDateTime: ZonedDateTime)

    /**
     * Renders the watch face into a Bitmap with the user style specified by the
     * [currentUserStyleRepository].
     *
     * @param zonedDateTime The [ZonedDateTime] to use when rendering the watch face
     * @param renderParameters The [RenderParameters] to use when rendering the watch face
     * @return A [Bitmap] containing a screenshot of the watch face
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun takeScreenshot(
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters
    ): Bitmap

    /**
     * Renders the watch face into a [Surface] with the user style specified by the
     * [currentUserStyleRepository].
     *
     * @param zonedDateTime The [ZonedDateTime] to use when rendering the watch face
     * @param renderParameters The [RenderParameters] to use when rendering the watch face
     * @param screenShotSurfaceHolder The [SurfaceHolder] containing the [Surface] to render into.
     *   This is assumed to have the same dimensions as the screen.
     */
    @Suppress("HiddenAbstractMethod")
    @UiThread
    internal abstract fun renderScreenshotToSurface(
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters,
        screenShotSurfaceHolder: SurfaceHolder
    )

    /**
     * Called when the [RenderParameters] has been updated. Will always be called before the first
     * call to [CanvasRenderer.render] or [GlesRenderer.render].
     */
    @UiThread protected open fun onRenderParametersChanged(renderParameters: RenderParameters) {}

    /**
     * This method is used for accessibility support to describe the portion of the screen
     * containing the main clock element. By default we assume this is contained in the central half
     * of the watch face. Watch faces should override this to return the correct bounds for the main
     * clock element.
     *
     * @return A [Rect] describing the bounds of the watch faces' main clock element
     */
    @UiThread
    public open fun getMainClockElementBounds(): Rect {
        val quarterX = centerX / 2
        val quarterY = centerY / 2
        return Rect(
            (centerX - quarterX).toInt(),
            (centerY - quarterY).toInt(),
            (centerX + quarterX).toInt(),
            (centerY + quarterY).toInt()
        )
    }

    /**
     * The system periodically (at least once per minute) calls onTimeTick() to trigger a display
     * update. If the watch face needs to animate with an interactive frame rate, calls to
     * invalidate must be scheduled. This method controls whether or not we should do that and if
     * shouldAnimate returns true we inhibit entering [DrawMode.AMBIENT].
     *
     * By default we remain at an interactive frame rate when the watch face is visible and we're
     * not in ambient mode. Watch faces with animated transitions for entering ambient mode may need
     * to override this to ensure they play smoothly.
     *
     * @return Whether we should schedule an onDraw call to maintain an interactive frame rate
     */
    @UiThread
    public open fun shouldAnimate(): Boolean =
        watchState.isVisible.value!! && !watchState.isAmbient.value!!

    /**
     * Schedules a call to either [CanvasRenderer.render] or [GlesRenderer.render] to draw the next
     * frame.
     */
    @UiThread
    public fun invalidate() {
        watchFaceHostApi?.invalidate()
    }

    /**
     * Posts a message to schedule a call to either [CanvasRenderer.render] or [GlesRenderer.render]
     * to draw the next frame. Unlike [invalidate], this method is thread-safe and may be called on
     * any thread.
     */
    public fun postInvalidate() {
        watchFaceHostApi?.getUiThreadHandler()?.post { watchFaceHostApi!!.invalidate() }
    }

    @UiThread internal abstract fun dumpInternal(writer: IndentingPrintWriter)

    /**
     * Called when adb shell dumpsys is invoked for the WatchFaceService, allowing the renderer to
     * optionally record state for debugging purposes.
     */
    @UiThread public abstract fun onDump(writer: PrintWriter)

    /**
     * Perform UiThread specific initialization. Will be called once during initialization before
     * any subsequent calls to [renderInternal] or [takeScreenshot].
     */
    @UiThread
    internal abstract suspend fun uiThreadInitInternal(uiThreadCoroutineScope: CoroutineScope)

    @WorkerThread internal open suspend fun backgroundThreadInitInternal() {}

    /**
     * Representative [WatchFaceColors] which are made available to system clients via
     * [androidx.wear.watchface.client.InteractiveWatchFaceClient.OnWatchFaceColorsListener].
     *
     * Initially this value is `null` signifying that the colors are unknown. When possible the
     * watchFace should assign `non null` [WatchFaceColors] and keep this updated when the colors
     * change (e.g. due to a style change).
     */
    @WatchFaceExperimental
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:WatchFaceExperimental
    @set:WatchFaceExperimental
    public var watchfaceColors: WatchFaceColors? = null
        set(value) {
            require(value != null) { "watchfaceColors must be non-null " }

            val hostApi = watchFaceHostApi
            if (hostApi == null) {
                pendingWatchFaceColors = value
                pendingWatchFaceColorsSet = true
            } else {
                hostApi.onWatchFaceColorsChanged(value)
            }
        }

    /**
     * Multiple [WatchFaceService] instances and hence Renderers can exist concurrently (e.g. a
     * headless instance and an interactive instance) and using SharedAssets allows memory to be
     * saved by sharing immutable data (e.g. Bitmaps and shaders) between them.
     *
     * Note SharedAssets will be constructed on a background thread, but (typically) used and
     * released on the ui thread.
     */
    public interface SharedAssets {
        /**
         * Notification that any resources owned by SharedAssets should be released, called when no
         * renderer instances are left.
         */
        @UiThread public fun onDestroy()
    }

    internal abstract fun renderBlackFrame()

    /**
     * Sends a request to the system asking it to update the preview image. This is useful for watch
     * faces with configuration outside of the [UserStyleSchema] E.g. a watchface with a selectable
     * background.
     *
     * The system may choose to rate limit this method for performance reasons and the system is
     * free to schedule when the update occurs.
     *
     * Requires a compatible system to work (if the system is incompatible this does nothing). This
     * can be called from any thread.
     */
    public fun sendPreviewImageNeedsUpdateRequest() {
        synchronized(lock) {
            if (watchFaceHostApi == null) {
                pendingSendPreviewImageNeedsUpdateRequest = true
            } else {
                watchFaceHostApi!!.sendPreviewImageNeedsUpdateRequest()
            }
        }
    }

    /**
     * Watch faces that require [Canvas] rendering should extend their [Renderer] from this class or
     * [CanvasRenderer2] if they can take advantage of [SharedAssets] to save memory when editing
     * (there can be more than once WatchFace instance when editing).
     *
     * A CanvasRenderer is expected to be constructed on the background thread associated with
     * [WatchFaceService.getBackgroundThreadHandler] inside a call to
     * [WatchFaceService.createWatchFace]. All rendering is be done on the UiThread. There is a
     * memory barrier between construction and rendering so no special threading primitives are
     * required.
     *
     * In Java it may be easier to extend [androidx.wear.watchface.ListenableCanvasRenderer]
     * instead.
     *
     * @param surfaceHolder The [SurfaceHolder] from which a [Canvas] to will be obtained and passed
     *   into [render].
     * @param currentUserStyleRepository The watch face's associated [CurrentUserStyleRepository].
     * @param watchState The watch face's associated [WatchState].
     * @param canvasType The [CanvasType] to request. Note even if [CanvasType.HARDWARE] is used,
     *   screenshots will taken using the software rendering pipeline, as such [Bitmap]s with
     *   [Bitmap.Config.HARDWARE] must be avoided.
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     *   interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     *   rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     *   possible for better battery life. Variable frame rates can also help preserve battery life,
     *   e.g. if a watch face has a short animation once per second it can adjust the framerate
     *   inorder to sleep when not animating.
     * @param clearWithBackgroundTintBeforeRenderingHighlightLayer Whether the [Canvas] is cleared
     *   with [RenderParameters.HighlightLayer.backgroundTint] before [renderHighlightLayer] is
     *   called. Defaults to `false`.
     */
    @Deprecated(message = "CanvasRenderer is deprecated", ReplaceWith("CanvasRenderer2"))
    public abstract class CanvasRenderer
    @WorkerThread
    @JvmOverloads
    constructor(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @CanvasType private val canvasType: Int,
        @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
        val clearWithBackgroundTintBeforeRenderingHighlightLayer: Boolean = false
    ) :
        Renderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            interactiveDrawModeUpdateDelayMillis
        ) {
        internal override fun renderInternal(zonedDateTime: ZonedDateTime) {
            val canvas =
                (if (canvasType == CanvasType.HARDWARE) {
                    surfaceHolder.lockHardwareCanvas()
                } else {
                    surfaceHolder.lockCanvas()
                })
                    ?: return
            try {
                if (Build.VERSION.SDK_INT >= 30 || watchState.isVisible.value!!) {
                    renderAndComposite(canvas, zonedDateTime)
                } else {
                    canvas.drawColor(Color.BLACK)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        internal override fun takeScreenshot(
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters
        ): Bitmap =
            TraceEvent("CanvasRenderer.takeScreenshot").use {
                val bitmap =
                    Bitmap.createBitmap(
                        screenBounds.width(),
                        screenBounds.height(),
                        Bitmap.Config.ARGB_8888
                    )
                val prevRenderParameters = this.renderParameters
                val originalIsForScreenshot = renderParameters.isForScreenshot

                renderParameters.isForScreenshot = true
                this.renderParameters = renderParameters
                renderAndComposite(Canvas(bitmap), zonedDateTime)
                this.renderParameters = prevRenderParameters
                renderParameters.isForScreenshot = originalIsForScreenshot

                return bitmap
            }

        internal override fun renderScreenshotToSurface(
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters,
            screenShotSurfaceHolder: SurfaceHolder
        ) {
            val prevRenderParameters = this.renderParameters
            val originalIsForScreenshot = renderParameters.isForScreenshot
            val originalSurfaceHolder = surfaceHolder
            surfaceHolder = screenShotSurfaceHolder

            renderParameters.isForScreenshot = true
            this.renderParameters = renderParameters
            val canvas = surfaceHolder.surface.lockHardwareCanvas()
            TraceEvent("CanvasRenderer.renderScreenshotToSurface").use {
                renderAndComposite(canvas, zonedDateTime)
            }
            surfaceHolder.surface.unlockCanvasAndPost(canvas)
            this.renderParameters = prevRenderParameters
            renderParameters.isForScreenshot = originalIsForScreenshot
            surfaceHolder = originalSurfaceHolder
        }

        private fun renderAndComposite(canvas: Canvas, zonedDateTime: ZonedDateTime) {
            // Usually renderParameters.watchFaceWatchFaceLayers will be non-empty.
            if (renderParameters.watchFaceLayers.isNotEmpty()) {
                render(canvas, screenBounds, zonedDateTime)

                // Render and composite the HighlightLayer
                val highlightLayer = renderParameters.highlightLayer
                if (highlightLayer != null) {
                    val highlightLayerBitmap =
                        Bitmap.createBitmap(
                            screenBounds.width(),
                            screenBounds.height(),
                            Bitmap.Config.ARGB_8888
                        )
                    val highlightCanvas = Canvas(highlightLayerBitmap)
                    if (clearWithBackgroundTintBeforeRenderingHighlightLayer) {
                        highlightCanvas.drawColor(highlightLayer.backgroundTint)
                    }
                    renderHighlightLayer(highlightCanvas, screenBounds, zonedDateTime)
                    canvas.drawBitmap(highlightLayerBitmap, 0f, 0f, HIGHLIGHT_LAYER_COMPOSITE_PAINT)
                    highlightLayerBitmap.recycle()
                }
            } else {
                val highlightLayer = renderParameters.highlightLayer
                require(highlightLayer != null) {
                    "We don't support empty renderParameters.watchFaceWatchFaceLayers without a " +
                        "non-null renderParameters.highlightLayer"
                }
                if (clearWithBackgroundTintBeforeRenderingHighlightLayer) {
                    canvas.drawColor(highlightLayer.backgroundTint)
                }
                renderHighlightLayer(canvas, screenBounds, zonedDateTime)
            }
        }

        internal override suspend fun uiThreadInitInternal(uiThreadCoroutineScope: CoroutineScope) {
            init()
        }

        /**
         * Perform UiThread specific initialization. Will be called once during initialization
         * before any subsequent calls to [render]. If you need to override this method in java,
         * consider using [androidx.wear.watchface.ListenableCanvasRenderer] instead.
         */
        @UiThread public open suspend fun init() {}

        /**
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Please note [WatchState.isAmbient] may not
         * match the [RenderParameters.drawMode] and should not be used to decide what to render.
         * E.g. when editing from the companion phone while the watch is ambient, renders may be
         * requested with [DrawMode.INTERACTIVE].
         *
         * Any highlights due to [RenderParameters.highlightLayer] should be rendered by
         * [renderHighlightLayer] instead where possible. For correct behavior this function must
         * use the supplied [ZonedDateTime] in favor of any other ways of getting the time.
         *
         * Before any calls to render, [init] will be called once.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         *   the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param zonedDateTime The [ZonedDateTime] to render with
         */
        @UiThread
        public abstract fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime)

        internal override fun renderBlackFrame() {
            val canvas =
                if (canvasType == CanvasType.SOFTWARE) {
                    surfaceHolder.lockCanvas()
                } else {
                    surfaceHolder.lockHardwareCanvas()
                }
            try {
                canvas.drawColor(Color.BLACK)
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear [canvas] to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [ZonedDateTime] in favor of any other ways of getting the
         * time.
         *
         * Note if [clearWithBackgroundTintBeforeRenderingHighlightLayer] is `true` then [canvas]
         * will cleared with [RenderParameters.HighlightLayer.backgroundTint] before
         * renderHighlightLayer is called. Otherwise it is up to the overridden function to clear
         * the Canvas if necessary.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         *   the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param zonedDateTime the [ZonedDateTime] to render with
         */
        @UiThread
        public abstract fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime
        )

        internal override fun dumpInternal(writer: IndentingPrintWriter) {
            writer.println("CanvasRenderer:")
            writer.increaseIndent()
            writer.println("canvasType=$canvasType")
            writer.println("screenBounds=$screenBounds")
            writer.println(
                "interactiveDrawModeUpdateDelayMillis=$interactiveDrawModeUpdateDelayMillis"
            )
            writer.println("shouldAnimate=${shouldAnimate()}")
            renderParameters.dump(writer)
            onDump(writer.writer)
            writer.decreaseIndent()
        }

        override fun onDump(writer: PrintWriter) {}
    }

    /**
     * Watch faces that require [Canvas] rendering and are able to take advantage of [SharedAssets]
     * to save memory (there can be more than once instance when editing), should extend their
     * [Renderer] from this class.
     *
     * A CanvasRenderer is expected to be constructed on the background thread associated with
     * [WatchFaceService.getBackgroundThreadHandler] inside a call to
     * [WatchFaceService.createWatchFace]. All rendering is be done on the UiThread. There is a
     * memory barrier between construction and rendering so no special threading primitives are
     * required.
     *
     * In Java it may be easier to extend [androidx.wear.watchface.ListenableCanvasRenderer2]
     * instead.
     *
     * @param SharedAssetsT The type extending [SharedAssets] returned by [createSharedAssets] and
     *   passed into [render] and [renderHighlightLayer].
     * @param surfaceHolder The [SurfaceHolder] from which a [Canvas] to will be obtained and passed
     *   into [render].
     * @param currentUserStyleRepository The watch face's associated [CurrentUserStyleRepository].
     * @param watchState The watch face's associated [WatchState].
     * @param canvasType The [CanvasType] to request. Note even if [CanvasType.HARDWARE] is used,
     *   screenshots will taken using the software rendering pipeline, as such [Bitmap]s with
     *   [Bitmap.Config.HARDWARE] must be avoided.
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     *   interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     *   rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     *   possible for better battery life. Variable frame rates can also help preserve battery life,
     *   e.g. if a watch face has a short animation once per second it can adjust the framerate
     *   inorder to sleep when not animating.
     * @param clearWithBackgroundTintBeforeRenderingHighlightLayer Whether the [Canvas] is cleared
     *   with [RenderParameters.HighlightLayer.backgroundTint] before [renderHighlightLayer] is
     *   called. Defaults to `false`.
     */
    public abstract class CanvasRenderer2<SharedAssetsT>
    @WorkerThread
    constructor(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @CanvasType private val canvasType: Int,
        @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
        clearWithBackgroundTintBeforeRenderingHighlightLayer: Boolean
    ) :
        CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            canvasType,
            interactiveDrawModeUpdateDelayMillis,
            clearWithBackgroundTintBeforeRenderingHighlightLayer
        ) where SharedAssetsT : SharedAssets {
        /**
         * When editing multiple [WatchFaceService] instances and hence Renderers can exist
         * concurrently (e.g. a headless instance and an interactive instance) and using
         * [SharedAssets] allows memory to be saved by sharing immutable data (e.g. Bitmaps,
         * shaders, etc...) between them.
         *
         * To take advantage of SharedAssets, override this method. The constructed SharedAssets are
         * passed into the [render] as an argument (NB you'll have to cast this to your type).
         *
         * When all instances using SharedAssets have been closed, [SharedAssets.onDestroy] will be
         * called.
         *
         * Note that while SharedAssets are constructed on a background thread, they'll typically be
         * used on the main thread and subsequently destroyed there.
         *
         * @return The [SharedAssetsT] that will be passed into [render] and [renderHighlightLayer].
         */
        @WorkerThread protected abstract suspend fun createSharedAssets(): SharedAssetsT

        internal override suspend fun backgroundThreadInitInternal() {
            if (sharedAssetsHolder.sharedAssets == null) {
                sharedAssetsHolder.sharedAssets = createSharedAssets()
            }
        }

        /**
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Please note [WatchState.isAmbient] may not
         * match the [RenderParameters.drawMode] and should not be used to decide what to render.
         * E.g. when editing from the companion phone while the watch is ambient, renders may be
         * requested with [DrawMode.INTERACTIVE].
         *
         * Any highlights due to [RenderParameters.highlightLayer] should be rendered by
         * [renderHighlightLayer] instead where possible. For correct behavior this function must
         * use the supplied [ZonedDateTime] in favor of any other ways of getting the time.
         *
         * Before any calls to render, [init] will be called once.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         *   the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param zonedDateTime The [ZonedDateTime] to render with
         * @param sharedAssets The [SharedAssetsT] returned by [createSharedAssets]
         */
        @UiThread
        public abstract fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: SharedAssetsT
        )

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear [canvas] to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [ZonedDateTime] in favor of any other ways of getting the
         * time.
         *
         * Note if [clearWithBackgroundTintBeforeRenderingHighlightLayer] is `true` then [canvas]
         * will cleared with [RenderParameters.HighlightLayer.backgroundTint] before
         * renderHighlightLayer is called. Otherwise it is up to the overridden function to clear
         * the Canvas if necessary.
         *
         * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
         *   the [SurfaceHolder] backing the display
         * @param bounds A [Rect] describing the bonds of the canvas to draw into
         * @param zonedDateTime the [ZonedDateTime] to render with
         * @param sharedAssets The [SharedAssetsT] returned by [createSharedAssets]
         */
        @UiThread
        public abstract fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: SharedAssetsT
        )

        final override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
            @Suppress("UNCHECKED_CAST") // We know the type is correct.
            render(
                canvas,
                bounds,
                zonedDateTime,
                sharedAssetsHolder.sharedAssets!! as SharedAssetsT
            )
        }

        final override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime
        ) {
            @Suppress("UNCHECKED_CAST") // We know the type is correct.
            renderHighlightLayer(
                canvas,
                bounds,
                zonedDateTime,
                sharedAssetsHolder.sharedAssets!! as SharedAssetsT
            )
        }
    }

    /**
     * Watch faces that require [GLES20] rendering should extend their [Renderer] from this class or
     * [GlesRenderer2] if they can take advantage of [SharedAssets] to save memory when editing
     * (there can be more than once WatchFace instance when editing).
     *
     * A GlesRenderer is expected to be constructed on the background thread associated with
     * [WatchFaceService.getBackgroundThreadHandler] inside a call to
     * [WatchFaceService.createWatchFace]. All rendering is be done on the UiThread. There is a
     * memory barrier between construction and rendering so no special threading primitives are
     * required.
     *
     * Two linked [EGLContext]s are created [eglBackgroundThreadContext] and [eglUiThreadContext]
     * which are associated with background and UiThread respectively and are shared by all
     * instances of the renderer. OpenGL objects created on (e.g. shaders and textures) can be used
     * on the other.
     *
     * If you need to make any OpenGl calls outside of [render],
     * [onBackgroundThreadGlContextCreated] or [onUiThreadGlSurfaceCreated] then you must use either
     * [runUiThreadGlCommands] or [runBackgroundThreadGlCommands] to execute a [Runnable] inside of
     * the corresponding context. Access to the GL contexts this way is necessary because GL
     * contexts are not shared between renderers and there can be multiple watch face instances
     * existing concurrently (e.g. headless and interactive, potentially from different watch faces
     * if an APK contains more than one [WatchFaceService]). In addition most drivers do not support
     * concurrent access.
     *
     * In Java it may be easier to extend [androidx.wear.watchface.ListenableGlesRenderer] instead.
     *
     * @param surfaceHolder The [SurfaceHolder] whose [android.view.Surface] [render] will draw
     *   into.
     * @param currentUserStyleRepository The associated [CurrentUserStyleRepository].
     * @param watchState The associated [WatchState].
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     *   interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     *   rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     *   possible for better battery life. Variable frame rates can also help preserve battery life,
     *   e.g. if a watch face has a short animation once per second it can adjust the frame rate
     *   inorder to sleep when not animating.
     * @param eglConfigAttribList Attributes for [EGL14.eglChooseConfig]. By default this selects an
     *   RGBA8888 back buffer.
     * @param eglSurfaceAttribList The attributes to be passed to [EGL14.eglCreateWindowSurface]. By
     *   default this is empty.
     * @param eglContextAttribList The attributes to be passed to [EGL14.eglCreateContext]. By
     *   default this selects [EGL14.EGL_CONTEXT_CLIENT_VERSION] 2.
     * @throws [GlesException] If any GL calls fail during initialization.
     */
    @Deprecated(message = "GlesRenderer is deprecated", ReplaceWith("GlesRenderer2"))
    public abstract class GlesRenderer
    @Throws(GlesException::class)
    @JvmOverloads
    @WorkerThread
    constructor(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
        private val eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,
        private val eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST,
        private val eglContextAttribList: IntArray = EGL_CONTEXT_ATTRIB_LIST
    ) :
        Renderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            interactiveDrawModeUpdateDelayMillis
        ) {
        internal companion object {
            internal const val TAG = "Gles2WatchFace"

            private val glContextLock = Mutex()
        }

        /** Exception thrown if a GL call fails */
        public class GlesException(message: String) : Exception(message)

        init {
            if (!sharedAssetsHolder.eglDisplayInitialized()) {
                sharedAssetsHolder.eglDisplay =
                    EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY).apply {
                        if (this == EGL14.EGL_NO_DISPLAY) {
                            throw GlesException("eglGetDisplay returned EGL_NO_DISPLAY")
                        }
                        // Initialize the display. The major and minor version numbers are passed
                        // back.
                        val version = IntArray(2)
                        if (!EGL14.eglInitialize(this, version, 0, version, 1)) {
                            throw GlesException("eglInitialize failed")
                        }
                    }

                sharedAssetsHolder.eglConfig = chooseEglConfig(eglDisplay)
            }
        }

        /**
         * The GlesRenderer's [EGLDisplay].
         *
         * @throws UnsupportedOperationException setEglDisplay is unsupported.
         */
        public var eglDisplay: EGLDisplay
            get() = sharedAssetsHolder.eglDisplay
            @Deprecated("It's not intended for eglDisplay to be set")
            set(@Suppress("UNUSED_PARAMETER") eglDisplay) {
                throw UnsupportedOperationException()
            }

        /**
         * The GlesRenderer's [EGLConfig].
         *
         * @throws UnsupportedOperationException setEglConfig is unsupported.
         */
        public var eglConfig: EGLConfig
            get() = sharedAssetsHolder.eglConfig
            @Deprecated("It's not intended for eglConfig to be set")
            set(@Suppress("UNUSED_PARAMETER") eglConfig) {
                throw UnsupportedOperationException()
            }

        /** The GlesRenderer's background Thread [EGLContext]. */
        public val eglBackgroundThreadContext: EGLContext
            get() = sharedAssetsHolder.eglBackgroundThreadContext

        /**
         * The GlesRenderer's UiThread [EGLContext]. Note this not available until after
         * [WatchFaceService.createWatchFace] has completed.
         */
        public val eglUiThreadContext: EGLContext
            get() = sharedAssetsHolder.eglUiThreadContext

        // A 1x1 surface which is needed by EGL14.eglMakeCurrent.
        private val fakeBackgroundThreadSurface =
            EGL14.eglCreatePbufferSurface(
                eglDisplay,
                eglConfig,
                intArrayOf(
                    EGL14.EGL_WIDTH,
                    1,
                    EGL14.EGL_HEIGHT,
                    1,
                    EGL14.EGL_TEXTURE_TARGET,
                    EGL14.EGL_NO_TEXTURE,
                    EGL14.EGL_TEXTURE_FORMAT,
                    EGL14.EGL_NO_TEXTURE,
                    EGL14.EGL_NONE
                ),
                0
            )
        private lateinit var eglSurface: EGLSurface
        private var calledOnGlContextCreated = false
        private val renderBufferTexture by lazy {
            RenderBufferTexture(
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }

        /**
         * Chooses the EGLConfig to use.
         *
         * @throws [GlesException] if [EGL14.eglChooseConfig] fails
         */
        private fun chooseEglConfig(eglDisplay: EGLDisplay): EGLConfig {
            val numEglConfigs = IntArray(1)
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            if (
                !EGL14.eglChooseConfig(
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

        private suspend fun createWindowSurface(width: Int, height: Int) =
            TraceEvent("GlesRenderer.createWindowSurface").use {
                if (this::eglSurface.isInitialized) {
                    if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                        Log.w(TAG, "eglDestroySurface failed")
                    }
                }
                eglSurface =
                    if (watchState.isHeadless) {
                        // Headless instances have a fake surfaceHolder so fall back to a Pbuffer.
                        EGL14.eglCreatePbufferSurface(
                            eglDisplay,
                            eglConfig,
                            intArrayOf(
                                EGL14.EGL_WIDTH,
                                width,
                                EGL14.EGL_HEIGHT,
                                height,
                                EGL14.EGL_NONE
                            ),
                            0
                        )
                    } else {
                        require(surfaceHolder.surface.isValid) {
                            "A valid surfaceHolder is required. "
                        }
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

                runUiThreadGlCommands {
                    if (!calledOnGlContextCreated) {
                        calledOnGlContextCreated = true
                    }
                    TraceEvent("GlesRenderer.onGlSurfaceCreated").use {
                        onUiThreadGlSurfaceCreated(width, height)
                    }
                }
            }

        @CallSuper
        override fun onDestroy() {
            if (this::eglSurface.isInitialized) {
                if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                    Log.w(GlesRenderer.TAG, "eglDestroySurface failed")
                }
            }
        }

        /**
         * Inside of a [Mutex] this function sets the GL context associated with the
         * [WatchFaceService.getBackgroundThreadHandler]'s looper thread as the current one,
         * executes [commands] and finally unsets the GL context.
         *
         * Access to the GL context this way is necessary because GL contexts are not shared between
         * renderers and there can be multiple watch face instances existing concurrently (e.g.
         * headless and interactive, potentially from different watch faces if an APK contains more
         * than one [WatchFaceService]).
         *
         * NB this function is called by the library before running [runBackgroundThreadGlCommands]
         * so there's no need to use this directly in client code unless you need to make GL calls
         * outside of those methods. If you need to call this method from java, consider using
         * [androidx.wear.watchface.ListenableGlesRenderer] which provides an overload taking a
         * [Runnable].
         *
         * @throws [IllegalStateException] if the calls to [EGL14.eglMakeCurrent] fails
         */
        @WorkerThread
        public suspend fun runBackgroundThreadGlCommands(commands: suspend () -> Unit) {
            require(
                watchFaceHostApi == null ||
                    watchFaceHostApi!!.getBackgroundThreadHandler().looper.isCurrentThread
            ) {
                "runBackgroundThreadGlCommands must be called from the Background Thread"
            }
            // It's only safe to run GL command from one thread at a time.
            glContextLock.withLock {
                if (
                    !EGL14.eglMakeCurrent(
                        eglDisplay,
                        fakeBackgroundThreadSurface,
                        fakeBackgroundThreadSurface,
                        eglBackgroundThreadContext
                    )
                ) {
                    throw IllegalStateException(
                        "eglMakeCurrent failed, eglGetError() = " + EGL14.eglGetError()
                    )
                }

                try {
                    GLES20.glViewport(
                        0,
                        0,
                        surfaceHolder.surfaceFrame.width(),
                        surfaceHolder.surfaceFrame.height()
                    )
                    commands()
                } finally {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                }
            }
        }

        /**
         * Initializes the GlesRenderer, and calls [onUiThreadGlSurfaceCreated]. It is an error to
         * construct a [WatchFace] before this method has been called.
         *
         * @throws [GlesException] If any GL calls fail.
         */
        @WorkerThread
        internal override suspend fun backgroundThreadInitInternal() =
            TraceEvent("GlesRenderer.initBackgroundThreadOpenGlContext").use {
                if (!sharedAssetsHolder.eglBackgroundThreadContextInitialized()) {
                    sharedAssetsHolder.eglBackgroundThreadContext =
                        EGL14.eglCreateContext(
                            eglDisplay,
                            eglConfig,
                            EGL14.EGL_NO_CONTEXT,
                            eglContextAttribList,
                            0
                        )
                    if (sharedAssetsHolder.eglBackgroundThreadContext == EGL14.EGL_NO_CONTEXT) {
                        throw RuntimeException("eglCreateContext failed")
                    }
                }

                TraceEvent("GlesRenderer.onGlContextCreated").use {
                    runBackgroundThreadGlCommands {
                        maybeCreateSharedAssets()
                        this@GlesRenderer.onBackgroundThreadGlContextCreated()
                    }
                }
            }

        internal open suspend fun maybeCreateSharedAssets() {
            // NOP
        }

        /**
         * Inside of a [Mutex] this function sets the UiThread GL context as the current one,
         * executes [commands] and finally unsets the GL context.
         *
         * Access to the GL context this way is necessary because GL contexts are not shared between
         * renderers and there can be multiple watch face instances existing concurrently (e.g.
         * headless and interactive, potentially from different watch faces if an APK contains more
         * than one [WatchFaceService]).
         *
         * If you need to call this method from java, consider using
         * [androidx.wear.watchface.ListenableGlesRenderer] which provides an overload taking a
         * [Runnable].
         *
         * @throws [IllegalStateException] if the calls to [EGL14.eglMakeCurrent] fails
         */
        public suspend fun runUiThreadGlCommands(commands: suspend () -> Unit) {
            require(watchFaceHostApi!!.getUiThreadHandler().looper.isCurrentThread) {
                "runUiThreadGlCommands must be called from the UiThread"
            }

            // It's only safe to run GL command from one thread at a time.
            glContextLock.withLock {
                if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglUiThreadContext)) {
                    throw IllegalStateException(
                        "eglMakeCurrent failed, eglGetError() = " + EGL14.eglGetError()
                    )
                }

                try {
                    GLES20.glViewport(
                        0,
                        0,
                        surfaceHolder.surfaceFrame.width(),
                        surfaceHolder.surfaceFrame.height()
                    )
                    commands()
                } finally {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                }
            }
        }

        /**
         * Initializes the GlesRenderer, and calls [onUiThreadGlSurfaceCreated]. It is an error to
         * construct a [WatchFace] before this method has been called.
         *
         * @throws [GlesException] If any GL calls fail.
         */
        @UiThread
        internal override suspend fun uiThreadInitInternal(uiThreadCoroutineScope: CoroutineScope) =
            TraceEvent("GlesRenderer.initUiThreadOpenGlContext").use {
                if (!sharedAssetsHolder.eglUiThreadContextInitialized()) {
                    sharedAssetsHolder.eglUiThreadContext =
                        EGL14.eglCreateContext(
                            eglDisplay,
                            eglConfig,
                            eglBackgroundThreadContext,
                            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                            0
                        )
                }

                surfaceHolder.addCallback(
                    object : SurfaceHolder.Callback {
                        @SuppressLint("SyntheticAccessor")
                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            uiThreadCoroutineScope.launch { createWindowSurface(width, height) }
                        }

                        @SuppressLint("SyntheticAccessor")
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            if (this@GlesRenderer::eglSurface.isInitialized) {
                                if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                                    Log.w(TAG, "eglDestroySurface failed")
                                }
                            }
                        }

                        override fun surfaceCreated(holder: SurfaceHolder) {}
                    }
                )

                // Note we have to call this after the derived class's init() method has run or it's
                // typically going to fail because members have not been initialized.
                createWindowSurface(
                    surfaceHolder.surfaceFrame.width(),
                    surfaceHolder.surfaceFrame.height()
                )
            }

        /**
         * Called once a background thread when a new GL context is created on the background
         * thread, before any subsequent calls to [render]. Note this function is called inside a
         * lambda passed to [runBackgroundThreadGlCommands] which has synchronized access to the GL
         * context.
         *
         * If you need to override this method in java, consider using
         * [androidx.wear.watchface.ListenableGlesRenderer] instead.
         */
        @WorkerThread public open suspend fun onBackgroundThreadGlContextCreated() {}

        /**
         * Called when a new GL surface is created on the UiThread, before any subsequent calls to
         * [render] or in response to [SurfaceHolder.Callback.surfaceChanged]. Note this function is
         * called inside a lambda passed to [runUiThreadGlCommands] which has synchronized access to
         * the GL context.
         *
         * If you need to override this method in java, consider using
         * [androidx.wear.watchface.ListenableGlesRenderer] instead.
         *
         * @param width width of surface in pixels
         * @param height height of surface in pixels
         */
        @UiThread
        public open suspend fun onUiThreadGlSurfaceCreated(@Px width: Int, @Px height: Int) {}

        internal override fun renderInternal(zonedDateTime: ZonedDateTime) {
            runBlocking {
                runUiThreadGlCommands {
                    renderAndComposite(zonedDateTime)
                    if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                        Log.w(TAG, "eglSwapBuffers failed")
                    }
                }
            }
        }

        internal override fun takeScreenshot(
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters
        ): Bitmap =
            TraceEvent("GlesRenderer.takeScreenshot").use {
                val width = screenBounds.width()
                val height = screenBounds.height()
                val pixelBuf = ByteBuffer.allocateDirect(width * height * 4)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                runBlocking {
                    runUiThreadGlCommands {
                        val prevRenderParameters = this@GlesRenderer.renderParameters
                        this@GlesRenderer.renderParameters = renderParameters
                        renderParameters.isForScreenshot = true
                        renderAndComposite(zonedDateTime)
                        renderParameters.isForScreenshot = false
                        this@GlesRenderer.renderParameters = prevRenderParameters
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
                        // The image is flipped when using read pixels because the first pixel in
                        // the OpenGL buffer is in bottom left.
                        verticalFlip(pixelBuf, width, height)
                        bitmap.copyPixelsFromBuffer(pixelBuf)
                    }
                }
                return bitmap
            }

        internal override fun renderScreenshotToSurface(
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters,
            screenShotSurfaceHolder: SurfaceHolder
        ) {
            val prevRenderParameters = this.renderParameters
            val originalIsForScreenshot = renderParameters.isForScreenshot
            val originalSurfaceHolder = surfaceHolder
            surfaceHolder = screenShotSurfaceHolder

            renderParameters.isForScreenshot = true
            this.renderParameters = renderParameters

            runBlocking {
                glContextLock.withLock {
                    val tempEglSurface =
                        EGL14.eglCreateWindowSurface(
                            eglDisplay,
                            eglConfig,
                            surfaceHolder.surface,
                            eglSurfaceAttribList,
                            0
                        )

                    if (
                        !EGL14.eglMakeCurrent(
                            eglDisplay,
                            tempEglSurface,
                            tempEglSurface,
                            eglUiThreadContext
                        )
                    ) {
                        throw IllegalStateException(
                            "eglMakeCurrent failed, eglGetError() = " + EGL14.eglGetError()
                        )
                    }

                    try {
                        // NB we assume the surface has the same dimensions as the screen.
                        GLES20.glViewport(
                            0,
                            0,
                            surfaceHolder.surfaceFrame.width(),
                            surfaceHolder.surfaceFrame.height()
                        )

                        renderAndComposite(zonedDateTime)

                        if (!EGL14.eglSwapBuffers(eglDisplay, tempEglSurface)) {
                            Log.w(TAG, "eglSwapBuffers failed")
                        }
                    } finally {
                        EGL14.eglMakeCurrent(
                            eglDisplay,
                            EGL14.EGL_NO_SURFACE,
                            EGL14.EGL_NO_SURFACE,
                            EGL14.EGL_NO_CONTEXT
                        )
                        EGL14.eglDestroySurface(eglDisplay, tempEglSurface)
                    }
                }
            }

            this.renderParameters = prevRenderParameters
            renderParameters.isForScreenshot = originalIsForScreenshot
            surfaceHolder = originalSurfaceHolder
        }

        private fun renderAndComposite(zonedDateTime: ZonedDateTime) {
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)

            // Usually renderParameters.watchFaceWatchFaceLayers will be non-empty.
            if (renderParameters.watchFaceLayers.isNotEmpty()) {
                render(zonedDateTime)

                // Render and composite the HighlightLayer
                if (renderParameters.highlightLayer != null) {
                    renderBufferTexture.bindFrameBuffer()
                    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
                    renderHighlightLayer(zonedDateTime)
                    GLES20.glFlush()

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                    renderBufferTexture.compositeQuad()
                }
            } else {
                require(renderParameters.highlightLayer != null) {
                    "We don't support empty renderParameters.watchFaceWatchFaceLayers without a " +
                        "non-null renderParameters.highlightLayer"
                }
                renderHighlightLayer(zonedDateTime)
            }
        }

        internal override fun renderBlackFrame() {
            runBlocking {
                runUiThreadGlCommands {
                    GLES20.glClearColor(0f, 0f, 0f, 0f)
                    if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                        Log.w(TAG, "eglSwapBuffers failed")
                    }
                }
            }
        }

        /**
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Please note [WatchState.isAmbient] may not
         * match the [RenderParameters.drawMode] and should not be used to decide what to render.
         * E.g. when editing from the companion phone while the watch is ambient, renders may be
         * requested with [DrawMode.INTERACTIVE].
         *
         * Any highlights due to [RenderParameters.highlightLayer] should be rendered by
         * [renderHighlightLayer] instead where possible. For correct behavior this function must
         * use the supplied [ZonedDateTime] in favor of any other ways of getting the time.
         *
         * Note this function is called inside a lambda passed to [runUiThreadGlCommands] which has
         * synchronized access to the GL context.
         *
         * Note also `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` is called by the library
         * before this method.
         *
         * Before any calls to this function [onBackgroundThreadGlContextCreated] and
         * [onUiThreadGlSurfaceCreated] will have been called once on their respective threads.
         *
         * @param zonedDateTime The zonedDateTime [ZonedDateTime] to render with
         */
        @UiThread public abstract fun render(zonedDateTime: ZonedDateTime)

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear the buffer to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [ZonedDateTime] in favor of any other ways of getting the
         * time.
         *
         * Note this function is called inside a lambda passed to [runUiThreadGlCommands] which has
         * synchronized access to the GL context.
         *
         * Note also `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` is called by the library
         * before this method.
         *
         * @param zonedDateTime The zonedDateTime [ZonedDateTime] to render with
         */
        @UiThread public abstract fun renderHighlightLayer(zonedDateTime: ZonedDateTime)

        internal override fun dumpInternal(writer: IndentingPrintWriter) {
            writer.println("GlesRenderer:")
            writer.increaseIndent()
            writer.println("screenBounds=$screenBounds")
            writer.println(
                "interactiveDrawModeUpdateDelayMillis=$interactiveDrawModeUpdateDelayMillis"
            )
            writer.println("shouldAnimate=${shouldAnimate()}")
            renderParameters.dump(writer)
            onDump(writer.writer)
            writer.decreaseIndent()
        }

        override fun onDump(writer: PrintWriter) {}
    }

    /**
     * Watch faces that require [GLES20] rendering and are able to take advantage of [SharedAssets]
     * to save memory (there can be more than once instance when editing), should extend their
     * [Renderer] from this class. OpenGL objects created by [createSharedAssets] will be available
     * to all instances of the watch face on both threads.
     *
     * A GlesRenderer is expected to be constructed on the background thread associated with
     * [WatchFaceService.getBackgroundThreadHandler] inside a call to
     * [WatchFaceService.createWatchFace]. All rendering is be done on the UiThread. There is a
     * memory barrier between construction and rendering so no special threading primitives are
     * required.
     *
     * Two linked [EGLContext]s are created [eglBackgroundThreadContext] and [eglUiThreadContext]
     * which are associated with background and UiThread respectively and are shared by all
     * instances of the renderer. OpenGL objects created on (e.g. shaders and textures) can be used
     * on the other.
     *
     * If you need to make any OpenGl calls outside of [render],
     * [onBackgroundThreadGlContextCreated] or [onUiThreadGlSurfaceCreated] then you must use either
     * [runUiThreadGlCommands] or [runBackgroundThreadGlCommands] to execute a [Runnable] inside of
     * the corresponding context. Access to the GL contexts this way is necessary because GL
     * contexts are not shared between renderers and there can be multiple watch face instances
     * existing concurrently (e.g. headless and interactive, potentially from different watch faces
     * if an APK contains more than one [WatchFaceService]). In addition most drivers do not support
     * concurrent access.
     *
     * In Java it may be easier to extend [androidx.wear.watchface.ListenableGlesRenderer2] instead.
     *
     * @param SharedAssetsT The type extending [SharedAssets] returned by [createSharedAssets] and
     *   passed into [render] and [renderHighlightLayer].
     * @param surfaceHolder The [SurfaceHolder] whose [android.view.Surface] [render] will draw
     *   into.
     * @param currentUserStyleRepository The associated [CurrentUserStyleRepository].
     * @param watchState The associated [WatchState].
     * @param interactiveDrawModeUpdateDelayMillis The interval in milliseconds between frames in
     *   interactive [DrawMode]s. To render at 60hz set to 16. Note when battery is low, the frame
     *   rate will be clamped to 10fps. Watch faces are recommended to use lower frame rates if
     *   possible for better battery life. Variable frame rates can also help preserve battery life,
     *   e.g. if a watch face has a short animation once per second it can adjust the frame rate
     *   inorder to sleep when not animating.
     * @param eglConfigAttribList Attributes for [EGL14.eglChooseConfig]. By default this selects an
     *   RGBA8888 back buffer.
     * @param eglSurfaceAttribList The attributes to be passed to [EGL14.eglCreateWindowSurface]. By
     *   default this is empty.
     * @param eglContextAttribList The attributes to be passed to [EGL14.eglCreateContext]. By
     *   default this selects [EGL14.EGL_CONTEXT_CLIENT_VERSION] 2.
     * @throws [Renderer.GlesException] If any GL calls fail during initialization.
     */
    public abstract class GlesRenderer2<SharedAssetsT>
    @Throws(GlesException::class)
    @JvmOverloads
    @WorkerThread
    constructor(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
        eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,
        eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST,
        eglContextAttribList: IntArray = EGL_CONTEXT_ATTRIB_LIST
    ) :
        GlesRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            interactiveDrawModeUpdateDelayMillis,
            eglConfigAttribList,
            eglSurfaceAttribList,
            eglContextAttribList
        ) where SharedAssetsT : SharedAssets {
        /**
         * When editing multiple [WatchFaceService] instances and hence Renderers can exist
         * concurrently (e.g. a headless instance and an interactive instance) and using
         * [SharedAssets] allows memory to be saved by sharing immutable data (e.g. Bitmaps,
         * shaders, etc...) between them.
         *
         * To take advantage of SharedAssets, override this method. The constructed SharedAssets are
         * passed into the [render] as an argument (NB you'll have to cast this to your type). It is
         * safe to make GLES calls within this method.
         *
         * When all instances using SharedAssets have been closed, [SharedAssets.onDestroy] will be
         * called.
         *
         * Note that while SharedAssets are constructed on a background thread, they'll typically be
         * used on the main thread and subsequently destroyed there. The watch face library
         * constructs shared GLES contexts to allow resource sharing between threads.
         *
         * @return The [SharedAssetsT] that will be passed into [render] and [renderHighlightLayer].
         */
        @WorkerThread protected abstract suspend fun createSharedAssets(): SharedAssetsT

        /**
         * Sub-classes should override this to implement their watch face rendering logic which
         * should respect the current [renderParameters]. Please note [WatchState.isAmbient] may not
         * match the [RenderParameters.drawMode] and should not be used to decide what to render.
         * E.g. when editing from the companion phone while the watch is ambient, renders may be
         * requested with [DrawMode.INTERACTIVE].
         *
         * Any highlights due to [RenderParameters.highlightLayer] should be rendered by
         * [renderHighlightLayer] instead where possible. For correct behavior this function must
         * use the supplied [ZonedDateTime] in favor of any other ways of getting the time.
         *
         * Note this function is called inside a lambda passed to [runUiThreadGlCommands] which has
         * synchronized access to the GL context.
         *
         * Note also `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` is called by the library
         * before this method.
         *
         * Before any calls to this function [onBackgroundThreadGlContextCreated] and
         * [onUiThreadGlSurfaceCreated] will have been called once on their respective threads. In
         * addition if [SharedAssets] hasn't already been created, then a call to
         * [createSharedAssets] will have been completed before calling render.
         *
         * @param zonedDateTime The zonedDateTime [ZonedDateTime] to render with
         * @param sharedAssets The [SharedAssetsT] returned by [createSharedAssets]
         */
        @UiThread
        public abstract fun render(zonedDateTime: ZonedDateTime, sharedAssets: SharedAssetsT)

        final override suspend fun maybeCreateSharedAssets() {
            if (sharedAssetsHolder.sharedAssets == null) {
                sharedAssetsHolder.sharedAssets = createSharedAssets()
            }
        }

        /**
         * Sub-classes should override this to implement their watch face highlight layer rendering
         * logic for the [RenderParameters.highlightLayer] aspect of [renderParameters]. Typically
         * the implementation will clear the buffer to
         * [RenderParameters.HighlightLayer.backgroundTint] before rendering a transparent highlight
         * or a solid outline around the [RenderParameters.HighlightLayer.highlightedElement]. This
         * will be composited as needed on top of the results of [render]. For correct behavior this
         * function must use the supplied [ZonedDateTime] in favor of any other ways of getting the
         * time.
         *
         * Note this function is called inside a lambda passed to [runUiThreadGlCommands] which has
         * synchronized access to the GL context.
         *
         * Note also `GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)` is called by the library
         * before this method.
         *
         * @param zonedDateTime The zonedDateTime [ZonedDateTime] to render with
         * @param sharedAssets The [SharedAssetsT] returned by [createSharedAssets]
         */
        @UiThread
        public abstract fun renderHighlightLayer(
            zonedDateTime: ZonedDateTime,
            sharedAssets: SharedAssetsT
        )

        final override fun render(zonedDateTime: ZonedDateTime) {
            @Suppress("UNCHECKED_CAST") // We know the type is correct.
            render(zonedDateTime, sharedAssetsHolder.sharedAssets!! as SharedAssetsT)
        }

        final override fun renderHighlightLayer(zonedDateTime: ZonedDateTime) {
            @Suppress("UNCHECKED_CAST") // We know the type is correct.
            renderHighlightLayer(zonedDateTime, sharedAssetsHolder.sharedAssets!! as SharedAssetsT)
        }
    }
}
