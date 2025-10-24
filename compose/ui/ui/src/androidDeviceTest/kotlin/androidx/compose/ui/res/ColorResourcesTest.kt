/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.res

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ConfigChangeActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ColorResourcesTest {

    @get:Rule val rule = createAndroidComposeRule<ConfigChangeActivity>(StandardTestDispatcher())

    @Test
    fun colorResourceTest() {
        rule.setContent {
            assertThat(colorResource(R.color.color_resource)).isEqualTo(Color(0x12345678))
        }
    }

    @Test
    fun colorResource_observesConfigChanges() {
        var color = Color.Unspecified

        rule.activity.setDarkMode(false)
        rule.setContent { color = colorResource(R.color.day_night_color) }

        assertThat(color).isEqualTo(Color(0x11223344))

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        assertThat(color).isEqualTo(Color(0x44332211))
    }
}
