/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.AndroidColorSpaceTest.ColorSpaceHelper.Companion.colorSpaceTestHelper
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.TransferParameters
import androidx.compose.ui.graphics.colorspace.WhitePoint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidColorSpaceTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Srgb, // Compose
            ColorSpace.get(ColorSpace.Named.SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAcesColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Aces, // Compose
            ColorSpace.get(ColorSpace.Named.ACES) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAcescgColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Acescg, // Compose
            ColorSpace.get(ColorSpace.Named.ACESCG) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAdobeRgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.AdobeRgb, // Compose
            ColorSpace.get(ColorSpace.Named.ADOBE_RGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBt2020Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Bt2020, // Compose
            ColorSpace.get(ColorSpace.Named.BT2020) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBt709Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Bt709, // Compose
            ColorSpace.get(ColorSpace.Named.BT709) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCieLabColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.CieLab, // Compose
            ColorSpace.get(ColorSpace.Named.CIE_LAB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCieXyzColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.CieXyz, // Compose
            ColorSpace.get(ColorSpace.Named.CIE_XYZ) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDciP3Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.DciP3, // Compose
            ColorSpace.get(ColorSpace.Named.DCI_P3) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDisplayP3Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.DisplayP3, // Compose
            ColorSpace.get(ColorSpace.Named.DISPLAY_P3) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testExtendedSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.ExtendedSrgb, // Compose
            ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testLinearExtendedSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.LinearExtendedSrgb, // Compose
            ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testLinearSrgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.LinearSrgb, // Compose
            ColorSpace.get(ColorSpace.Named.LINEAR_SRGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testNtsc1953Colorspace() {
        colorSpaceTestHelper(
            ColorSpaces.Ntsc1953, // Compose
            ColorSpace.get(ColorSpace.Named.NTSC_1953) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testProPhotoRgbColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.ProPhotoRgb, // Compose
            ColorSpace.get(ColorSpace.Named.PRO_PHOTO_RGB) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSmpteCColorspace() {
        colorSpaceTestHelper(
            ColorSpaces.SmpteC, // Compose
            ColorSpace.get(ColorSpace.Named.SMPTE_C) // Framework
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testUnknownColorspace3WhitePointValues() {
        val name = "MyCustomColorSpace"
        val whitePoint = floatArrayOf(1.0f, 2.0f, 3.0f)
        val transferParameters = ColorSpace.Rgb.TransferParameters(
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
            ColorSpace.Rgb(
                name,
                primaries,
                whitePoint,
                transferParameters
            )
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testUnknownColorSpaceNoTransform() {
        val name = "MyCustomColorSpace"
        val whitePoint = floatArrayOf(1.0f, 2.0f, 3.0f)
        val primaries = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
        colorSpaceTestHelper(
            androidx.compose.ui.graphics.colorspace.Rgb(
                name = name,
                primaries = primaries,
                WhitePoint(1.0f, 2.0f, 3.0f),
                { 1.0 },
                { 2.0 },
                2f,
                4f,
            ),
            ColorSpace.Rgb(
                name,
                primaries,
                whitePoint,
                { _ -> 1.0 },
                { _ -> 2.0 },
                2f,
                4f,
            )
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testUnknownColorspace2WhitePointValues() {
        val name = "MyCustomColorSpace"
        val whitePoint = floatArrayOf(1.0f, 2.0f)
        val transferParameters = ColorSpace.Rgb.TransferParameters(
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
            Rgb(
                name = name,
                primaries = primaries,
                WhitePoint(1.0f, 2.0f),
                TransferParameters(0.7, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6)
            ),
            ColorSpace.Rgb(
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
                val convertedColorSpace = frameworkColorSpace.toComposeColorSpace()
                Assert.assertEquals(composeColorSpace, convertedColorSpace)

                val frameworkConvertedColorSpace = convertedColorSpace.toAndroidColorSpace()
                Assert.assertEquals(frameworkColorSpace, frameworkConvertedColorSpace)
            }
        }
    }
}