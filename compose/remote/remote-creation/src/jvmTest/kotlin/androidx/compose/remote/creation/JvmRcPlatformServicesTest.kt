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
package androidx.compose.remote.creation

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JvmRcPlatformServicesTest {

    private val services = JvmRcPlatformServices()

    @Test
    fun testImageToByteArray() {
        // Create a simple 10x10 red image
        val width = 10
        val height = 10
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = java.awt.Color.RED
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()

        // Convert to byte array
        val bytes = services.imageToByteArray(image)
        assertNotNull("Byte array should not be null", bytes)
        assertTrue("Byte array should not be empty", bytes!!.isNotEmpty())

        // Verify it's a valid PNG
        val inputStream = ByteArrayInputStream(bytes)
        val readImage = ImageIO.read(inputStream)
        assertNotNull("Should be able to read back the image", readImage)
        assertEquals(width, readImage.width)
        assertEquals(height, readImage.height)
    }

    @Test
    fun testGetImageDimensions() {
        val width = 20
        val height = 30
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        assertEquals(width, services.getImageWidth(image))
        assertEquals(height, services.getImageHeight(image))
    }

    @Test
    fun testIsAlpha8Image() {
        val argbImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        assertFalse(services.isAlpha8Image(argbImage))

        val grayImage = BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY)
        assertTrue(services.isAlpha8Image(grayImage))
    }
}
