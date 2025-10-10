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
import android.graphics.Color
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
class RemotePaintTest {
    @Test
    fun setColorTest() {
        val remotePaint = RemotePaint()
        val color = Color.valueOf(Color.RED)
        remotePaint.setColor(color.toArgb())
        assertThat(remotePaint.color).isEqualTo(color.toArgb())
    }

    @Test
    fun copyConstructorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(Color.valueOf(Color.RED))
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
        remotePaint.color = Color.RED
        remotePaint.remoteColor = null
        remotePaint.remoteColorFilter = null

        val copiedPaint = RemotePaint(remotePaint)
        assertThat(copiedPaint.color).isEqualTo(Color.RED)
        assertThat(copiedPaint.remoteColor).isNull()
        assertThat(copiedPaint.remoteColorFilter).isNull()
    }

    @Test
    fun remoteColorTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(Color.valueOf(Color.RED))
        remotePaint.remoteColor = remoteColor
        assertThat(remotePaint.color).isEqualTo(Color.TRANSPARENT)

        remotePaint.setColor(Color.BLUE)
        assertThat(remotePaint.remoteColor).isNull()
    }

    @Test
    fun remoteColorNullTest() {
        val remotePaint = RemotePaint()
        remotePaint.setColor(Color.BLUE)
        remotePaint.remoteColor = null
        assertThat(remotePaint.color).isEqualTo(Color.BLUE)
    }

    @Test
    fun remoteColorFilterTest() {
        val remotePaint = RemotePaint()
        val remoteColor = RemoteColor(Color.valueOf(Color.RED))
        val remoteColorFilter = RemoteBlendModeColorFilter(remoteColor, BlendMode.MULTIPLY)
        remotePaint.remoteColorFilter = remoteColorFilter
        assertThat(remotePaint.colorFilter).isNull()

        remotePaint.colorFilter = remotePaint.colorFilter
        assertThat(remotePaint.remoteColorFilter).isNull()
    }
}
