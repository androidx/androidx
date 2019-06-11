/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.graphics.ColorSpace
import androidx.ui.painting.Image
import androidx.ui.painting.ImageConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImageTest {

    @Test
    fun testCreatedImage() {
        val cs = ColorSpace.get(ColorSpace.Named.DisplayP3)
        val image = Image(
            width = 10,
            height = 20,
            config = ImageConfig.Argb8888,
            hasAlpha = false,
            colorSpace = cs
        )

        assertEquals(10, image.width)
        assertEquals(20, image.height)
        assertEquals(ImageConfig.Argb8888, image.config)
        assertFalse(image.hasAlpha)
        assertEquals(cs, image.colorSpace)
    }
}