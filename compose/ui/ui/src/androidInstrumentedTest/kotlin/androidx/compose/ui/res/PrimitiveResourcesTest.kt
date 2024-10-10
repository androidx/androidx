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

import androidx.compose.ui.test.ConfigChangeActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PrimitiveResourcesTest {

    @get:Rule val rule = createAndroidComposeRule<ConfigChangeActivity>()

    @Test
    fun integerResourceTest() {
        rule.setContent { assertThat(integerResource(R.integer.integer_value)).isEqualTo(123) }
    }

    @Test
    fun integerArrayResourceTest() {
        rule.setContent {
            assertThat(integerArrayResource(R.array.integer_array)).isEqualTo(intArrayOf(234, 345))
        }
    }

    @Test
    fun booleanResourceTest() {
        rule.setContent { assertThat(booleanResource(R.bool.boolean_value)).isTrue() }
    }

    @Test
    fun dimensionResourceTest() {
        rule.setContent { assertThat(dimensionResource(R.dimen.dimension_value)).isEqualTo(32.dp) }
    }

    @Test
    fun integerResourceTest_observesConfigChanges() {
        var int = 0

        rule.activity.setDarkMode(false)
        rule.setContent { int = integerResource(R.integer.day_night_int) }

        assertThat(int).isEqualTo(11)

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        assertThat(int).isEqualTo(99)
    }

    @Test
    fun integerArrayResourceTest_observesConfigChanges() {
        var intArray = intArrayOf()

        rule.activity.setDarkMode(false)
        rule.setContent { intArray = integerArrayResource(R.array.day_night_int_array) }

        assertThat(intArray).isEqualTo(intArrayOf(22, 33))

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        assertThat(intArray).isEqualTo(intArrayOf(88, 77))
    }

    @Test
    fun booleanResourceTest_observesConfigChanges() {
        var bool: Boolean? = null

        rule.activity.setDarkMode(false)
        rule.setContent { bool = booleanResource(R.bool.day_night_bool) }

        assertThat(bool).isEqualTo(false)

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        assertThat(bool).isEqualTo(true)
    }

    @Test
    fun dimensionResourceTest_observesConfigChanges() {
        var dimen = 0.dp

        rule.activity.setDarkMode(false)
        rule.setContent { dimen = dimensionResource(R.dimen.day_night_dimen) }

        assertThat(dimen).isEqualTo(100.dp)

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        assertThat(dimen).isEqualTo(200.dp)
    }
}
