/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DynamicColorSchemeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun should_fallback_to_default_if_dynamic_colors_not_enabled() {
        val expected =
            ColorScheme(primary = Color.Red, secondary = Color.Green, tertiary = Color.Blue)
        var actualPrimary: Color = Color.Unspecified
        var actualSecondary: Color = Color.Unspecified
        var actualTertiary: Color = Color.Unspecified

        rule.setContent {
            MaterialTheme(
                colorScheme =
                    dynamicColorScheme(LocalContext.current, defaultColorScheme = expected)
            ) {
                actualPrimary = MaterialTheme.colorScheme.primary
                actualSecondary = MaterialTheme.colorScheme.secondary
                actualTertiary = MaterialTheme.colorScheme.tertiary
            }
        }

        assertEquals(expected.primary, actualPrimary)
        assertEquals(expected.secondary, actualSecondary)
        assertEquals(expected.tertiary, actualTertiary)
    }
}
