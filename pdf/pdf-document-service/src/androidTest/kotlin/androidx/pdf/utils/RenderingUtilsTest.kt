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

package androidx.pdf.utils

import android.graphics.Matrix
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
@RunWith(AndroidJUnit4::class)
class RenderingUtilsTest {
    @Test
    fun getTransformationMatrix_positiveScaleAndTranslation() {
        val left = 100
        val top = 200
        val scaledPageWidth = 500f
        val scaledPageHeight = 800f
        val pageWidth = 1000
        val pageHeight = 1600

        val matrix =
            getTransformationMatrix(
                left,
                top,
                scaledPageWidth,
                scaledPageHeight,
                pageWidth,
                pageHeight,
            )

        val expectedMatrix =
            Matrix().apply {
                setScale(0.5f, 0.5f)
                postTranslate(-100f, -200f)
            }

        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun getTransformationMatrix_zeroScaleAndPositiveTranslation() {
        val left = 100
        val top = 200
        val scaledPageWidth = 0f
        val scaledPageHeight = 0f
        val pageWidth = 1000
        val pageHeight = 1600

        val matrix =
            getTransformationMatrix(
                left,
                top,
                scaledPageWidth,
                scaledPageHeight,
                pageWidth,
                pageHeight,
            )

        val expectedMatrix =
            Matrix().apply {
                setScale(0f, 0f)
                postTranslate(-100f, -200f)
            }
        assertEquals(expectedMatrix, matrix)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getTransformationMatrix_zeroWidth_infiniteXScale() {
        val left = 100
        val top = 200
        val scaledPageWidth = 500f
        val scaledPageHeight = 800f
        val pageWidth = 0
        val pageHeight = 1600

        getTransformationMatrix(left, top, scaledPageWidth, scaledPageHeight, pageWidth, pageHeight)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getTransformationMatrix_zeroHeight_infiniteYScale() {
        val left = 100
        val top = 200
        val scaledPageWidth = 500f
        val scaledPageHeight = 800f
        val pageWidth = 1000
        val pageHeight = 0

        getTransformationMatrix(left, top, scaledPageWidth, scaledPageHeight, pageWidth, pageHeight)
    }
}
