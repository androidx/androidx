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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.icu.util.Calendar
import android.view.SurfaceHolder
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import androidx.wear.watchface.style.UserStyleRepository

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

/**
 * Watch faces that require [Canvas] rendering should extend their [Renderer] from this class.
 */
public abstract class CanvasRenderer(
    /** The [SurfaceHolder] from which a [Canvas] to will be obtained and passed into [render]. */
    surfaceHolder: SurfaceHolder,

    /** The watch face's associated [UserStyleRepository]. */
    userStyleRepository: UserStyleRepository,

    /** The watch face's associated [WatchState]. */
    watchState: WatchState,

    /** The type of canvas to request. */
    @CanvasType private val canvasType: Int,

    /**
     * The interval in milliseconds between frames in interactive [DrawMode]s. To render at 60hz
     * set to 16. Note when battery is low, the frame rate will be clamped to 10fps. Watch faces are
     * recommended to use lower frame rates if possible for better battery life. Variable frame
     * rates can also help preserve battery life, e.g. if a watch face has a short animation once
     * per second it can adjust the frame rate inorder to sleep when not animating.
     */
    @IntRange(from = 0, to = 10000)
    interactiveDrawModeUpdateDelayMillis: Long
) : Renderer(surfaceHolder, userStyleRepository, watchState, interactiveDrawModeUpdateDelayMillis) {

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
