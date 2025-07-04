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

package androidx.xr.glimmer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TypographyTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun themeUpdatesWithNewTypography() {
        val typography = Typography()
        val customTypography =
            Typography(titleLarge = TextStyle(fontSize = 100.sp, fontWeight = FontWeight.W100))
        val typographyState = mutableStateOf(typography)
        var currentTypography: Typography? = null
        rule.setContent {
            GlimmerTheme(typography = typographyState.value) {
                currentTypography = GlimmerTheme.typography
            }
        }

        rule.runOnIdle {
            assertThat(currentTypography).isEqualTo(typography)
            typographyState.value = customTypography
        }

        rule.runOnIdle { assertThat(currentTypography).isEqualTo(customTypography) }
    }
}
