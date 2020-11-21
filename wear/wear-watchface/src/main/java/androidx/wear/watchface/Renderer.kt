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

import android.graphics.Bitmap
import android.graphics.Rect
import android.icu.util.Calendar
import android.view.SurfaceHolder
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.wear.watchface.style.UserStyleRepository

/** The base class for [CanvasRenderer] and [GlesRenderer]. */
public abstract class Renderer(
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
     * Called when the [DrawMode] has been updated. Will always be called before the first
     * call to onDraw().
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

    /** Schedules a call to [renderInternal] to draw the next frame. */
    @UiThread
    public fun invalidate() {
        watchFaceHostApi.invalidate()
    }

    /**
     * Posts a message to schedule a call to [renderInternal] to draw the next frame. Unlike
     * [invalidate], this method is thread-safe and may be called on any thread.
     */
    public fun postInvalidate() {
        watchFaceHostApi.getHandler().post { watchFaceHostApi.invalidate() }
    }
}
