/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(ComplicationsTestRunner::class)
public class CanvasComplicationDrawableTest {
    private val complicationDrawable =
        ComplicationDrawable(ApplicationProvider.getApplicationContext())
    private val watchState = MutableWatchState()
    private val invalidateCallback = Mockito.mock(CanvasComplication.InvalidateCallback::class.java)
    private val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val bounds = Rect(0, 0, 100, 100)
    private val canvas = Canvas(bitmap)
    private val calendar = Calendar.getInstance(TimeZone.GMT_ZONE)
    private val canvasComplicationDrawable = CanvasComplicationDrawable(
        complicationDrawable,
        watchState.asWatchState(),
        invalidateCallback
    )
    private val slotId = 100

    @Test
    public fun render_ambientMode() {
        canvasComplicationDrawable.render(
            canvas,
            bounds,
            calendar,
            RenderParameters(
                DrawMode.AMBIENT,
                setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
                null,
                emptyMap()
            ),
            slotId
        )
        assertThat(complicationDrawable.isInAmbientMode).isTrue()

        canvasComplicationDrawable.render(
            canvas,
            bounds,
            calendar,
            RenderParameters(
                DrawMode.INTERACTIVE,
                setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
                null,
                emptyMap()
            ),
            slotId
        )
        assertThat(complicationDrawable.isInAmbientMode).isFalse()
    }

    @Test
    public fun render_bounds() {
        canvasComplicationDrawable.render(
            canvas,
            bounds,
            calendar,
            RenderParameters(
                DrawMode.INTERACTIVE,
                setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
                null,
                emptyMap()
            ),
            slotId
        )
        assertThat(complicationDrawable.bounds).isEqualTo(bounds)
    }

    @Test
    public fun render_currentTimeMillis() {
        calendar.timeInMillis = 1234
        canvasComplicationDrawable.render(
            canvas,
            bounds,
            calendar,
            RenderParameters(
                DrawMode.INTERACTIVE,
                setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
                null,
                emptyMap()
            ),
            slotId
        )
        assertThat(complicationDrawable.currentTimeMillis).isEqualTo(1234)
    }

    @Test
    public fun render_highlight() {
        val renderParameters = RenderParameters(
            DrawMode.INTERACTIVE,
            setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
            null,
            mapOf(slotId to TapEvent(50, 50, 1100))
        )

        calendar.timeInMillis = 1099
        canvasComplicationDrawable.render(canvas, bounds, calendar, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isFalse()

        calendar.timeInMillis = 1100
        canvasComplicationDrawable.render(canvas, bounds, calendar, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isTrue()

        calendar.timeInMillis = 1099 + CanvasComplicationDrawable.COMPLICATION_HIGHLIGHT_DURATION_MS
        canvasComplicationDrawable.render(canvas, bounds, calendar, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isTrue()

        calendar.timeInMillis = 1100 + CanvasComplicationDrawable.COMPLICATION_HIGHLIGHT_DURATION_MS
        canvasComplicationDrawable.render(canvas, bounds, calendar, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isFalse()
    }
}