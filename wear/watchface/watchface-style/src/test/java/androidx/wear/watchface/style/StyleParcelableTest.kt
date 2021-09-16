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

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Parcel
import androidx.annotation.RequiresApi

import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.CustomValueUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleSettingWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(StyleTestRunner::class)
@RequiresApi(Build.VERSION_CODES.P)
public class StyleParcelableTest {

    private val icon1 = Icon.createWithContentUri("icon1")
    private val icon2 = Icon.createWithContentUri("icon2")
    private val icon3 = Icon.createWithContentUri("icon3")
    private val icon4 = Icon.createWithContentUri("icon4")
    private val option1 = ListOption(Option.Id("1"), "one", icon1)
    private val option2 = ListOption(Option.Id("2"), "two", icon2)
    private val option3 = ListOption(Option.Id("3"), "three", icon3)
    private val option4 = ListOption(Option.Id("4"), "four", icon4)

    @Before
    public fun setUp() {
        assumeTrue("These tests require API 28", Build.VERSION.SDK_INT >= 28)
    }

    @Test
    public fun parcelAndUnparcelStyleSettingAndOption() {
        val settingIcon = Icon.createWithContentUri("settingIcon")
        val styleSetting = ListUserStyleSetting(
            UserStyleSetting.Id("id"),
            "displayName",
            "description",
            settingIcon,
            listOf(option1, option2, option3),
            listOf(WatchFaceLayer.BASE)
        )

        val parcel = Parcel.obtain()
        styleSetting.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val unparceled =
            UserStyleSetting.createFromWireFormat(
                UserStyleSettingWireFormat.CREATOR.createFromParcel(parcel)
            )
        parcel.recycle()

        assert(unparceled is ListUserStyleSetting)

        assertThat(unparceled.id.value).isEqualTo("id")
        assertThat(unparceled.displayName).isEqualTo("displayName")
        assertThat(unparceled.description).isEqualTo("description")
        assertThat(unparceled.icon!!.uri.toString()).isEqualTo("settingIcon")
        assertThat(unparceled.affectedWatchFaceLayers.size).isEqualTo(1)
        assertThat(unparceled.affectedWatchFaceLayers.first()).isEqualTo(WatchFaceLayer.BASE)
        val optionArray = unparceled.options.filterIsInstance<ListOption>().toTypedArray()
        assertThat(optionArray.size).isEqualTo(3)
        assertThat(optionArray[0].id.value.decodeToString()).isEqualTo("1")
        assertThat(optionArray[0].displayName).isEqualTo("one")
        assertThat(optionArray[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray[1].id.value.decodeToString()).isEqualTo("2")
        assertThat(optionArray[1].displayName).isEqualTo("two")
        assertThat(optionArray[1].icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(optionArray[2].id.value.decodeToString()).isEqualTo("3")
        assertThat(optionArray[2].displayName).isEqualTo("three")
        assertThat(optionArray[2].icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    public fun marshallAndUnmarshallOptions() {
        val wireFormat1 = option1.toWireFormat()
        val wireFormat2 = option2.toWireFormat()
        val wireFormat3 = option3.toWireFormat()

        val unmarshalled1 = Option.createFromWireFormat(wireFormat1) as ListOption
        val unmarshalled2 = Option.createFromWireFormat(wireFormat2) as ListOption
        val unmarshalled3 = Option.createFromWireFormat(wireFormat3) as ListOption

        assertThat(unmarshalled1.id.value.decodeToString()).isEqualTo("1")
        assertThat(unmarshalled1.displayName).isEqualTo("one")
        assertThat(unmarshalled1.icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(unmarshalled2.id.value.decodeToString()).isEqualTo("2")
        assertThat(unmarshalled2.displayName).isEqualTo("two")
        assertThat(unmarshalled2.icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(unmarshalled3.id.value.decodeToString()).isEqualTo("3")
        assertThat(unmarshalled3.displayName).isEqualTo("three")
        assertThat(unmarshalled3.icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    public fun parcelAndUnparcelUserStyleSchema() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("id1"),
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(WatchFaceLayer.BASE)
        )
        val styleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
        val styleSetting3 = BooleanUserStyleSetting(
            UserStyleSetting.Id("id3"),
            "displayName3",
            "description3",
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )
        val styleSetting4 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        val srcSchema = UserStyleSchema(
            listOf(
                styleSetting1,
                styleSetting2,
                styleSetting3,
                styleSetting4
            )
        )

        val parcel = Parcel.obtain()
        srcSchema.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val schema =
            UserStyleSchema(UserStyleSchemaWireFormat.CREATOR.createFromParcel(parcel))
        parcel.recycle()

        assert(schema.userStyleSettings[0] is ListUserStyleSetting)
        assertThat(schema.userStyleSettings[0].id.value).isEqualTo("id1")
        assertThat(schema.userStyleSettings[0].displayName).isEqualTo("displayName1")
        assertThat(schema.userStyleSettings[0].description).isEqualTo("description1")
        assertThat(schema.userStyleSettings[0].icon!!.uri.toString()).isEqualTo("settingIcon1")
        assertThat(schema.userStyleSettings[0].affectedWatchFaceLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[0].affectedWatchFaceLayers.first()).isEqualTo(
            WatchFaceLayer.BASE
        )
        val optionArray1 =
            schema.userStyleSettings[0].options.filterIsInstance<ListOption>().toTypedArray()
        assertThat(optionArray1.size).isEqualTo(2)
        assertThat(optionArray1[0].id.value.decodeToString()).isEqualTo("1")
        assertThat(optionArray1[0].displayName).isEqualTo("one")
        assertThat(optionArray1[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray1[1].id.value.decodeToString()).isEqualTo("2")
        assertThat(optionArray1[1].displayName).isEqualTo("two")
        assertThat(optionArray1[1].icon!!.uri.toString()).isEqualTo("icon2")

        assert(schema.userStyleSettings[1] is ListUserStyleSetting)
        assertThat(schema.userStyleSettings[1].id.value).isEqualTo("id2")
        assertThat(schema.userStyleSettings[1].displayName).isEqualTo("displayName2")
        assertThat(schema.userStyleSettings[1].description).isEqualTo("description2")
        assertThat(schema.userStyleSettings[1].icon!!.uri.toString()).isEqualTo("settingIcon2")
        assertThat(schema.userStyleSettings[1].affectedWatchFaceLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[1].affectedWatchFaceLayers.first()).isEqualTo(
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        val optionArray2 =
            schema.userStyleSettings[1].options.filterIsInstance<ListOption>().toTypedArray()
        assertThat(optionArray2.size).isEqualTo(2)
        assertThat(optionArray2[0].id.value.decodeToString()).isEqualTo("3")
        assertThat(optionArray2[0].displayName).isEqualTo("three")
        assertThat(optionArray2[0].icon!!.uri.toString()).isEqualTo("icon3")
        assertThat(optionArray2[1].id.value.decodeToString()).isEqualTo("4")
        assertThat(optionArray2[1].displayName).isEqualTo("four")
        assertThat(optionArray2[1].icon!!.uri.toString()).isEqualTo("icon4")

        assert(schema.userStyleSettings[2] is BooleanUserStyleSetting)
        assertThat(schema.userStyleSettings[2].id.value).isEqualTo("id3")
        assertThat(schema.userStyleSettings[2].displayName).isEqualTo("displayName3")
        assertThat(schema.userStyleSettings[2].description).isEqualTo("description3")
        assertThat(schema.userStyleSettings[2].icon).isEqualTo(null)
        assertThat(schema.userStyleSettings[2].affectedWatchFaceLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[2].affectedWatchFaceLayers.first()).isEqualTo(
            WatchFaceLayer.BASE
        )

        assert(schema.userStyleSettings[3] is CustomValueUserStyleSetting)
        assertThat(schema.userStyleSettings[3].defaultOption.id.value.decodeToString())
            .isEqualTo("default")
        assertThat(schema.userStyleSettings[3].affectedWatchFaceLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[3].affectedWatchFaceLayers.first()).isEqualTo(
            WatchFaceLayer.BASE
        )
    }

    @Test
    public fun parcelAndUnparcelUserStyle() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("id1"),
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(WatchFaceLayer.BASE)
        )
        val styleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
        val schema = UserStyleSchema(listOf(styleSetting1, styleSetting2))
        val userStyle = UserStyle(
            hashMapOf(
                styleSetting1 as UserStyleSetting to option2 as UserStyleSetting.Option,
                styleSetting2 as UserStyleSetting to option3 as UserStyleSetting.Option
            )
        )

        val parcel = Parcel.obtain()
        userStyle.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val unparcelled =
            UserStyle(UserStyleData(UserStyleWireFormat.CREATOR.createFromParcel(parcel)), schema)
        parcel.recycle()

        assertThat(unparcelled.size).isEqualTo(2)
        assertThat(unparcelled[styleSetting1]!!.id.value.decodeToString())
            .isEqualTo(option2.id.value.decodeToString())
        assertThat(unparcelled[styleSetting2]!!.id.value.decodeToString())
            .isEqualTo(option3.id.value.decodeToString())
    }

    @Test
    public fun booleanUserStyleSetting_defaultValue() {
        val booleanUserStyleSettingDefaultTrue = BooleanUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )
        assertTrue(booleanUserStyleSettingDefaultTrue.getDefaultValue())

        val booleanUserStyleSettingDefaultFalse = BooleanUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            listOf(WatchFaceLayer.BASE),
            false
        )
        assertFalse(booleanUserStyleSettingDefaultFalse.getDefaultValue())
    }

    @Test
    public fun doubleRangeUserStyleSetting_defaultValue() {
        val doubleRangeUserStyleSettingDefaultMin = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            -1.0
        )
        assertThat(doubleRangeUserStyleSettingDefaultMin.defaultValue).isEqualTo(-1.0)

        val doubleRangeUserStyleSettingDefaultMid = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            0.5
        )
        assertThat(doubleRangeUserStyleSettingDefaultMid.defaultValue).isEqualTo(0.5)

        val doubleRangeUserStyleSettingDefaultMax = DoubleRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            listOf(WatchFaceLayer.BASE),
            1.0
        )
        assertThat(doubleRangeUserStyleSettingDefaultMax.defaultValue).isEqualTo(1.0)
    }

    @Test
    public fun longRangeUserStyleSetting_defaultValue() {
        val longRangeUserStyleSettingDefaultMin = LongRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1,
            10,
            listOf(WatchFaceLayer.BASE),
            -1,
        )
        assertThat(longRangeUserStyleSettingDefaultMin.defaultValue).isEqualTo(-1)

        val longRangeUserStyleSettingDefaultMid = LongRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1,
            10,
            listOf(WatchFaceLayer.BASE),
            5
        )
        assertThat(longRangeUserStyleSettingDefaultMid.defaultValue).isEqualTo(5)

