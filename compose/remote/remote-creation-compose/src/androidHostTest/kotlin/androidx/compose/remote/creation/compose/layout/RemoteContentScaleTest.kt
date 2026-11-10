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
import androidx.compose.ui.graphics.Color
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
    fun remoteColorPainter_instantiation() {
        val painter = painterRemoteColor(RemoteColor(Color.Red))
        assertThat(painter).isInstanceOf(RemoteColorPainter::class.java)
        assertThat(painter.intrinsicSize).isNull()
    }
}
