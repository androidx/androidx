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

import android.graphics.BlendMode
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.RemoteContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    fun setColorTest() {
        val remotePaint = RemotePaint()
        val color = Color.Red
        remotePaint.setColor(color.toArgb())
        assertThat(remotePaint.color).isEqualTo(color.toArgb())
    }

    @Test
    fun copyConstructorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = Color.Red.rc
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.MULTIPLY)
        remotePaint.remoteColor = remoteColor
        remotePaint.remoteColorFilter = remoteColorFilter

        val copiedPaint = RemotePaint(remotePaint)
        assertThat(copiedPaint.remoteColor).isEqualTo(remoteColor)
        assertThat(copiedPaint.remoteColorFilter).isEqualTo(remoteColorFilter)
    }

    @Test
    fun copyConstructorWithNullsTest() {
        val remotePaint = RemotePaint()
        remotePaint.color = AndroidColor.RED
        remotePaint.remoteColor = null
        remotePaint.remoteColorFilter = null

        val copiedPaint = RemotePaint(remotePaint)
        assertThat(copiedPaint.color).isEqualTo(AndroidColor.RED)
        assertThat(copiedPaint.remoteColor).isNull()
        assertThat(copiedPaint.remoteColorFilter).isNull()
    }

    @Test
    fun remoteColorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = Color.Red.rc
        remotePaint.remoteColor = remoteColor
        assertThat(remotePaint.color).isEqualTo(AndroidColor.RED)

        remotePaint.setColor(AndroidColor.BLUE)
        assertThat(remotePaint.remoteColor).isNull()
    }

    @Test
    fun remoteColorNonConstantTest() {
        val remotePaint = RemotePaint()
        val remoteColor =
            RemoteColor.fromARGB(
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                RemoteFloat(1f),
                RemoteFloat(1f),
                RemoteFloat(1f),
            )
        remotePaint.remoteColor = remoteColor
        assertThat(remotePaint.color).isEqualTo(AndroidColor.TRANSPARENT)
    }

    @Test
    fun remoteColorNullTest() {
        val remotePaint = RemotePaint()
        remotePaint.setColor(AndroidColor.BLUE)
        remotePaint.remoteColor = null
        assertThat(remotePaint.color).isEqualTo(AndroidColor.BLUE)
    }

    @Test
    fun remoteColorFilterTest() {
        val remotePaint = RemotePaint()
        val remoteColor = Color.Red.rc
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.MULTIPLY)
        remotePaint.remoteColorFilter = remoteColorFilter
        assertThat(remotePaint.colorFilter).isNotNull()

        remotePaint.colorFilter = remotePaint.colorFilter
        assertThat(remotePaint.remoteColorFilter).isNull()
    }

    @Test
    fun remoteColorFilterNonConstantTest() {
        val remotePaint = RemotePaint()
        val remoteColor =
            RemoteColor.fromARGB(
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                RemoteFloat(1f),
                RemoteFloat(1f),
                RemoteFloat(1f),
            )
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.MULTIPLY)
        remotePaint.remoteColorFilter = remoteColorFilter
        assertThat(remotePaint.colorFilter).isNull()
    }
}
