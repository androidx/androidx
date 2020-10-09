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
import androidx.annotation.UiThread
import androidx.wear.watchface.style.UserStyleRepository

/** @hide */
@IntDef(
    value = [
        CanvasType.SOFTWARE,
        CanvasType.HARDWARE
    ]
)
annotation class CanvasType {
    companion object {
        /** A software canvas will be requested. */
        const val SOFTWARE = 0

        /**
         * A hardware canvas will be requested. This is usually faster than software rendering,
         * however it can sometimes increase battery usage by rendering at a higher frame rate.
         */
        const val HARDWARE = 1
    }
}

/**
 * Watch faces that require [Canvas] rendering should extend their [Renderer] from this
 * class.
 */
abstract class CanvasRenderer(
    /** The [SurfaceHolder] that [render] will draw into. */
    surfaceHolder: SurfaceHolder,

    /** The associated [UserStyleRepository]. */
    userStyleRepository: UserStyleRepository,

    /** The associated [WatchState]. */
    watchState: WatchState,

    /** The type of canvas to use. */
    @CanvasType private val canvasType: Int
) : Renderer(surfaceHolder, userStyleRepository, watchState) {

    internal override fun renderInternal(
        calendar: Calendar
    ) {
        val canvas = (
            if (canvasType == CanvasType.HARDWARE) {
                surfaceHolder.lockHardwareCanvas()
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
     * the current [DrawMode]. For correct functioning watch faces must use the supplied
     * [Calendar] and avoid using any other ways of getting the time.
     *
     * @param canvas The [Canvas] to render into. Don't assume this is always the canvas from
     *     the [SurfaceHolder] backing the display
     * @param bounds A [Rect] describing the bonds of the canvas to draw into
     * @param calendar The current [Calendar]
     */
    @UiThread
    abstract fun render(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar
    )
}
