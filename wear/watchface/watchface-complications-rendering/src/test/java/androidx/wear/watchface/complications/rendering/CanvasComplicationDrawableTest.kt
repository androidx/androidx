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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(ComplicationsTestRunner::class)
public class CanvasComplicationDrawableTest {
    private val complicationDrawable =
        ComplicationDrawable(ApplicationProvider.getApplicationContext())
    private val watchState = MutableWatchState()
    private val invalidateCallback = Mockito.mock(CanvasComplication.InvalidateCallback::class.java)
    private val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val bounds = Rect(0, 0, 100, 100)
    private val canvas = Canvas(bitmap)
    private val zonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234), ZoneId.of("UTC"))
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
            zonedDateTime,
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
            zonedDateTime,
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
            zonedDateTime,
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
        canvasComplicationDrawable.render(
            canvas,
            bounds,
            zonedDateTime,
            RenderParameters(
                DrawMode.INTERACTIVE,
                setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
                null,
                emptyMap()
            ),
            slotId
        )
        assertThat(complicationDrawable.currentTime.toEpochMilli()).isEqualTo(1234)
    }

    @Test
    public fun render_highlight() {
        val renderParameters = RenderParameters(
            DrawMode.INTERACTIVE,
            setOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS),
            null,
            mapOf(slotId to TapEvent(50, 50, Instant.ofEpochMilli(1100)))
        )

        val t1099 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1099), ZoneId.of("UTC"))
        canvasComplicationDrawable.render(canvas, bounds, t1099, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isFalse()

        val t1100 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1100), ZoneId.of("UTC"))
        canvasComplicationDrawable.render(canvas, bounds, t1100, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isTrue()

        val t1099_plus = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(
                1099 + CanvasComplicationDrawable.COMPLICATION_HIGHLIGHT_DURATION_MS
            ),
            ZoneId.of("UTC")
        )

        canvasComplicationDrawable.render(canvas, bounds, t1099_plus, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isTrue()

        val t1100_plus = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(
                1100 + CanvasComplicationDrawable.COMPLICATION_HIGHLIGHT_DURATION_MS
            ),
            ZoneId.of("UTC")
        )
        canvasComplicationDrawable.render(canvas, bounds, t1100_plus, renderParameters, slotId)
        assertThat(complicationDrawable.isHighlighted).isFalse()
    }
}
