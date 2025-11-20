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

package androidx.wear.compose.material3.test

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class SwipeToDismissBoxTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun uses_theme_colors_by_default() {
        val testBackgroundColor: Color = Color.Yellow
        var capturedContentScrimColor: Color = Color.Unspecified
        var capturedBackgroundScrimColor: Color = Color.Unspecified

        rule.setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(background = testBackgroundColor)
            ) {
                SwipeToDismissBox(state = rememberSwipeToDismissBoxState()) {
                    capturedBackgroundScrimColor = LocalSwipeToDismissBackgroundScrimColor.current
                    capturedContentScrimColor = LocalSwipeToDismissContentScrimColor.current
                }
            }
        }

        assertThat(capturedBackgroundScrimColor).isEqualTo(testBackgroundColor)
        assertThat(capturedContentScrimColor).isEqualTo(testBackgroundColor)
    }

    @Test
    fun allows_overriding_theme_colors() {
        val customBackgroundColor = Color.Red
        val customContentColor = Color.Green
        var capturedBackgroundColor: Color = Color.Unspecified
        var capturedContentColor: Color = Color.Unspecified

        rule.setContent {
            SwipeToDismissBox(
                state = rememberSwipeToDismissBoxState(),
                backgroundScrimColor = customBackgroundColor,
                contentScrimColor = customContentColor,
            ) {
                capturedBackgroundColor = LocalSwipeToDismissBackgroundScrimColor.current
                capturedContentColor = LocalSwipeToDismissContentScrimColor.current
            }
        }

        assertThat(capturedBackgroundColor).isEqualTo(customBackgroundColor)
        assertThat(capturedContentColor).isEqualTo(customContentColor)
    }
}
