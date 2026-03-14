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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemotePaintTest {
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
        val color = Color.Red
        val rc = color.rc
        remotePaint.color = rc
        assertThat(remotePaint.color).isEqualTo(rc)
    }

    @Test
    fun copyConstructorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(0xFFFF0000.toInt())
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.Multiply)
        remotePaint.color = remoteColor
        remotePaint.colorFilter = remoteColorFilter

        val copiedPaint = StandardRemotePaint(remotePaint)
        assertThat(copiedPaint.color).isEqualTo(remoteColor)
        assertThat(copiedPaint.colorFilter).isEqualTo(remoteColorFilter)
    }

    @Test
    fun copyConstructorWithNullsTest() {
        val remotePaint = RemotePaint()
        remotePaint.color = Color.Red.rc
        remotePaint.colorFilter = null

        val copiedPaint = StandardRemotePaint(remotePaint)
        assertThat(copiedPaint.color.constantValue).isEqualTo(Color.Red)
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
                alpha = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
            )
        remotePaint.color = remoteColor
        assertThat(remotePaint.color).isEqualTo(remoteColor)
    }

    @Test
    fun remoteColorBlackByDefaultTest() {
        val remotePaint = RemotePaint()
        assertThat(remotePaint.color.constantValue).isEqualTo(Color.Black)
    }

    @Test
    fun remoteColorFilterTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(0xFFFF0000.toInt())
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.Multiply)
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
                alpha = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
            )
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.Multiply)
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

        assertThat(defaultPaint.color.constantValue).isEqualTo(Color.Blue)
        assertThat(defaultPaint.strokeWidth.constantValue).isEqualTo(10f)
        assertThat(defaultPaint.isAntiAlias).isTrue()
        assertThat(defaultPaint.style).isEqualTo(PaintingStyle.Stroke)
    }

    @Test
    fun composePaintToDefaultRemotePaintTest() {
        val composePaint =
            androidx.compose.ui.graphics.Paint().apply {
                color = Color.Green
                strokeWidth = 5f
                isAntiAlias = false
                style = PaintingStyle.Fill
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Bevel
            }
        val remotePaint = composePaint.asRemotePaint()

        val defaultPaint = StandardRemotePaint(remotePaint)

        assertThat(defaultPaint.color.constantValue).isEqualTo(Color.Green)
        assertThat(defaultPaint.strokeWidth.constantValue).isEqualTo(5f)
        assertThat(defaultPaint.isAntiAlias).isFalse()
        assertThat(defaultPaint.style).isEqualTo(PaintingStyle.Fill)
        assertThat(defaultPaint.strokeCap).isEqualTo(StrokeCap.Round)
        assertThat(defaultPaint.strokeJoin).isEqualTo(StrokeJoin.Bevel)
    }
}
