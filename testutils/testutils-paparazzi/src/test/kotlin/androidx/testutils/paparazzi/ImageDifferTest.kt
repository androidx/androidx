/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.testutils.paparazzi

import androidx.testutils.paparazzi.ImageDiffer.DiffResult.Different
import androidx.testutils.paparazzi.ImageDiffer.DiffResult.Similar
import androidx.testutils.paparazzi.ImageDiffer.PixelPerfect
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ImageDifferTest {
    @Test
    fun `PixelPerfect similar`() {
        val result = PixelPerfect.diff(loadTestImage("circle"), loadTestImage("circle"))
        assertIs<Similar>(result)
        assertEquals("0 of 65536 pixels different", result.description)
        assertNull(result.highlights)
    }

    @Test
    fun `PixelPerfect different`() {
        val result = PixelPerfect.diff(loadTestImage("circle"), loadTestImage("star"))
        assertIs<Different>(result)
        assertEquals("17837 of 65536 pixels different", result.description)
        assertIs<Similar>(
            PixelPerfect.diff(result.highlights, loadTestImage("PixelPerfect_diff"))
        )
    }

    @Test
    fun `PixelPerfect name`() {
        assertEquals("PixelPerfect", PixelPerfect.name)
    }

    private fun loadTestImage(name: String) =
        ImageIO.read(javaClass.getResourceAsStream("$name.png")!!)
}
