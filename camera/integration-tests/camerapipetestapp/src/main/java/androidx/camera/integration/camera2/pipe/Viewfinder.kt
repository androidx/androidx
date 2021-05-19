/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.camera.integration.camera2.pipe

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.GuardedBy
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A Surface-view based Android Camera Viewfinder.
 *
 * This ui element has three modes:
 * - Fit    (Scale the preview until it is within this ui element)
 * - Fill   (Scale the preview until it completely fills this ui element, even if it clips)
 * - Center (Do not scale the preview, and keep it centered).
 *
 * To use the viewfinder, call configure with the desired surface size, mode, and format.
 */
@Suppress("DEPRECATION")
class Viewfinder(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    private val surfaceView: SurfaceView
    private val displaySize: Point = Point()

    @GuardedBy("this")
    private var setFixedSize = true

    @GuardedBy("this")
    private var _viewfinderLayout = ViewfinderLayout.FIT

    @GuardedBy("this")
    private var viewfinderSize: Size? = null

    @GuardedBy("this")
    private var surfaceState: SurfaceState? = null

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        // Parse out the custom viewfinderLayout, if one is set.
        if (attrs != null) {
            val layoutMode: String? = attrs.getAttributeValue(null, "viewfinderLayout")
            if (layoutMode != null) {
                _viewfinderLayout =
                    ViewfinderLayout.parseString(layoutMode)
            }
            val fixedSize: String? = attrs.getAttributeValue(null, "setFixedSize")
            if (fixedSize != null) {
                setFixedSize = java.lang.Boolean.parseBoolean(fixedSize)
            }
        }
        surfaceView = SurfaceView(context)
        addView(surfaceView)

        // Make the viewfinder work better in editors by defining a placeholder viewfinder size.
        if (isInEditMode) {
            configure(
                Size(480, 640),
                object : SurfaceListener {
                    override fun onSurfaceChanged(surface: Surface?, size: Size?) {}
                }
            )
        }
    }

    /**
     * Gets the current viewfinder layout mode.
     */
    var viewfinderLayout: ViewfinderLayout
        get() = synchronized(this) { _viewfinderLayout }
        set(value) {
            var shouldRequestLayout = false
            synchronized(this) {
                if (this._viewfinderLayout != value) {
                    this._viewfinderLayout = value
                    shouldRequestLayout = true
                }
            }
            if (shouldRequestLayout) {
                requestLayout()
            }
        }

    /** Tell if Surface size is fixed. Call this before the view enters the hierarchy.  */
    fun setFixedSize(value: Boolean) {
        synchronized(this) {
            check(parent == null)
            setFixedSize = value
        }
    }

    /**
     * Sets the desired viewfinder size and a listener that will be invoked when the Surface is
     * configured with that size.
     */
    fun configure(viewfinderSize: Size, listener: SurfaceListener) {
        configure(viewfinderSize, PixelFormat.UNKNOWN, listener)
    }

    /**
     * Sets the desired viewfinder size, pixel format, and a listener that will be invoked once the
     * Surface is configured with that size.
     */
    fun configure(
        viewfinderSize: Size,
        viewfinderPixelFormat: Int,
        listener: SurfaceListener
    ) {
        synchronized(this) {
            this.viewfinderSize = viewfinderSize
            var surfaceState = surfaceState
            if (surfaceState == null) {
                val surfaceHolder: SurfaceHolder = checkNotNull(surfaceView.holder)
                surfaceState = SurfaceState(surfaceHolder)
                surfaceHolder.addCallback(surfaceState)
                this.surfaceState = surfaceState
            }
            debugLog { "configured: $viewfinderSize with $viewfinderPixelFormat" }
            surfaceState.configure(viewfinderSize, viewfinderPixelFormat, listener, setFixedSize)
            // TODO(codelogic): May or may not require a call to "requestLayout()".
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        display.getRealSize(displaySize)
        val viewWidth: Int = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight: Int = MeasureSpec.getSize(heightMeasureSpec)
        debugLog { "onMeasure:          " + viewWidth + "x" + viewHeight }

        var vfSize: Size?
        var vfLayout: ViewfinderLayout
        synchronized(this) {
            vfSize = viewfinderSize
            vfLayout = _viewfinderLayout
        }
        if (vfSize == null || vfSize!!.area() == 0L) {
            debugLog { "  Viewfinder size not available yet" }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            measureAllChildren(viewWidth, viewHeight)
        } else {
            debugLog { "  Viewfinder size of $vfSize" }
            val scaled: Size = computeUiSize(
                vfSize!!,
                vfLayout,
                displaySize,
                viewWidth,
                viewHeight
            )
            setMeasuredDimension(
                scaled.width.coerceAtMost(viewWidth),
                scaled.height.coerceAtMost(viewHeight)
            )
            measureAllChildren(scaled.width, scaled.height)
        }
    }

    private fun measureAllChildren(width: Int, height: Int) {
        val count: Int = childCount
        for (x in 0 until count) {
            val view: View = getChildAt(x)
            if (view.visibility != View.GONE) {
                var childWidth: Int
                var childHeight: Int
                val params: LayoutParams = view.layoutParams
                childWidth = when (params.width) {
                    LayoutParams.MATCH_PARENT -> {
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                    }
                    LayoutParams.WRAP_CONTENT -> {
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
                    }
                    else -> {
                        MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY)
                    }
                }
                childHeight = when (params.height) {
                    LayoutParams.MATCH_PARENT -> {
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                    }
                    LayoutParams.WRAP_CONTENT -> {
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                    }
                    else -> {
                        MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY)
                    }
                }
                view.measure(childWidth, childHeight)
            }
        }
    }

    override fun onLayout(
        changed: Boolean,
        layoutLeft: Int,
        layoutTop: Int,
        layoutRight: Int,
        layoutBottom: Int
    ) {
        debugLog { "onLayout:           $layoutLeft, $layoutTop, $layoutRight, $layoutBottom" }

        var vfSize: Size?
        var vfLayout: ViewfinderLayout
        synchronized(this) {
            vfSize = viewfinderSize
            vfLayout = _viewfinderLayout
        }

        var left = layoutLeft
        var top = layoutTop
        var right = layoutRight
        var bottom = layoutBottom

        // If the size is not yet configured, just leave it to be the same as the container view.
        if (vfSize != null) {
            // Compute the viewfinder ui size based on the available width, height, and ui orientation.
            val viewWidth = right - left
            val viewHeight = bottom - top
            val scaled: Size = computeUiSize(
                vfSize!!,
                vfLayout,
                displaySize,
                viewWidth,
                viewHeight
            )

            // Compute the center of the view.
            val centerX = viewWidth / 2
            val centerY = viewHeight / 2

            // Compute the left / top / right / bottom values such that viewfinder is centered.
            left = centerX - scaled.width / 2
            top = centerY - scaled.height / 2
            right = left + scaled.width
            bottom = top + scaled.height
            debugLog { "  Size and rotation OK, surface is $left, $top, $right, $bottom" }
        } else {
            debugLog { "  Size and rotation missing" }
        }
        val count: Int = childCount
        for (x in 0 until count) {
            val view: View = getChildAt(x)
            if (view.visibility != View.GONE) {
                if (view === surfaceView) {
                    view.layout(left, top, right, bottom)
                } else {
                    val childWidth: Int = view.measuredWidth
                    val childHeight: Int = view.measuredHeight
                    debugLog { "  Child $x has size $childWidth, $childHeight" }

                    // TODO: Consider adding support for one or more of: padding, margins, layout_gravity.
                    // Currently all children are positioned at the top-left corner of the surfaceView.
                    view.layout(left, top, left + childWidth, top + childHeight)
                }
            }
        }
    }

    /**
     * Listener that is aware of the currently desired size and caches the last configured Surface.
     *
     * - Reconfiguring with the same Size will immediately invoke the listener callback.
     * - Reconfiguring with a new size will call setFixedSize on the surface.
     * - Listener callback is only invoked with the configured size and surface size match.
     */
    private class SurfaceState(private val surfaceHolder: SurfaceHolder) :
        SurfaceHolder.Callback2 {
        private var configured = false

        private var lastSize: Size? = null

        private var lastSurface: Surface? = null
        private var configuredPixelFormat = 0
        private var configuredSize: Size? = null
        private var configuredListener: SurfaceListener? = null

        /** Tell this object to configure the surface to match the desired size and pixel format.  */
        fun configure(
            size: Size,
            pixelFormat: Int,
            listener: SurfaceListener,
            setFixedSize: Boolean
        ) {
            synchronized(this) {
                // Initialization
                // Do all size comparisons in Portrait orientation to avoid identical, but otherwise
                // rotated sizes from triggering updates.
                val portraitSize: Size = size.asPortrait()

                // Special Case #1
                // If the configuration is identical, do nothing.
                if (configured &&
                    portraitSize == configuredSize &&
                    pixelFormat == configuredPixelFormat &&
                    listener === configuredListener
                ) {
                    // If the configured size and listener are identical, then do nothing.
                    return
                }

                // Update the listener, but do not update size and format (yet).
                configuredListener = listener

                // Special Case #2
                // If the size and format are the same, but the listener is different, check to see
                // if the last configured size and surface are valid and also match. If so,
                // immediately invoke the listener and return.
                if (portraitSize == configuredSize &&
                    pixelFormat == configuredPixelFormat
                ) {
                    val lastPortraitSize = lastSize?.asPortrait()
                    if (lastSurface != null &&
                        lastPortraitSize != null &&
                        portraitSize == lastPortraitSize
                    ) {
                        listener.onSurfaceChanged(lastSurface, lastSize)
                    }
                    return
                }

                // At this point, we know that configured size or format is different, and we need to
                // force the SurfaceHolder to reconfigure.
                configuredPixelFormat = pixelFormat
                configuredSize = portraitSize
                configuredListener = listener

                // Clear last known size since we are calling setFixedSize and setFormat.
                lastSize = null
                lastSurface = null
                configured = true
                if (pixelFormat != 0) {
                    surfaceHolder.setFormat(pixelFormat)
                }
                if (setFixedSize) {
                    surfaceHolder.setFixedSize(size.width, size.height)
                }
            }
        }

        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {}
        override fun surfaceCreated(holder: SurfaceHolder) {
            debugLog { "Surface Created" }
            // Ignored. This is not useful until we know the configured format, width, height.
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            synchronized(this) {
                val size = Size(width, height)
                val portraitSize: Size = size.asPortrait()
                val lastPortraitSize = lastSize?.asPortrait()

                if (!(lastPortraitSize != null && portraitSize == lastPortraitSize) &&
                    portraitSize == configuredSize
                ) {
                    val surface: Surface? = holder.surface

                    // Check to make sure the surface is not null and valid.
                    if (surface != null && surface.isValid) {
                        lastSize = size
                        lastSurface = surface
                        debugLog { "Surface Configured" }
                        configuredListener?.onSurfaceChanged(surface, size)
                    }
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            synchronized(this) {
                debugLog { "Surface Destroyed" }
                lastSize = null
                lastSurface = null
                configured = false
                configuredListener?.onSurfaceChanged(null, null)
            }
        }
    }

    /** Measure and Layout modes for the viewfinder.  */
    enum class ViewfinderLayout {
        FIT, FILL, CENTER;

        companion object {
            fun parseString(layoutMode: String): ViewfinderLayout {
                return when (layoutMode.lowercase(Locale.ROOT)) {
                    "fit" -> FIT
                    "fill" -> FILL
                    "center" -> CENTER
                    else -> throw IllegalStateException(
                        "Unknown viewfinderLayout: $layoutMode available values are: " +
                            "[fit, fill, center]"
                    )
                }
            }
        }
    }

    /**
     * Listener for observing Surface configuration changes.
     */
    interface SurfaceListener {
        /**
         * Invoked when the Surface's configuration has changed, or null if the Surface was destroyed.
         *
         *
         * If surface is non-null, then size will be non-null.
         */
        fun onSurfaceChanged(surface: Surface?, size: Size?)
    }

    companion object {
        private const val DEBUG = false

        /**
         * Compute a new size based on the viewfinder size, ui orientation, and layout mode.
         */
        internal fun computeUiSize(
            size: Size,
            viewfinderLayout: ViewfinderLayout,
            displaySize: Point,
            viewWidth: Int,
            viewHeight: Int
        ): Size {

            // While this may appear to be overly simplified, the Android display system will
            // accounts for most of the rotation edge cases when the UI surface is passed directly
            // to the camera. This may *NOT* be the case when handling other aspects of the
            // display-to-sensor interactions.
            // - Landscape-natural devices have landscape-aligned sensors (Enforced by CDD).
            // - Portrait-natural devices have portrait-aligned sensors (Enforced by CDD).
            // - Sizes are reported in sensor-aligned coordinates, which reads out long side first.
            // - The displaySize changes depending on the orientation of the display.
            // - The situations where the size needs to be flipped reduces down to "display is in
            //   portrait orientation".
            // - The UI width / height should *not* be used to figure this out, since multi-window
            //   will cause the aspect ratio of the Activity to not match the display.
            val alignedSize = if (displaySize.x < displaySize.y) {
                Size(size.height, size.width)
            } else {
                size
            }

            val width: Int
            val height: Int
            val sizeRatio: Float = alignedSize.width / alignedSize.height.toFloat()
            val viewRatio = viewWidth / viewHeight.toFloat()
            return when (viewfinderLayout) {
                ViewfinderLayout.FIT -> {
                    // Match longest sides together.
                    if (sizeRatio > viewRatio) {
                        width = viewWidth
                        height = (viewWidth / sizeRatio).roundToInt()
                    } else {
                        width = (viewHeight * sizeRatio).roundToInt()
                        height = viewHeight
                    }
                    Size(width, height)
                }
                ViewfinderLayout.FILL -> {
                    // Match shortest sides together.
                    if (sizeRatio < viewRatio) {
                        width = viewWidth
                        height = (viewWidth / sizeRatio).roundToInt()
                    } else {
                        width = (viewHeight * sizeRatio).roundToInt()
                        height = viewHeight
                    }
                    Size(width, height)
                }
                ViewfinderLayout.CENTER -> alignedSize
            }
        }

        /**
         * Debug logging that can be enabled.
         */
        internal inline fun debugLog(crossinline msg: () -> String) {
            if (DEBUG) {
                Log.i("Viewfinder", msg())
            }
        }

        /**
         * Utility method for converting an displayRotation int into a human readable string.
         */
        private fun displayRotationToString(displayRotation: Int): String {
            return if (
                displayRotation == Surface.ROTATION_0 ||
                displayRotation == Surface.ROTATION_180
            ) {
                "Portrait-${displayRotation * 90}"
            } else if (
                displayRotation == Surface.ROTATION_90 ||
                displayRotation == Surface.ROTATION_270
            ) {
                "Landscape-${displayRotation * 90}"
            } else {
                "Unknown"
            }
        }
    }
}

internal fun Size.asPortrait(): Size {
    return if (this.width >= this.height) {
        Size(this.height, this.width)
    } else {
        this
    }
}

internal fun Size.area(): Long {
    return this.width * this.height.toLong()
}
