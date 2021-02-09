/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.style

import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(StyleTestRunner::class)
class UserStyleSettingTest {

    @Test
    fun rangedUserStyleSetting_getOptionForId_returns_default_for_bad_input() {
        val defaultValue = 0.75
        val rangedUserStyleSetting =
            DoubleRangeUserStyleSetting(
                "example_setting",
                "Example Ranged Setting",
                "An example setting",
                null,
                0.0,
                1.0,
                listOf(Layer.BASE_LAYER),
                defaultValue
            )

        assertThat(rangedUserStyleSetting.getOptionForId("not a number").id)
            .isEqualTo(defaultValue.toString())

        assertThat(rangedUserStyleSetting.getOptionForId("-1").id)
            .isEqualTo(defaultValue.toString())

        assertThat(rangedUserStyleSetting.getOptionForId("10").id)
            .isEqualTo(defaultValue.toString())
    }

    @Test
    fun rangedUserStyleSetting_getOptionForId() {
        val defaultValue = 0.75
        val rangedUserStyleSetting =
            DoubleRangeUserStyleSetting(
                "example_setting",
                "Example Ranged Setting",
                "An example setting",
                null,
                0.0,
                1.0,
                listOf(Layer.BASE_LAYER),
                defaultValue
            )

        assertThat(rangedUserStyleSetting.getOptionForId("0").id)
            .isEqualTo("0.0")

        assertThat(rangedUserStyleSetting.getOptionForId("0.5").id)
            .isEqualTo("0.5")

        assertThat(rangedUserStyleSetting.getOptionForId("1").id)
            .isEqualTo("1.0")
    }

    @Test
    fun maximumUserStyleSettingIdLength() {
        // OK.
        DoubleRangeUserStyleSetting(
            "x".repeat(UserStyleSetting.maxIdLength),
            "",
            "",
            null,
            0.0,
            1.0,
            emptyList(),
            1.0
        )

        try {
            // Not OK.
            DoubleRangeUserStyleSetting(
                "x".repeat(UserStyleSetting.maxIdLength + 1),
                "",
                "",
                null,
                0.0,
                1.0,
                emptyList(),
                1.0
            )
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun maximumOptionIdLength() {
        // OK.
        UserStyleSetting.ListUserStyleSetting.ListOption(
            "x".repeat(UserStyleSetting.Option.maxIdLength),
            "",
            null
        )

        try {
            // Not OK.
            UserStyleSetting.ListUserStyleSetting.ListOption(
                "x".repeat(UserStyleSetting.Option.maxIdLength + 1),
                "",
                null
            )
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Expected
        }
    }
}