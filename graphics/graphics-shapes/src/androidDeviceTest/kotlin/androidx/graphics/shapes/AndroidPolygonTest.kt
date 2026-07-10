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

import android.graphics.Matrix
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class AndroidPolygonTest {
    @Test
    fun transformKeepsContiguousAnchorsEqual() {
        val poly =
            RoundedPolygon(radius = 1f, numVertices = 4, rounding = CornerRounding(7 / 15f))
                .transformed(
                    Matrix().apply {
                        postRotate(45f)
                        postScale(648f, 648f)
                        postTranslate(540f, 1212f)
                    }
                )
        poly.cubics.indices.forEach { i ->
            // It has to be the same point
            assertEquals(
                "Failed at X, index $i",
                poly.cubics[i].anchor1X,
                poly.cubics[(i + 1) % poly.cubics.size].anchor0X,
                0f,
            )
            assertEquals(
                "Failed at Y, index $i",
                poly.cubics[i].anchor1Y,
                poly.cubics[(i + 1) % poly.cubics.size].anchor0Y,
                0f,
            )
        }
    }
}
