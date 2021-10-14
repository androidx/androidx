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

import android.graphics.Bitmap
import android.graphics.drawable.Icon
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
import kotlin.test.assertFailsWith

@RunWith(StyleTestRunner::class)
public class UserStyleSettingTest {

    private fun doubleToOptionId(value: Double) =
        Option.Id(ByteArray(8).apply { ByteBuffer.wrap(this).putDouble(value) })

    private fun byteArrayToDouble(value: ByteArray) = ByteBuffer.wrap(value).double

    private val icon_100x100 =
        Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

    private val icon_10x10 =
        Icon.createWithBitmap(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

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
                rangedUserStyleSetting.getOptionForId(
                    Option.Id("not a number".encodeToByteArray())
                ) as DoubleRangeOption
                ).value
        ).isEqualTo(defaultValue)

        assertThat(
            (
                rangedUserStyleSetting.getOptionForId(
                    Option.Id("-1".encodeToByteArray())
                ) as DoubleRangeOption
                ).value
        ).isEqualTo(defaultValue)

        assertThat(
            (
                rangedUserStyleSetting.getOptionForId(
                    Option.Id("10".encodeToByteArray())
                ) as DoubleRangeOption
                ).value
        ).isEqualTo(defaultValue)
    }

    @Test
    public fun byteArrayConversion() {
        assertThat(BooleanOption.TRUE.value).isEqualTo(true)
        assertThat(BooleanOption.FALSE.value).isEqualTo(false)
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
                rangedUserStyleSetting.getOptionForId(doubleToOptionId(0.0)).id.value
            )
        ).isEqualTo(0.0)

        assertThat(
            byteArrayToDouble(
                rangedUserStyleSetting.getOptionForId(doubleToOptionId(0.5)).id.value
            )
        ).isEqualTo(0.5)

        assertThat(
            byteArrayToDouble(
                rangedUserStyleSetting.getOptionForId(doubleToOptionId(1.0)).id.value
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

    @Test
    public fun equalsBasedOnId() {
        val setting = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        val settingCopy = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        val settings1ModifiedInfo = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting (modified)",
            "An example setting (modified)",
            null,
            0.0,
            100.0,
            listOf(WatchFaceLayer.BASE),
            3.0
        )
        val settings1ModifiedId = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting_modified"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        assertThat(setting).isEqualTo(setting)
        assertThat(setting).isEqualTo(settingCopy)
        assertThat(setting).isEqualTo(settings1ModifiedInfo)
        assertThat(setting).isNotEqualTo(settings1ModifiedId)
    }

    @Test
    public fun hashcodeBasedOnId() {
        val setting = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        val settingCopy = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        val settings1ModifiedInfo = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting"),
            "Example Ranged Setting (modified)",
            "An example setting (modified)",
            null,
            0.0,
            100.0,
            listOf(WatchFaceLayer.BASE),
            3.0
        )
        val settings1ModifiedId = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("example_setting_modified"),
            "Example Ranged Setting",
            "An example setting",
            null,
            0.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.1
        )
        assertThat(setting.hashCode()).isEqualTo(setting.hashCode())
        assertThat(setting.hashCode()).isEqualTo(settingCopy.hashCode())
        assertThat(setting.hashCode()).isEqualTo(settings1ModifiedInfo.hashCode())
        assertThat(setting.hashCode()).isNotEqualTo(settings1ModifiedId.hashCode())
    }

    @Test
    public fun noDuplicatedComplicationSlotOptions() {
        val leftComplicationSlot =
            UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(1)
        val rightComplicationSlot =
            UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(2)
        assertFailsWith<IllegalArgumentException>("should not allow duplicates") {
            UserStyleSetting.ComplicationSlotsUserStyleSetting(
                UserStyleSetting.Id("complication_location"),
                "Complication Location",
                "Configure the location of the complications on the watch face",
                icon = null,
                listOf(
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id("both"),
                        "left and right complications",
                        icon = null,
                        listOf(leftComplicationSlot, rightComplicationSlot),
                    ),
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id("left"),
                        "left complication",
                        icon = null,
                        listOf(leftComplicationSlot),
                    ),
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id("right"),
                        "right complication",
                        icon = null,
                        listOf(rightComplicationSlot),
                    ),
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id("both"),
                        "right and left complications",
                        icon = null,
                        listOf(rightComplicationSlot, leftComplicationSlot),
                    )
                ),
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS
            )
        }
    }

    @Test
    public fun noDuplicatedListOptions() {
        assertFailsWith<IllegalArgumentException>("should not allow duplicates") {
            UserStyleSetting.ListUserStyleSetting(
                UserStyleSetting.Id("hands"),
                "Hands",
                "Configure the hands of the watch face",
                icon = null,
                listOf(
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("plain"),
                        "plain hands",
                        icon = null
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("florescent"),
                        "florescent hands",
                        icon = null
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("thick"),
                        "thick hands",
                        icon = null
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("plain"),
                        "simple hands",
                        icon = null
                    )
                ),
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS
            )
        }
    }
}
