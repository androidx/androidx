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

package androidx.xr.glimmer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentSpacingValuesTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun themeUpdatesWithNewComponentSpacingValues() {
        val componentSpacingValues = ComponentSpacingValues()
        val customComponentSpacingValues =
            ComponentSpacingValues(
                extraSmall = 1.dp,
                small = 2.dp,
                medium = 3.dp,
                large = 4.dp,
                extraLarge = 5.dp,
            )
        val componentSpacingValuesState = mutableStateOf(componentSpacingValues)
        var currentComponentSpacingValues: ComponentSpacingValues? = null
        rule.setContent {
            GlimmerTheme(componentSpacingValues = componentSpacingValuesState.value) {
                currentComponentSpacingValues = GlimmerTheme.componentSpacingValues
            }
        }

        rule.runOnIdle {
            assertThat(currentComponentSpacingValues).isEqualTo(componentSpacingValues)
            componentSpacingValuesState.value = customComponentSpacingValues
        }

        rule.runOnIdle {
            assertThat(currentComponentSpacingValues).isEqualTo(customComponentSpacingValues)
        }
    }
}
