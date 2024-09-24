/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.test.R
import com.google.common.truth.Truth
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
public class IconWireSizeAndDimensionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testIcon = Icon.createWithResource(context, R.drawable.test_icon)

    @Test
    public fun resource() {
        val wireSizeAndDimensions = testIcon.getWireSizeAndDimensions(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Truth.assertThat(wireSizeAndDimensions.wireSizeBytes).isEqualTo(547)
        } else {
            Truth.assertThat(wireSizeAndDimensions.wireSizeBytes).isNull()
        }
        Truth.assertThat(wireSizeAndDimensions.width).isEqualTo(72)
        Truth.assertThat(wireSizeAndDimensions.height).isEqualTo(72)
    }

    @Test
    public fun bitmap() {
        val wireSizeAndDimensions =
            Icon.createWithBitmap(Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888))
                .getWireSizeAndDimensions(context)
        Truth.assertThat(wireSizeAndDimensions.wireSizeBytes).isNull()
        Truth.assertThat(wireSizeAndDimensions.width).isEqualTo(10)
        Truth.assertThat(wireSizeAndDimensions.height).isEqualTo(20)
    }

    @Test
    public fun estimateWireSizeInBytesAndValidateIconDimensions_BooleanUserStyleSetting_IconOK() {
        val setting =
            UserStyleSetting.BooleanUserStyleSetting.Builder(
                    UserStyleSetting.Id("ID1"),
                    listOf(WatchFaceLayer.BASE),
                    false,
                    "displayName",
                    "description"
                )
                .setIcon(testIcon)
                .build()

        val estimate = setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Truth.assertThat(estimate).isEqualTo(582)
        } else {
            Truth.assertThat(estimate).isEqualTo(35)
        }
    }

    @Test
    public fun estimateWireSizeInBytesAndValidateIconDimensions_BooleanUserStyleSetting_IconBad() {
        val setting =
            UserStyleSetting.BooleanUserStyleSetting.Builder(
                    UserStyleSetting.Id("ID1"),
                    listOf(WatchFaceLayer.BASE),
                    false,
                    "displayName",
                    "description"
                )
                .setIcon(testIcon)
                .build()

        try {
            setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 10, 20)
            fail("An exception should have been thrown because the icon is too big")
        } catch (e: Exception) {
            Truth.assertThat(e.message)
                .contains(
                    "UserStyleSetting id ID1 has a 72 x 72 icon. " +
                        "This is too big, the maximum size is 10 x 20."
                )
        }
    }

    @Test
    public fun estimateWireSizeInBytes_DoubleRangeUserStyleSetting() {
        val setting =
            UserStyleSetting.DoubleRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("example_setting"),
                    0.0,
                    1.0,
                    0.1,
                    listOf(WatchFaceLayer.BASE),
                    "Example Ranged Setting",
                    "An example setting"
                )
                .build()
        Truth.assertThat(
                setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
            )
            .isEqualTo(87)
    }

    @Test
    public fun estimateWireSizeInBytes_ListUserStyleSetting() {
        val classicStyleOption =
            UserStyleSetting.ListUserStyleSetting.ListOption.Builder(
                    UserStyleSetting.Option.Id("classic_style"),
                    "Classic",
                    "Classic screen reader name"
                )
                .setIcon(testIcon)
                .build()

        val modernStyleOption =
            UserStyleSetting.ListUserStyleSetting.ListOption.Builder(
                    UserStyleSetting.Option.Id("modern_style"),
                    "Modern",
                    "Modern screen reader name"
                )
                .setIcon(testIcon)
                .build()

        val gothicStyleOption =
            UserStyleSetting.ListUserStyleSetting.ListOption.Builder(
                    UserStyleSetting.Option.Id("gothic_style"),
                    "Gothic",
                    "Gothic screen reader name"
                )
                .setIcon(testIcon)
                .build()

        val watchHandStyleList = listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

        val setting =
            UserStyleSetting.ListUserStyleSetting.Builder(
                    UserStyleSetting.Id("hand_style_setting"),
                    watchHandStyleList,
                    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    "Hand Style",
                    "Hand visual look"
                )
                .setIcon(testIcon)
                .build()

        val estimate = setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Truth.assertThat(estimate).isEqualTo(2296)
        } else {
            Truth.assertThat(estimate).isEqualTo(108)
        }
    }

    @Test
    public fun estimateWireSizeInBytes_LongRangeUserStyleSetting() {
        val setting =
            UserStyleSetting.LongRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("watch_hand_length_style_setting"),
                    1,
                    100,
                    2,
                    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    "Hand length",
                    "Scale of watch hands"
                )
                .setIcon(testIcon)
                .build()

        val estimate = setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Truth.assertThat(estimate).isEqualTo(641)
        } else {
            Truth.assertThat(estimate).isEqualTo(94)
        }
    }

    @Test
    public fun estimateWireSizeInBytes_CustomValueUserStyleSetting() {
        val setting =
            UserStyleSetting.CustomValueUserStyleSetting(
                listOf(WatchFaceLayer.BASE),
                "custom value".encodeToByteArray()
            )
        Truth.assertThat(
                setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
            )
            .isEqualTo(31)
    }

    @Test
    public fun estimateWireSizeInBytes_ComplicationSlotsUserStyleSetting() {
        val leftComplicationID = 101
        val rightComplicationID = 102
        val setting =
            ComplicationSlotsUserStyleSetting.Builder(
                    UserStyleSetting.Id("complications_style_setting"),
                    complicationConfig =
                        listOf(
                            ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                                    UserStyleSetting.Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
                                    listOf(),
                                    "Both",
                                    "Both screen reader name"
                                )
                                .setIcon(testIcon)
                                .build(),
                            ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                                    UserStyleSetting.Option.Id("NO_COMPLICATIONS"),
                                    listOf(
                                        ComplicationSlotOverlay.Builder(leftComplicationID)
                                            .setEnabled(false)
                                            .build(),
                                        ComplicationSlotOverlay.Builder(rightComplicationID)
                                            .setEnabled(false)
                                            .build()
                                    ),
                                    "None",
                                    "None screen reader name"
                                )
                                .setIcon(testIcon)
                                .build(),
                            ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                                    UserStyleSetting.Option.Id("LEFT_COMPLICATION"),
                                    listOf(
                                        ComplicationSlotOverlay.Builder(rightComplicationID)
                                            .setEnabled(false)
                                            .build()
                                    ),
                                    "Left",
                                    "Left screen reader name"
                                )
                                .setIcon(testIcon)
                                .build(),
                            ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                                    UserStyleSetting.Option.Id("RIGHT_COMPLICATION"),
                                    listOf(
                                        ComplicationSlotOverlay.Builder(leftComplicationID)
                                            .setEnabled(false)
                                            .build()
                                    ),
                                    "Right",
                                    "Right screen reader name"
                                )
                                .setIcon(testIcon)
                                .build()
                        ),
                    listOf(WatchFaceLayer.COMPLICATIONS),
                    "Complications",
                    "Number and position"
                )
                .setIcon(testIcon)
                .build()

        val estimate = setting.estimateWireSizeInBytesAndValidateIconDimensions(context, 100, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Truth.assertThat(estimate).isEqualTo(2962)
        } else {
            Truth.assertThat(estimate).isEqualTo(227)
        }
    }
}
