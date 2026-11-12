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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteProgressIndicatorColorsTest {

    private val redBrush = RemoteBrush.solidColor(RemoteColor.Red)
    private val blueBrush = RemoteBrush.solidColor(RemoteColor.Blue)
    private val greenBrush = RemoteBrush.solidColor(RemoteColor.Green)
    private val grayBrush = RemoteBrush.solidColor(RemoteColor.Gray)
    private val darkGrayBrush = RemoteBrush.solidColor(RemoteColor.DarkGray)
    private val lightGrayBrush = RemoteBrush.solidColor(RemoteColor.LightGray)

    private val colors =
        RemoteProgressIndicatorColors(
            indicatorBrush = redBrush,
            trackBrush = blueBrush,
            overflowTrackBrush = greenBrush,
            disabledIndicatorBrush = grayBrush,
            disabledTrackBrush = darkGrayBrush,
            disabledOverflowTrackBrush = lightGrayBrush,
        )

    @Test
    fun resolves_enabled_when_constant_true() {
        assertThat(colors.indicatorBrush(true.rb)).isEqualTo(redBrush)
        assertThat(colors.trackBrush(true.rb)).isEqualTo(blueBrush)
        assertThat(colors.overflowTrackBrush(true.rb)).isEqualTo(greenBrush)
    }

    @Test
    fun resolves_disabled_when_constant_false() {
        assertThat(colors.indicatorBrush(false.rb)).isEqualTo(grayBrush)
        assertThat(colors.trackBrush(false.rb)).isEqualTo(darkGrayBrush)
        assertThat(colors.overflowTrackBrush(false.rb)).isEqualTo(lightGrayBrush)
    }

    @Test
    fun resolves_enabled_brush_when_expression_provided() {
        val expressionBoolean = 1f.rf.isGreaterThan(0f.rf)
        assertThat(colors.indicatorBrush(expressionBoolean)).isEqualTo(redBrush)
        assertThat(colors.trackBrush(expressionBoolean)).isEqualTo(blueBrush)
        assertThat(colors.overflowTrackBrush(expressionBoolean)).isEqualTo(greenBrush)
    }

    @Test
    fun copy_preserves_values_when_no_arguments_passed() {
        val copied = colors.copy()
        assertThat(copied).isEqualTo(colors)
        assertThat(copied.hashCode()).isEqualTo(colors.hashCode())
    }

    @Test
    fun copy_overrides_specified_brushes() {
        val yellowBrush = RemoteBrush.solidColor(RemoteColor.Yellow)
        val cyanBrush = RemoteBrush.solidColor(RemoteColor.Cyan)
        val copied = colors.copy(indicatorBrush = yellowBrush, disabledTrackBrush = cyanBrush)
        assertThat(copied.indicatorBrush).isEqualTo(yellowBrush)
        assertThat(copied.trackBrush).isEqualTo(blueBrush)
        assertThat(copied.overflowTrackBrush).isEqualTo(greenBrush)
        assertThat(copied.disabledIndicatorBrush).isEqualTo(grayBrush)
        assertThat(copied.disabledTrackBrush).isEqualTo(cyanBrush)
        assertThat(copied.disabledOverflowTrackBrush).isEqualTo(lightGrayBrush)
    }
}
