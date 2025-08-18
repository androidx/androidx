/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.skiko

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.RTreeFactory
import org.jetbrains.skiko.SkikoRenderDelegate

internal class RecordDrawRectRenderDecorator(
    private val decorated: SkikoRenderDelegate,
    private val onDrawRectChange: (Rect) -> Unit
) : SkikoRenderDelegate by decorated {
    private val pictureRecorder = PictureRecorder()
    private val bbhFactory = RTreeFactory()
    private var drawRect = Rect.Zero
        set(value) {
            if (value != field) {
                field = value
                onDrawRectChange(value)
            }
        }

    // TODO(@MatkovIvan): nobody calls it
    fun close() {
        pictureRecorder.close()
        bbhFactory.close()
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        drawRect = canvas.recordCullRect {
            decorated.onRender(it, width, height, nanoTime)
        }?.toComposeRect() ?: Rect.Zero
    }

    private inline fun Canvas.recordCullRect(
        block: (Canvas) -> Unit
    ): SkRect? {
        val pictureCanvas = pictureRecorder.beginRecording(SkRect.Unconstrained, bbhFactory)
        pictureCanvas.translate(MeasureOffset, MeasureOffset)
        block(pictureCanvas)
        val picture = pictureRecorder.finishRecordingAsPicture()
        try {
            save()
            translate(-MeasureOffset, -MeasureOffset)
            drawPicture(picture, null, null)
            restore()
            return if (!picture.cullRect.isEmpty) {
                picture.cullRect.offset(-MeasureOffset, -MeasureOffset)
            } else {
                // It means that there ware no drawings.
                // Applying our offset is incorrect in this case.
                null
            }
        } finally {
            picture.close()
        }
    }
}

/**
 * Skia cannot return negative values in [Picture.cullRect],
 * so temporary applying some offset is required to get right measurement in negative area.
 */
private const val MeasureOffset = (1 shl 14).toFloat()

private val SkRect.Companion.Unconstrained: SkRect
    get() = makeLTRB(
        l = Float.MIN_VALUE,
        t = Float.MIN_VALUE,
        r = Float.MAX_VALUE,
        b = Float.MAX_VALUE
    )
