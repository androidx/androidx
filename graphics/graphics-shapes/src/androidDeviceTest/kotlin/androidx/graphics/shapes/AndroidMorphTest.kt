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

package androidx.graphics.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.scaleMatrix
import androidx.test.filters.SmallTest
import org.junit.Test

@SmallTest
class AndroidMorphTest : MorphTest() {

    /**
     * This test checks to see whether a morph between two different polygons is correct at the
     * start (progress 0) and end (progress 1). The actual cubics of the morph vs the polygons it
     * was constructed from may differ, due to the way the morph is constructed, but the rendering
     * result should be the same.
     */
    @Test
    fun morphDrawingTest() {
        // Shapes are in canonical size of 2x2 around center (.5, .5). Translate/scale to
        // get a larger path
        val m = scaleMatrix(SCALE, SCALE)
        m.postTranslate(SCALE / 2f, SCALE / 2F)
        val poly1Path = poly1.toPath()
        poly1Path.transform(m)
        val poly2Path = poly2.toPath()
        poly2Path.transform(m)
        val morph120Path = morph12.toPath(0f)
        morph120Path.transform(m)
        val morph121Path = morph12.toPath(1f)
        morph121Path.transform(m)

        val polyBitmap = Bitmap.createBitmap(RADIUS * 2, RADIUS * 2, Bitmap.Config.ARGB_8888)
        val morphBitmap = Bitmap.createBitmap(RADIUS * 2, RADIUS * 2, Bitmap.Config.ARGB_8888)
        val polyCanvas = Canvas(polyBitmap)
        val morphCanvas = Canvas(morphBitmap)

        // Check that the morph at progress 0 is equivalent to poly1
        drawTestPath(polyCanvas, poly1Path)
        drawTestPath(morphCanvas, morph120Path)
        assertBitmapsEqual(polyBitmap, morphBitmap)

        // Check that the morph at progress 1 is equivalent to poly2
        drawTestPath(polyCanvas, poly2Path)
        drawTestPath(morphCanvas, morph121Path)
        assertBitmapsEqual(polyBitmap, morphBitmap)
    }

    /** Utility function - Fill the canvas with black and draw the path in white. */
    private fun drawTestPath(canvas: Canvas, path: Path) {
        canvas.drawColor(Color.BLACK)
        val paint = Paint()
        paint.isAntiAlias = false
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawPath(path, paint)
    }
}
