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

package androidx.compose.ui.graphics

import android.graphics.ColorSpace
import android.graphics.ColorSpace.Named
import android.graphics.ColorSpace.Rgb
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmapTest.ColorSpaceHelper.Companion.colorSpaceTestHelper
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.TransferParameters
import androidx.compose.ui.graphics.colorspace.WhitePoint
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress

@SmallTest
@RunWith(AndroidJUnit4::class)
class ImageBitmapTest {

    @Test
    fun testCreatedImage() {
        val cs = ColorSpaces.Srgb
        val image = ImageBitmap(
            width = 10,
            height = 20,
            config = ImageBitmapConfig.Argb8888,
            hasAlpha = false,
            colorSpace = cs
        )

        assertEquals(10, image.width)
        assertEquals(20, image.height)
        assertEquals(ImageBitmapConfig.Argb8888, image.config)
        assertFalse(image.hasAlpha)
        assertEquals(cs, image.colorSpace)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Srgb, // Compose
            ColorSpace.get(Named.SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAcesColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Aces, // Compose
            ColorSpace.get(Named.ACES) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAcescgColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Acescg, // Compose
            ColorSpace.get(Named.ACESCG) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAdobeRgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.AdobeRgb, // Compose
            ColorSpace.get(Named.ADOBE_RGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBt2020Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Bt2020, // Compose
            ColorSpace.get(Named.BT2020) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBt709Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Bt709, // Compose
            ColorSpace.get(Named.BT709) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCieLabColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.CieLab, // Compose
            ColorSpace.get(Named.CIE_LAB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCieXyzColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.CieXyz, // Compose
            ColorSpace.get(Named.CIE_XYZ) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDciP3Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.DciP3, // Compose
            ColorSpace.get(Named.DCI_P3) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDisplayP3Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.DisplayP3, // Compose
            ColorSpace.get(Named.DISPLAY_P3) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testExtendedSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.ExtendedSrgb, // Compose
            ColorSpace.get(Named.EXTENDED_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testLinearExtendedSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.LinearExtendedSrgb, // Compose
            ColorSpace.get(Named.LINEAR_EXTENDED_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testLinearSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.LinearSrgb, // Compose
            ColorSpace.get(Named.LINEAR_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testNtsc1953Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Ntsc1953, // Compose
            ColorSpace.get(Named.NTSC_1953) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testProPhotoRgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.ProPhotoRgb, // Compose
            ColorSpace.get(Named.PRO_PHOTO_RGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSmpteCColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.SmpteC, // Compose
            ColorSpace.get(Named.SMPTE_C) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testUnknownColorspace3WhitePointValues() {
        val name = "MyCustomColorSpace"
        val whitePoint = floatArrayOf(1.0f, 2.0f, 3.0f)
        val transferParameters = Rgb.TransferParameters(
            0.1, // a
            0.2, // b
            0.3, // c
            0.4, // d
            0.5, // e
            0.6, // f
            0.7 // g
        )
        val primaries = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
        colorSpaceTestHelper(
            androidx.compose.ui.graphics.colorspace.Rgb(
                name = name,
                primaries = primaries,
                WhitePoint(1.0f, 2.0f, 3.0f),
                TransferParameters(0.7, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6)
            ),
            Rgb(
                name,
                primaries,
                whitePoint,
                transferParameters
            )
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testUnknownColorspace2WhitePointValues() {
        val name = "MyCustomColorSpace"
        val whitePoint = floatArrayOf(1.0f, 2.0f)
        val transferParameters = Rgb.TransferParameters(
            0.1, // a
            0.2, // b
            0.3, // c
            0.4, // d
            0.5, // e
            0.6, // f
            0.7 // g
        )
        val primaries = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)

        colorSpaceTestHelper(
            androidx.compose.ui.graphics.colorspace.Rgb(
                name = name,
                primaries = primaries,
                WhitePoint(1.0f, 2.0f),
                TransferParameters(0.7, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6)
            ),
            Rgb(
                name,
                primaries,
                whitePoint,
                transferParameters
            )
        )
    }

    // Helper class to avoid NoSuchClassExceptions being thrown when tests are run on an older
    // API level that does not understand ColorSpace APIs
    internal class ColorSpaceHelper {
        companion object {
            @RequiresApi(Build.VERSION_CODES.O)
            fun colorSpaceTestHelper(
                composeColorSpace: androidx.compose.ui.graphics.colorspace.ColorSpace,
                frameworkColorSpace: ColorSpace
            ) {
                with(Api26Bitmap) {
                    assertEquals(composeColorSpace, frameworkColorSpace.composeColorSpace())
                }
            }
        }
    }
}