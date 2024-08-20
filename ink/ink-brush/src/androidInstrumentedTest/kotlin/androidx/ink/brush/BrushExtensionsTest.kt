/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import android.graphics.Color as AndroidColor
import android.graphics.ColorSpace as AndroidColorSpace
import android.os.Build
import androidx.annotation.ColorLong
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class BrushExtensionsTest {
    private val displayP3 = AndroidColorSpace.get(AndroidColorSpace.Named.DISPLAY_P3)
    private val adobeRgb = AndroidColorSpace.get(AndroidColorSpace.Named.ADOBE_RGB)

    private val testColor = AndroidColor.valueOf(0.4f, 0.6f, 0.8f, 0.2f, displayP3)
    @ColorLong private val testColorLong = testColor.pack()

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private val testFamily = BrushFamily(uri = "/brush-family:pencil")

    @Test
    fun brushGetAndroidColor_getsCorrectColor() {
        val brush = Brush.withColorLong(testFamily, testColorLong, 1f, 1f)

        // Note that expectedColor is not necessarily the same as testColor, because of precision
        // loss
        // when converting from testColor to testColorLong (which is necessary, because Brush stores
        // the
        // color internally as a ColorLong anyway).
        val expectedColor = AndroidColor.valueOf(testColorLong)
        assertThat(brush.getAndroidColor()).isEqualTo(expectedColor)
    }

    @Test
    fun brushCopyWithAndroidColor_setsColor() {
        val brush = Brush.withColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newBrush = brush.copyWithAndroidColor(color = testColor)

        assertThat(newBrush.family).isEqualTo(brush.family)
        assertThat(newBrush.colorLong).isEqualTo(testColorLong)
        assertThat(newBrush.size).isEqualTo(brush.size)
        assertThat(newBrush.epsilon).isEqualTo(brush.epsilon)
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    @Test
    fun brushCopyWithAndroidColor_andOtherChangedValues_createsBrushWithColor() {
        val brush = Brush.withColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newBrush =
            brush.copyWithAndroidColor(
                color = testColor,
                family = BrushFamily(),
                size = 2f,
                epsilon = 0.2f,
            )

        assertThat(newBrush.family).isEqualTo(BrushFamily())
        assertThat(newBrush.colorLong).isEqualTo(testColorLong)
        assertThat(newBrush.size).isEqualTo(2f)
        assertThat(newBrush.epsilon).isEqualTo(0.2f)
    }

    @Test
    fun brushCopyWithAndroidColor_withUnsupportedColorSpace_setsConvertedColor() {
        val brush = Brush.withColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val newBrush = brush.copyWithAndroidColor(color = newColor)

        // newColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(newColor.pack()).convert(displayP3)
        assertThat(newBrush.colorLong).isEqualTo(expectedColor.pack())
    }

    @Test
    fun brushBuilderAndroidColor_setsColor() {
        val brush =
            Brush.builder()
                .setFamily(testFamily)
                .setAndroidColor(testColor)
                .setSize(1f)
                .setEpsilon(1f)
                .build()

        assertThat(brush.colorLong).isEqualTo(testColorLong)
    }

    @Test
    fun brushBuilderAndroidColor_withUnsupportedColorSpace_setsConvertedColor() {
        val unsupportedColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush =
            Brush.builder()
                .setFamily(testFamily)
                .setAndroidColor(unsupportedColor)
                .setSize(1f)
                .setEpsilon(1f)
                .build()

        // unsupportedColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(unsupportedColor.pack()).convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.pack())
    }

    @Test
    fun brushWithAndroidColor_createsBrushWithColor() {
        val brush = Brush.withAndroidColor(testFamily, testColor, 1f, 1f)
        assertThat(brush.colorLong).isEqualTo(testColorLong)
    }

    @Test
    fun brushWithAndroidColor_withUnsupportedColorSpace_createsBrushWithConvertedColor() {
        val unsupportedColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush = Brush.withAndroidColor(testFamily, unsupportedColor, 1f, 1f)

        // unsupportedColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(unsupportedColor.pack()).convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.pack())
    }

    @Test
    fun brushUtilGetAndroidColor_getsCorrectColor() {
        val brush = Brush.withColorLong(testFamily, testColorLong, 1f, 1f)

        // Note that expectedColor is not necessarily the same as testColor, because of precision
        // loss
        // when converting from testColor to testColorLong.
        val expectedColor = AndroidColor.valueOf(testColorLong)
        assertThat(BrushUtil.getAndroidColor(brush)).isEqualTo(expectedColor)
    }

    @Test
    fun brushUtilToBuilderWithAndroidColor_setsColor() {
        val brush = Brush.withColorIntArgb(testFamily, 0x4499bb66, 2f, 0.2f)

        val newBrush = BrushUtil.toBuilderWithAndroidColor(brush, testColor).build()

        assertThat(newBrush.colorLong).isEqualTo(testColorLong)
        assertThat(brush.family).isEqualTo(testFamily)
        assertThat(brush.size).isEqualTo(2f)
        assertThat(brush.epsilon).isEqualTo(0.2f)
    }

    @Test
    fun brushUtilToBuilderWithAndroidColor_withUnsupportedColorSpace_setsConvertedColor() {
        val brush = Brush.withColorIntArgb(testFamily, 0x4499bb66, 2f, 0.2f)

        val unsupportedColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val newBrush = BrushUtil.toBuilderWithAndroidColor(brush, unsupportedColor).build()

        // unsupportedColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(unsupportedColor.pack()).convert(displayP3)
        assertThat(newBrush.colorLong).isEqualTo(expectedColor.pack())

        assertThat(brush.family).isEqualTo(testFamily)
        assertThat(brush.size).isEqualTo(2f)
        assertThat(brush.epsilon).isEqualTo(0.2f)
    }

    @Test
    fun brushUtilMakeBuilderWithAndroidColor_setsColor() {
        val brush =
            BrushUtil.makeBuilderWithAndroidColor(testColor)
                .setFamily(testFamily)
                .setSize(2f)
                .setEpsilon(0.2f)
                .build()

        assertThat(brush.family).isEqualTo(testFamily)
        assertThat(brush.colorLong).isEqualTo(testColorLong)
        assertThat(brush.size).isEqualTo(2f)
        assertThat(brush.epsilon).isEqualTo(0.2f)
    }

    @Test
    fun brushUtilMakeBuilderAndroidColor_withUnsupportedColorSpace_setsConvertedColor() {
        val unsupportedColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush =
            BrushUtil.makeBuilderWithAndroidColor(unsupportedColor)
                .setFamily(testFamily)
                .setSize(2f)
                .setEpsilon(0.2f)
                .build()

        // unsupportedColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(unsupportedColor.pack()).convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.pack())
    }

    @Test
    fun brushUtilMakeBrushWithAndroidColor_createsBrushWithColor() {
        val brush = BrushUtil.makeBrushWithAndroidColor(testFamily, testColor, 1f, 1f)
        assertThat(brush.colorLong).isEqualTo(testColorLong)
    }

    @Test
    fun brushUtilMakeBrushWithAndroidColor_withUnsupportedColorSpace_createsBrushWithConvertedColor() {
        val unsupportedColor = AndroidColor.valueOf(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush = BrushUtil.makeBrushWithAndroidColor(testFamily, unsupportedColor, 1f, 1f)

        // unsupportedColor gets converted to ColorLong (losing precision) and then to Display P3.
        val expectedColor = AndroidColor.valueOf(unsupportedColor.pack()).convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.pack())
    }
}
