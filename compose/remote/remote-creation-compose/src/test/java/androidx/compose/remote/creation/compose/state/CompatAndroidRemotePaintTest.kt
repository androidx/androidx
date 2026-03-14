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
import android.graphics.BlendModeColorFilter
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.sweepGradient
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
class CompatAndroidRemotePaintTest {

    @Test
    fun copyConstructorTest() {
        val original = CompatAndroidRemotePaint()
        val remoteColor = Color.Yellow.rc
        original.remoteColor = remoteColor
        val remoteColorFilter =
            RemoteBlendModeColorFilter(
                Color.Green.rc,
                androidx.compose.ui.graphics.BlendMode.SrcOver,
            )
        original.remoteColorFilter = remoteColorFilter
        val sweepGradient = RemoteBrush.sweepGradient(listOf(Color.Red.rc, Color.Blue.rc))
        val remoteShader =
            with(NoRemoteCompose()) { with(sweepGradient) { createShader(RemoteSize.Zero) } }
        original.remoteShader = remoteShader

        val copy = CompatAndroidRemotePaint(original)
        assertThat(copy.remoteColor?.constantValue).isEqualTo(remoteColor.constantValue)
        assertThat(copy.remoteColorFilter).isEqualTo(remoteColorFilter)
        assertThat(copy.remoteShader).isEqualTo(remoteShader)
    }

    @Test
    fun remoteColorTest() {
        val paint = CompatAndroidRemotePaint()
        val remoteColor = Color.Red.rc
        paint.remoteColor = remoteColor

        assertThat(paint.remoteColor).isEqualTo(remoteColor)
        assertThat(paint.color).isEqualTo(android.graphics.Color.RED)
    }

    @Test
    fun setColorClearsRemoteColorTest() {
        val paint = CompatAndroidRemotePaint()
        paint.remoteColor = Color.Red.rc
        paint.color = android.graphics.Color.BLUE

        assertThat(paint.remoteColor).isNull()
        assertThat(paint.color).isEqualTo(android.graphics.Color.BLUE)
    }

    @Test
    fun remoteColorFilterTest() {
        val paint = CompatAndroidRemotePaint()
        val remoteColor = Color.Green.rc
        val remoteColorFilter =
            RemoteBlendModeColorFilter(remoteColor, androidx.compose.ui.graphics.BlendMode.SrcOver)
        paint.remoteColorFilter = remoteColorFilter

        assertThat(paint.remoteColorFilter).isEqualTo(remoteColorFilter)
        val filter = paint.colorFilter as BlendModeColorFilter
        assertThat(filter.color).isEqualTo(Color.Green.toArgb())
        assertThat(filter.mode).isEqualTo(BlendMode.SRC_OVER)
    }

    @Test
    fun setColorFilterClearsRemoteColorFilterTest() {
        val paint = CompatAndroidRemotePaint()
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(Color.Red.rc, androidx.compose.ui.graphics.BlendMode.SrcOver)
        paint.colorFilter =
            android.graphics.PorterDuffColorFilter(
                android.graphics.Color.BLUE,
                android.graphics.PorterDuff.Mode.SRC_IN,
            )

        assertThat(paint.remoteColorFilter).isNull()
    }

    @Test
    fun toRemotePaintTest() {
        val paint = CompatAndroidRemotePaint()
        val remoteColor = Color.Cyan.rc
        paint.remoteColor = remoteColor

        val remotePaint = paint.remotePaint
        assertThat(remotePaint.color.constantValue).isEqualTo(remoteColor.constantValue)
    }

    @Test
    fun asRemotePaintTest() {
        val paint = CompatAndroidRemotePaint()
        val remoteColor = Color.Magenta.rc
        paint.remoteColor = remoteColor

        val remotePaint = paint.asRemotePaint()
        assertThat(remotePaint.color.constantValue).isEqualTo(remoteColor.constantValue)
    }

    @Test
    fun remoteShaderTest() {
        val paint = CompatAndroidRemotePaint()
        val sweepGradient = RemoteBrush.sweepGradient(listOf(Color.Red.rc, Color.Blue.rc))
        val remoteShader =
            with(NoRemoteCompose()) { with(sweepGradient) { createShader(RemoteSize.Zero) } }
        paint.remoteShader = remoteShader

        assertThat(paint.remoteShader).isEqualTo(remoteShader)
        assertThat(paint.shader).isEqualTo(remoteShader)
    }
}
