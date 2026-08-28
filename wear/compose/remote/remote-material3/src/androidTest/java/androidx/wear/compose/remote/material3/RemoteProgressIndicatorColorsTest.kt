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
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RemoteProgressIndicatorColorsTest {

    private val redBrush = RemoteBrush.solidColor(Color.Red.rc)
    private val blueBrush = RemoteBrush.solidColor(Color.Blue.rc)
    private val greenBrush = RemoteBrush.solidColor(Color.Green.rc)
    private val grayBrush = RemoteBrush.solidColor(Color.Gray.rc)
    private val darkGrayBrush = RemoteBrush.solidColor(Color.DarkGray.rc)
    private val lightGrayBrush = RemoteBrush.solidColor(Color.LightGray.rc)

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
        assertEquals(redBrush, colors.indicatorBrush(true.rb))
        assertEquals(blueBrush, colors.trackBrush(true.rb))
        assertEquals(greenBrush, colors.overflowTrackBrush(true.rb))
    }

    @Test
    fun resolves_disabled_when_constant_false() {
        assertEquals(grayBrush, colors.indicatorBrush(false.rb))
        assertEquals(darkGrayBrush, colors.trackBrush(false.rb))
        assertEquals(lightGrayBrush, colors.overflowTrackBrush(false.rb))
    }

    @Test
    fun resolves_enabled_brush_when_expression_provided() {
        val expressionBoolean = 1f.rf.isGreaterThan(0f.rf)
        assertEquals(redBrush, colors.indicatorBrush(expressionBoolean))
        assertEquals(blueBrush, colors.trackBrush(expressionBoolean))
        assertEquals(greenBrush, colors.overflowTrackBrush(expressionBoolean))
    }
}
