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

import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption
import androidx.wear.watchface.style.UserStyleSetting.Option
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(StyleTestRunner::class)
public class UserStyleSettingTest {

    private fun doubleToByteArray(value: Double) =
        ByteArray(8).apply { ByteBuffer.wrap(this).putDouble(value) }

    private fun byteArrayToDouble(value: ByteArray) = ByteBuffer.wrap(value).double

    @Test
    public fun rangedUserStyleSetting_getOptionForId_returns_default_for_bad_input() {
        val defaultValue = 0.75
        val rangedUserStyleSetting =
            DoubleRangeUserStyleSetting(
                UserStyleSetting.Id("example_setting"),
                "Example Ranged Setting",
                "An example setting",
                null,
                0.0,
                1.0,
                listOf(WatchFaceLayer.BASE),
                defaultValue
            )

        assertThat(
            (
                rangedUserStyleSetting.getOptionForId("not a number".encodeToByteArray()) as
                    DoubleRangeOption
                ).value
        ).isEqualTo(defaultValue)

        assertThat(
            (rangedUserStyleSetting.getOptionForId("-1".encodeToByteArray()) as DoubleRangeOption)
                .value
        ).isEqualTo(defaultValue)

        assertThat(
            (rangedUserStyleSetting.getOptionForId("10".encodeToByteArray()) as DoubleRangeOption)
                .value
        ).isEqualTo(defaultValue)
    }

    @Test
    public fun byteArrayConversion() {
        assertThat(BooleanOption(true).value).isEqualTo(true)
        assertThat(BooleanOption(false).value).isEqualTo(false)
        assertThat(DoubleRangeOption(123.4).value).isEqualTo(123.4)
        assertThat(LongRangeOption(1234).value).isEqualTo(1234)
    }

    @Test
    public fun rangedUserStyleSetting_getOptionForId() {
        val defaultValue = 0.75
        val rangedUserStyleSetting =
            DoubleRangeUserStyleSetting(
                UserStyleSetting.Id("example_setting"),
                "Example Ranged Setting",
                "An example setting",
                null,
                0.0,
                1.0,
                listOf(WatchFaceLayer.BASE),
                defaultValue
            )

        assertThat(
            byteArrayToDouble(
                rangedUserStyleSetting.getOptionForId(doubleToByteArray(0.0)).id.value
            )
        ).isEqualTo(0.0)

        assertThat(
            byteArrayToDouble(
                rangedUserStyleSetting.getOptionForId(doubleToByteArray(0.5)).id.value
            )
        ).isEqualTo(0.5)

        assertThat(
            byteArrayToDouble(
                rangedUserStyleSetting.getOptionForId(doubleToByteArray(1.0)).id.value
            )
        ).isEqualTo(1.0)
    }

    @Test
    public fun maximumUserStyleSettingIdLength() {
        // OK.
        UserStyleSetting.Id("x".repeat(UserStyleSetting.Id.MAX_LENGTH))

        try {
            // Not OK.
            UserStyleSetting.Id("x".repeat(UserStyleSetting.Id.MAX_LENGTH + 1))
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    public fun maximumOptionIdLength() {
        // OK.
        Option.Id("x".repeat(Option.Id.MAX_LENGTH))

        try {
            // Not OK.
            Option.Id("x".repeat(Option.Id.MAX_LENGTH + 1))
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Expected
        }
    }
}