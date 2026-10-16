/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.creation.compose.painter.RemoteColorPainter
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteContentScaleTest {

    @Test
    fun constants_matchImageScalingValues() {
        assertThat(RemoteContentScale.Crop.value).isEqualTo(ImageScaling.SCALE_CROP)
        assertThat(RemoteContentScale.Fit.value).isEqualTo(ImageScaling.SCALE_FIT)
        assertThat(RemoteContentScale.FillHeight.value).isEqualTo(ImageScaling.SCALE_FILL_HEIGHT)
        assertThat(RemoteContentScale.FillWidth.value).isEqualTo(ImageScaling.SCALE_FILL_WIDTH)
        assertThat(RemoteContentScale.Inside.value).isEqualTo(ImageScaling.SCALE_INSIDE)
        assertThat(RemoteContentScale.None.value).isEqualTo(ImageScaling.SCALE_NONE)
        assertThat(RemoteContentScale.FillBounds.value).isEqualTo(ImageScaling.SCALE_FILL_BOUNDS)
    }

    @Test
    fun contentScale_convertsToRemoteContentScale() {
        assertThat(ContentScale.Crop.toRemoteContentScale()).isEqualTo(RemoteContentScale.Crop)
        assertThat(ContentScale.Fit.toRemoteContentScale()).isEqualTo(RemoteContentScale.Fit)
        assertThat(ContentScale.FillHeight.toRemoteContentScale())
            .isEqualTo(RemoteContentScale.FillHeight)
        assertThat(ContentScale.FillWidth.toRemoteContentScale())
            .isEqualTo(RemoteContentScale.FillWidth)
        assertThat(ContentScale.Inside.toRemoteContentScale()).isEqualTo(RemoteContentScale.Inside)
        assertThat(ContentScale.None.toRemoteContentScale()).isEqualTo(RemoteContentScale.None)
        assertThat(ContentScale.FillBounds.toRemoteContentScale())
            .isEqualTo(RemoteContentScale.FillBounds)

        val customContentScale =
            object : ContentScale {
                override fun computeScaleFactor(srcSize: Size, dstSize: Size) = ScaleFactor(1f, 1f)
            }
        assertThat(customContentScale.toRemoteContentScale()).isEqualTo(RemoteContentScale.None)
    }

    @Test
    fun remoteColorPainter_instantiationAndBridges() {
        val painter = painterRemoteColor(RemoteColor(Color.Red))
        assertThat(painter).isInstanceOf(RemoteColorPainter::class.java)
        assertThat(painter.intrinsicSize).isNull()

        val bridgePainter = painterRemoteColor(Color.Blue)
        assertThat(bridgePainter).isInstanceOf(RemoteColorPainter::class.java)
    }
}
