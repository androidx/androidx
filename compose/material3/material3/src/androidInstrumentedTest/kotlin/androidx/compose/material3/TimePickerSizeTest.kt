/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalMaterial3Api::class)
class TimePickerSizeTest(val config: Config) {

    @get:Rule val rule = createComposeRule()

    @Test
    fun clockFace_size_resizesCorrectly() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)
            )
        rule
            .setMaterialContentForSizeAssertions(
                parentMaxWidth = config.size.width,
                parentMaxHeight = config.size.height,
            ) {
                ClockFace(
                    modifier = Modifier.then(ClockFaceSizeModifier()),
                    state = state,
                    colors = TimePickerDefaults.colors(),
                    autoSwitchToMinute = true,
                )
            }
            .assertIsSquareWithSize(config.expected)
    }

    @Test
    fun clockFace_24Hour_everyValue() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)
            )

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.size(config.size)) {
                ClockFace(
                    modifier = Modifier,
                    state = state,
                    colors = TimePickerDefaults.colors(),
                    autoSwitchToMinute = true,
                )
            }
        }

        repeat(24) { number ->
            rule
                .onNodeWithTimeValue(number, TimePickerSelectionMode.Hour, is24Hour = true)
                .performClick()

            rule.runOnIdle {
                state.selection = TimePickerSelectionMode.Hour
                assertThat(state.hour).isEqualTo(number)
            }

            rule
                .onNodeWithTimeValue(number, TimePickerSelectionMode.Hour, is24Hour = true)
                .assertIsSelected()
        }
    }

    internal companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                Config(DpSize(384.dp, 384.dp), 256.dp),
                Config(DpSize(350.dp, 350.dp), 238.dp),
                Config(DpSize(300.dp, 300.dp), 200.dp),
            )
    }

    data class Config(val size: DpSize, val expected: Dp)
}
