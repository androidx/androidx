/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemotePaintTest {
    @Test
    fun blendModeTest() {
        val remotePaint = RemotePaint()
        assertThat(remotePaint.blendMode).isEqualTo(RemoteBlendMode.SrcOver)
        remotePaint.blendMode = RemoteBlendMode.Clear
        assertThat(remotePaint.blendMode).isEqualTo(RemoteBlendMode.Clear)
        remotePaint.blendMode = RemoteBlendMode.SrcIn
        assertThat(remotePaint.blendMode).isEqualTo(RemoteBlendMode.SrcIn)
    }

    @Test
    fun styleTest() {
        val remotePaint = RemotePaint()
        assertThat(remotePaint.style).isEqualTo(RemotePaintingStyle.Fill)
        remotePaint.style = RemotePaintingStyle.Stroke
        assertThat(remotePaint.style).isEqualTo(RemotePaintingStyle.Stroke)
        remotePaint.style = RemotePaintingStyle.Fill
        assertThat(remotePaint.style).isEqualTo(RemotePaintingStyle.Fill)
    }

    @Test
    fun isAntiAliasTest() {
        val remotePaint = RemotePaint()
        remotePaint.isAntiAlias = true
        assertThat(remotePaint.isAntiAlias).isTrue()
        remotePaint.isAntiAlias = false
        assertThat(remotePaint.isAntiAlias).isFalse()
    }

    @Test
    fun setColorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor.Red
        remotePaint.color = remoteColor
        assertThat(remotePaint.color).isEqualTo(remoteColor)
    }

    @Test
    fun copyConstructorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(0xFFFF0000.toInt())
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, RemoteBlendMode.Multiply)
        remotePaint.color = remoteColor
        remotePaint.colorFilter = remoteColorFilter

        val copiedPaint = StandardRemotePaint(remotePaint)
        assertThat(copiedPaint.color).isEqualTo(remoteColor)
        assertThat(copiedPaint.colorFilter).isEqualTo(remoteColorFilter)
    }

    @Test
    fun copyConstructorWithNullsTest() {
        val remotePaint = RemotePaint()
        remotePaint.color = RemoteColor.Red
        remotePaint.colorFilter = null

        val copiedPaint = StandardRemotePaint(remotePaint)
        assertThat(copiedPaint.color.constantValue).isEqualTo(RemoteColor.Red.constantValue)
        assertThat(copiedPaint.colorFilter).isEqualTo(null)
    }

    @Test
    fun remoteColorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(0xFFFF0000.toInt())
        remotePaint.color = remoteColor
        assertThat(remotePaint.color).isEqualTo(remoteColor)
    }

    @Test
    fun remoteColorNonConstantTest() {
        val remotePaint = RemotePaint()
        val remoteColor =
            RemoteColor.rgb(
                red = RemoteFloat(1f),
                green = RemoteFloat(1f),
                blue = RemoteFloat(1f),
                alpha = RemoteTimeVariables.continuousSeconds,
            )
        remotePaint.color = remoteColor
        assertThat(remotePaint.color).isEqualTo(remoteColor)
    }

    @Test
    fun remoteColorBlackByDefaultTest() {
        val remotePaint = RemotePaint()
        assertThat(remotePaint.color.constantValue).isEqualTo(RemoteColor.Black.constantValue)
    }

    @Test
    fun remoteColorBlackByDefaultMatchesColorBlackRcTest() {
        val color = RemotePaint().color

        assertThat(color.isStructurallyEqual(RemoteColor.Black)).isTrue()
        assertThat(color).isSameInstanceAs(RemoteColor.Black)
    }

    @Test
    fun remoteColorFilterTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(0xFFFF0000.toInt())
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, RemoteBlendMode.Multiply)
        remotePaint.colorFilter = remoteColorFilter
        assertThat(remotePaint.colorFilter).isEqualTo(remoteColorFilter)

        remotePaint.colorFilter = null
        assertThat(remotePaint.colorFilter).isNull()
    }

    @Test
    fun remoteColorFilterNonConstantTest() {
        val remotePaint = RemotePaint()
        val remoteColor =
            RemoteColor.rgb(
                red = RemoteFloat(1f),
                green = RemoteFloat(1f),
                blue = RemoteFloat(1f),
                alpha = RemoteTimeVariables.continuousSeconds,
            )
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, RemoteBlendMode.Multiply)
        remotePaint.colorFilter = remoteColorFilter
        assertThat(remotePaint.colorFilter).isEqualTo(remoteColorFilter)
    }

    @Test
    fun androidPaintToDefaultRemotePaintTest() {
        val frameworkPaint =
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLUE
                strokeWidth = 10f
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
            }
        val remotePaint = frameworkPaint.asRemotePaint()

        val defaultPaint = StandardRemotePaint(remotePaint)

        assertThat(defaultPaint.color.constantValue).isEqualTo(RemoteColor.Blue.constantValue)
        assertThat(defaultPaint.strokeWidth.constantValue).isEqualTo(10f)
        assertThat(defaultPaint.isAntiAlias).isTrue()
        assertThat(defaultPaint.style).isEqualTo(RemotePaintingStyle.Stroke)
    }

    @Test
    fun compatAndroidRemotePaintColorTest() {
        val paint = CompatAndroidRemotePaint()
        val color = RemoteColor.Red
        paint.remoteColor = color
        assertThat(paint.color).isEqualTo(android.graphics.Color.RED)
        assertThat(paint.remoteColor?.constantValue).isEqualTo(color.constantValue)

        paint.setColor(android.graphics.Color.BLUE)
        assertThat(paint.remoteColor).isNull()
        assertThat(paint.color).isEqualTo(android.graphics.Color.BLUE)
    }

    @Test
    fun compatAndroidRemotePaintNonConstantColorTest() {
        val paint = CompatAndroidRemotePaint()
        val remoteColor =
            RemoteColor.rgb(
                red = RemoteFloat(1f),
                green = RemoteFloat(1f),
                blue = RemoteFloat(1f),
                alpha = RemoteTimeVariables.continuousSeconds,
            )
        paint.remoteColor = remoteColor
        assertThat(paint.color).isEqualTo(android.graphics.Color.TRANSPARENT)
        assertThat(paint.remoteColor).isEqualTo(remoteColor)
    }

    @Test
    fun compatAndroidRemotePaintColorFilterTest() {
        val paint = CompatAndroidRemotePaint()
        val filter = RemoteBlendModeColorFilter(RemoteColor.Red, RemoteBlendMode.SrcIn)
        paint.remoteColorFilter = filter
        assertThat(paint.remoteColorFilter).isEqualTo(filter)
        assertThat(paint.colorFilter).isNotNull()

        paint.setColorFilter(null)
        assertThat(paint.remoteColorFilter).isNull()
        assertThat(paint.colorFilter).isNull()
    }

    @Test
    fun compatAndroidRemotePaintBasicPropertiesTest() {
        val compatPaint =
            CompatAndroidRemotePaint().apply {
                isAntiAlias = false
                style = android.graphics.Paint.Style.STROKE
                blendMode = android.graphics.BlendMode.CLEAR
                isFilterBitmap = false
            }
        val remotePaint = compatPaint.asRemotePaint()

        assertThat(remotePaint.isAntiAlias).isFalse()
        assertThat(remotePaint.style).isEqualTo(RemotePaintingStyle.Stroke)
        assertThat(remotePaint.blendMode).isEqualTo(RemoteBlendMode.Clear)
        assertThat(remotePaint.filterQuality).isEqualTo(RemoteFilterQuality.None)
    }

    @Test
    fun compatAndroidRemotePaintBlendModeTest() {
        val compatPaint = CompatAndroidRemotePaint()

        assertThat(compatPaint.asRemotePaint().blendMode).isEqualTo(RemoteBlendMode.SrcOver)

        compatPaint.blendMode = android.graphics.BlendMode.CLEAR
        assertThat(compatPaint.asRemotePaint().blendMode).isEqualTo(RemoteBlendMode.Clear)

        compatPaint.blendMode = android.graphics.BlendMode.SRC_IN
        assertThat(compatPaint.asRemotePaint().blendMode).isEqualTo(RemoteBlendMode.SrcIn)

        compatPaint.blendMode = android.graphics.BlendMode.SRC_OUT
        assertThat(compatPaint.asRemotePaint().blendMode).isEqualTo(RemoteBlendMode.SrcOut)
    }

    @Test
    fun compatAndroidRemotePaintStrokePropertiesTest() {
        val compatPaint =
            CompatAndroidRemotePaint().apply {
                strokeWidth = 15f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.BEVEL
            }
        val remotePaint = compatPaint.asRemotePaint()

        assertThat(remotePaint.strokeWidth.constantValue).isEqualTo(15f)
        assertThat(remotePaint.strokeCap).isEqualTo(RemoteStrokeCap.Round)
        assertThat(remotePaint.strokeJoin).isEqualTo(RemoteStrokeJoin.Bevel)
    }

    @Test
    fun compatAndroidRemotePaintColorPropertiesTest() {
        val compatPaint =
            CompatAndroidRemotePaint().apply {
                remoteColor = RemoteColor.Green
                remoteColorFilter =
                    RemoteBlendModeColorFilter(RemoteColor.Red, RemoteBlendMode.SrcIn)
            }
        val remotePaint = compatPaint.asRemotePaint()

        assertThat(remotePaint.color.constantValue).isEqualTo(RemoteColor.Green.constantValue)
        val colorFilter = remotePaint.colorFilter as RemoteBlendModeColorFilter
        assertThat(colorFilter.color.constantValue).isEqualTo(RemoteColor.Red.constantValue)
        assertThat(colorFilter.blendMode).isEqualTo(RemoteBlendMode.SrcIn)
    }

    @Test
    fun compatAndroidRemotePaintEffectPropertiesTest() {
        val shader =
            RemoteLinearShader(
                x0 = 0f.rf,
                y0 = 0f.rf,
                x1 = 10f.rf,
                y1 = 10f.rf,
                colors = listOf(RemoteColor.Red, RemoteColor.Blue),
                positions = null,
                tileMode = RemoteTileMode.Clamp,
            )
        val compatPaint = CompatAndroidRemotePaint().apply { this.remoteShader = shader }
        val remotePaint = compatPaint.asRemotePaint()

        assertThat(remotePaint.shader).isEqualTo(shader)
    }

    @Test
    fun compatAndroidRemotePaintTextPropertiesTest() {
        val compatPaint =
            CompatAndroidRemotePaint().apply {
                textSize = 22f
                typeface = android.graphics.Typeface.SERIF
            }
        val remotePaint = compatPaint.asRemotePaint()

        assertThat(remotePaint.textSize.constantValue).isEqualTo(22f)
        assertThat(remotePaint.typeface).isEqualTo(RemoteTypeface.Serif)
    }

    @Test
    fun remotePaintTypefaceTest() {
        val remotePaint =
            StandardRemotePaint().apply {
                typeface = RemoteTypeface.Serif
            }
        assertThat(remotePaint.typeface).isEqualTo(RemoteTypeface.Serif)
    }
}
