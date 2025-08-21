/*
 * Copyright 2025 The Android Open Source Project
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
import android.graphics.Matrix
import androidx.core.graphics.get
import org.junit.Assert.assertEquals

internal fun pointRotator(angle: Float): PointTransformer {
    val matrix = Matrix().apply { setRotate(angle) }
    return PointTransformer { x, y ->
        val point = floatArrayOf(x, y)
        matrix.mapPoints(point)
        TransformResult(point[0], point[1])
    }
}

internal fun assertBitmapsEqual(b0: Bitmap, b1: Bitmap) {
    assertEquals(b0.width, b1.width)
    assertEquals(b0.height, b1.height)
    for (row in 0 until b0.height) {
        for (col in 0 until b0.width) {
            assertEquals("Pixels at ($col, $row) not equal", b0.get(col, row), b1.get(col, row))
        }
    }
}