        val longRangeUserStyleSettingDefaultMax = LongRangeUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            null,
            -1,
            10,
            listOf(WatchFaceLayer.BASE),
            10
        )
        assertThat(longRangeUserStyleSettingDefaultMax.defaultValue).isEqualTo(10)
    }

    @Test
    public fun parcelAndUnparcelComplicationsUserStyleSetting() {
        val leftComplicationID = 101
        val rightComplicationID = 102
        val src = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            "Complications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
                    "Both",
                    null,
                    listOf()
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id("NO_COMPLICATIONS"),
                    "None",
                    null,
                    listOf(
                        ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(
                            leftComplicationID,
                            enabled = false
                        ),
                        ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(
                            rightComplicationID,
                            enabled = false
                        )
                    )
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id("LEFT_COMPLICATION"),
                    "Left",
                    null,
                    listOf(
                        ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(
                            rightComplicationID,
                            enabled = false
                        )
                    )
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id("RIGHT_COMPLICATION"),
                    "Right",
                    null,
                    listOf(
                        ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay(
                            leftComplicationID,
                            enabled = false
                        )
                    )
                )
            ),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

        val parcel = Parcel.obtain()
        src.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val unparceled =
            UserStyleSetting.createFromWireFormat(
                UserStyleSettingWireFormat.CREATOR.createFromParcel(parcel)
            )
        parcel.recycle()

        assert(unparceled is ComplicationSlotsUserStyleSetting)
        assertThat(unparceled.id.value).isEqualTo("complications_style_setting")

        val options = unparceled.options.filterIsInstance<
            ComplicationSlotsUserStyleSetting.ComplicationSlotsOption>()
        assertThat(options.size).isEqualTo(4)
        assertThat(options[0].id.value.decodeToString()).isEqualTo("LEFT_AND_RIGHT_COMPLICATIONS")
        assertThat(options[0].complicationSlotOverlays.size).isEqualTo(0)

        assertThat(options[1].id.value.decodeToString()).isEqualTo("NO_COMPLICATIONS")
        assertThat(options[1].complicationSlotOverlays.size).isEqualTo(2)
        val options1Overlays = ArrayList(options[1].complicationSlotOverlays)
        assertThat(options1Overlays[0].complicationSlotId).isEqualTo(leftComplicationID)
        assertFalse(options1Overlays[0].enabled!!)
        assertThat(options1Overlays[1].complicationSlotId).isEqualTo(rightComplicationID)
        assertFalse(options1Overlays[1].enabled!!)

        assertThat(options[2].id.value.decodeToString()).isEqualTo("LEFT_COMPLICATION")
        assertThat(options[2].complicationSlotOverlays.size).isEqualTo(1)
        val options2Overlays = ArrayList(options[2].complicationSlotOverlays)
        assertThat(options2Overlays[0].complicationSlotId).isEqualTo(rightComplicationID)
        assertFalse(options2Overlays[0].enabled!!)

        assertThat(options[3].id.value.decodeToString()).isEqualTo("RIGHT_COMPLICATION")
        assertThat(options[3].complicationSlotOverlays.size).isEqualTo(1)
        val options3Overlays = ArrayList(options[3].complicationSlotOverlays)
        assertThat(options3Overlays[0].complicationSlotId).isEqualTo(leftComplicationID)
        assertFalse(options3Overlays[0].enabled!!)
    }

    @Test
    public fun styleSchemaToString() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("id1"),
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(WatchFaceLayer.BASE)
        )
        val styleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
        val styleSetting3 = BooleanUserStyleSetting(
            UserStyleSetting.Id("id3"),
            "displayName3",
            "description3",
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )
        val styleSetting4 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        val schema = UserStyleSchema(
            listOf(
                styleSetting1,
                styleSetting2,
                styleSetting3,
                styleSetting4
            )
        )

        assertThat(schema.toString()).isEqualTo(
            "[{id1 : 1, 2}, {id2 : 3, 4}, {id3 : true, false}, {CustomValue : default}]"
        )
    }

    @Ignore
    @Test
    public fun userStyleToString() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("id1"),
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(WatchFaceLayer.BASE)
        )
        val styleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("id2"),
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
        val style = UserStyle(
            mapOf(
                styleSetting1 to option2,
                styleSetting2 to option3
            )
        )

        assertThat(style.toString()).contains("id1 -> 2")
        assertThat(style.toString()).contains("id2 -> 3")
    }
}
