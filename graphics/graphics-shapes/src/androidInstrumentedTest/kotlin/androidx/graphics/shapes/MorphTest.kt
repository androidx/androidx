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
class MorphTest {

    val RADIUS = 50
    val SCALE = RADIUS.toFloat()

    val poly1 = RoundedPolygon(3, centerX = .5f, centerY = .5f)
    val poly2 = RoundedPolygon(4, centerX = .5f, centerY = .5f)
    val morph11 = Morph(poly1, poly1)
    val morph12 = Morph(poly1, poly2)

    /**
     * Simple test to verify that a Morph with the same start and end shape has curves
     * equivalent to those in that shape.
     */
    @Test
    fun cubicsTest() {
        val p1Cubics = poly1.cubics
        val cubics11 = morph11.asCubics(0f)
        assert(cubics11.size > 0)
        // The structure of a morph and its component shapes may not match exactly, because morph
        // calculations may optimize some of the zero-length curves out. But in general, every
        // curve in the morph *should* exist somewhere in the shape it is based on, so we
        // do an exhaustive search for such existence. Note that this assertion only works because
        // we constructed the Morph from/to the same shape. A Morph between different shapes
        // may not have the curves replicated exactly.
        for (morphCubic in cubics11) {
            var matched = false
            for (p1Cubic in p1Cubics) {
                if (cubicsEqualish(morphCubic, p1Cubic)) {
                    matched = true
                    continue
                }
            }
            assert(matched)
        }
    }

    /**
     * This test checks to see whether a morph between two different polygons is correct
     * at the start (progress 0) and end (progress 1). The actual cubics of the morph vs the
     * polygons it was constructed from may differ, due to the way the morph is constructed,
     * but the rendering result should be the same.
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

    /**
     * Utility function - Fill the canvas with black and draw the path in white.
     */
    private fun drawTestPath(canvas: Canvas, path: Path) {
        canvas.drawColor(Color.BLACK)
        val paint = Paint()
        paint.isAntiAlias = false
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawPath(path, paint)
    }
}
