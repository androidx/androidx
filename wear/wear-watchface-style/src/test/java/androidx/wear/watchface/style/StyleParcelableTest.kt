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
import android.os.Parcel
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleSettingWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(StyleTestRunner::class)
class StyleParcelableTest {

    private val icon1 = Icon.createWithContentUri("icon1")
    private val icon2 = Icon.createWithContentUri("icon2")
    private val icon3 = Icon.createWithContentUri("icon3")
    private val icon4 = Icon.createWithContentUri("icon4")
    private val option1 = ListUserStyleSetting.ListOption("1", "one", icon1)
    private val option2 = ListUserStyleSetting.ListOption("2", "two", icon2)
    private val option3 = ListUserStyleSetting.ListOption("3", "three", icon3)
    private val option4 = ListUserStyleSetting.ListOption("4", "four", icon4)

    @Test
    fun parcelAndUnparcelStyleSettingAndOption() {
        val settingIcon = Icon.createWithContentUri("settingIcon")
        val styleSetting = ListUserStyleSetting(
            "id",
            "displayName",
            "description",
            settingIcon,
            listOf(option1, option2, option3),
            listOf(Layer.BASE_LAYER)
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

        assertThat(unparceled.id).isEqualTo("id")
        assertThat(unparceled.displayName).isEqualTo("displayName")
        assertThat(unparceled.description).isEqualTo("description")
        assertThat(unparceled.icon!!.uri.toString()).isEqualTo("settingIcon")
        assertThat(unparceled.affectsLayers.size).isEqualTo(1)
        assertThat(unparceled.affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
        val optionArray =
            unparceled.options.filterIsInstance<ListUserStyleSetting.ListOption>()
                .toTypedArray()
        assertThat(optionArray.size).isEqualTo(3)
        assertThat(optionArray[0].id).isEqualTo("1")
        assertThat(optionArray[0].displayName).isEqualTo("one")
        assertThat(optionArray[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray[1].id).isEqualTo("2")
        assertThat(optionArray[1].displayName).isEqualTo("two")
        assertThat(optionArray[1].icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(optionArray[2].id).isEqualTo("3")
        assertThat(optionArray[2].displayName).isEqualTo("three")
        assertThat(optionArray[2].icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    fun marshallAndUnmarshallOptions() {
        val wireFormat1 = option1.toWireFormat()
        val wireFormat2 = option2.toWireFormat()
        val wireFormat3 = option3.toWireFormat()

        val unmarshalled1 = UserStyleSetting.Option.createFromWireFormat(wireFormat1)
            as ListUserStyleSetting.ListOption
        val unmarshalled2 = UserStyleSetting.Option.createFromWireFormat(wireFormat2)
            as ListUserStyleSetting.ListOption
        val unmarshalled3 = UserStyleSetting.Option.createFromWireFormat(wireFormat3)
            as ListUserStyleSetting.ListOption

        assertThat(unmarshalled1.id).isEqualTo("1")
        assertThat(unmarshalled1.displayName).isEqualTo("one")
        assertThat(unmarshalled1.icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(unmarshalled2.id).isEqualTo("2")
        assertThat(unmarshalled2.displayName).isEqualTo("two")
        assertThat(unmarshalled2.icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(unmarshalled3.id).isEqualTo("3")
        assertThat(unmarshalled3.displayName).isEqualTo("three")
        assertThat(unmarshalled3.icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    fun parcelAndUnparcelUserStyleSchema() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            "id1",
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(Layer.BASE_LAYER)
        )
        val styleSetting2 = ListUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(Layer.TOP_LAYER)
        )
        val styleSetting3 = BooleanUserStyleSetting(
            "id3",
            "displayName3",
            "description3",
            null,
            true,
            listOf(Layer.BASE_LAYER)
        )

        val srcSchema = UserStyleSchema(
            listOf(
                styleSetting1,
                styleSetting2,
                styleSetting3
            )
        )

        val parcel = Parcel.obtain()
        srcSchema.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val schema =
            UserStyleSchema(UserStyleSchemaWireFormat.CREATOR.createFromParcel(parcel))
        parcel.recycle()

        assert(schema.userStyleSettings[0] is ListUserStyleSetting)
        assertThat(schema.userStyleSettings[0].id).isEqualTo("id1")
        assertThat(schema.userStyleSettings[0].displayName).isEqualTo("displayName1")
        assertThat(schema.userStyleSettings[0].description).isEqualTo("description1")
        assertThat(schema.userStyleSettings[0].icon!!.uri.toString()).isEqualTo("settingIcon1")
        assertThat(schema.userStyleSettings[0].affectsLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[0].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
        val optionArray1 =
            schema.userStyleSettings[0].options.filterIsInstance<ListUserStyleSetting.ListOption>()
                .toTypedArray()
        assertThat(optionArray1.size).isEqualTo(2)
        assertThat(optionArray1[0].id).isEqualTo("1")
        assertThat(optionArray1[0].displayName).isEqualTo("one")
        assertThat(optionArray1[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray1[1].id).isEqualTo("2")
        assertThat(optionArray1[1].displayName).isEqualTo("two")
        assertThat(optionArray1[1].icon!!.uri.toString()).isEqualTo("icon2")

        assert(schema.userStyleSettings[1] is ListUserStyleSetting)
        assertThat(schema.userStyleSettings[1].id).isEqualTo("id2")
        assertThat(schema.userStyleSettings[1].displayName).isEqualTo("displayName2")
        assertThat(schema.userStyleSettings[1].description).isEqualTo("description2")
        assertThat(schema.userStyleSettings[1].icon!!.uri.toString()).isEqualTo("settingIcon2")
        assertThat(schema.userStyleSettings[1].affectsLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[1].affectsLayers.first()).isEqualTo(Layer.TOP_LAYER)
        val optionArray2 =
            schema.userStyleSettings[1].options.filterIsInstance<ListUserStyleSetting.ListOption>()
                .toTypedArray()
        assertThat(optionArray2.size).isEqualTo(2)
        assertThat(optionArray2[0].id).isEqualTo("3")
        assertThat(optionArray2[0].displayName).isEqualTo("three")
        assertThat(optionArray2[0].icon!!.uri.toString()).isEqualTo("icon3")
        assertThat(optionArray2[1].id).isEqualTo("4")
        assertThat(optionArray2[1].displayName).isEqualTo("four")
        assertThat(optionArray2[1].icon!!.uri.toString()).isEqualTo("icon4")

        assert(schema.userStyleSettings[2] is BooleanUserStyleSetting)
        assertThat(schema.userStyleSettings[2].id).isEqualTo("id3")
        assertThat(schema.userStyleSettings[2].displayName).isEqualTo("displayName3")
        assertThat(schema.userStyleSettings[2].description).isEqualTo("description3")
        assertThat(schema.userStyleSettings[2].icon).isEqualTo(null)
        assertThat(schema.userStyleSettings[2].affectsLayers.size).isEqualTo(1)
        assertThat(schema.userStyleSettings[2].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
    }

    @Test
    fun parcelAndUnparcelUserStyle() {
        val settingIcon1 = Icon.createWithContentUri("settingIcon1")
        val settingIcon2 = Icon.createWithContentUri("settingIcon2")
        val styleSetting1 = ListUserStyleSetting(
            "id1",
            "displayName1",
            "description1",
            settingIcon1,
            listOf(option1, option2),
            listOf(Layer.BASE_LAYER)
        )
        val styleSetting2 = ListUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            settingIcon2,
            listOf(option3, option4),
            listOf(Layer.TOP_LAYER)
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
            UserStyle(UserStyleWireFormat.CREATOR.createFromParcel(parcel), schema)
        parcel.recycle()

        assertThat(unparcelled.selectedOptions.size).isEqualTo(2)
        assertThat(unparcelled.selectedOptions[styleSetting1]!!.id).isEqualTo(option2.id)
        assertThat(unparcelled.selectedOptions[styleSetting2]!!.id).isEqualTo(option3.id)
    }

    @Test
    fun booleanUserStyleSetting_defaultValue() {
        val booleanUserStyleSettingDefaultTrue = BooleanUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            true,
            listOf(Layer.BASE_LAYER)
        )
        assertTrue(booleanUserStyleSettingDefaultTrue.getDefaultValue())

        val booleanUserStyleSettingDefaultFalse = BooleanUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            false,
            listOf(Layer.BASE_LAYER)
        )
        assertFalse(booleanUserStyleSettingDefaultFalse.getDefaultValue())
    }

    @Test
    fun doubleRangeUserStyleSetting_defaultValue() {
        val doubleRangeUserStyleSettingDefaultMin = DoubleRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            -1.0,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleSettingDefaultMin.getDefaultValue()).isEqualTo(-1.0)

        val doubleRangeUserStyleSettingDefaultMid = DoubleRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            0.5,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleSettingDefaultMid.getDefaultValue()).isEqualTo(0.5)

        val doubleRangeUserStyleSettingDefaultMax = DoubleRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            1.0,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleSettingDefaultMax.getDefaultValue()).isEqualTo(1.0)
    }

    @Test
    fun longRangeUserStyleSetting_defaultValue() {
        val longRangeUserStyleSettingDefaultMin = LongRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            -1,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleSettingDefaultMin.getDefaultValue()).isEqualTo(-1)

        val longRangeUserStyleSettingDefaultMid = LongRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            5,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleSettingDefaultMid.getDefaultValue()).isEqualTo(5)

        val longRangeUserStyleSettingDefaultMax = LongRangeUserStyleSetting(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            10,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleSettingDefaultMax.getDefaultValue()).isEqualTo(10)
    }

    @Test
    fun parcelAndUnparcelComplicationsUserStyleSetting() {
        val leftComplicationID = 101
        val rightComplicationID = 102
        val src = ComplicationsUserStyleSetting(
            "complications_style_setting",
            "Complications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                ComplicationsUserStyleSetting.ComplicationsOption(
                    "LEFT_AND_RIGHT_COMPLICATIONS",
                    "Both",
                    null,
                    listOf()
                ),
                ComplicationsUserStyleSetting.ComplicationsOption(
                    "NO_COMPLICATIONS",
                    "None",
                    null,
                    listOf(
                        ComplicationsUserStyleSetting.ComplicationOverlay(
                            leftComplicationID,
                            enabled = false
                        ),
                        ComplicationsUserStyleSetting.ComplicationOverlay(
                            rightComplicationID,
                            enabled = false
                        )
                    )
                ),
                ComplicationsUserStyleSetting.ComplicationsOption(
                    "LEFT_COMPLICATION",
                    "Left",
                    null,
                    listOf(
                        ComplicationsUserStyleSetting.ComplicationOverlay(
                            rightComplicationID,
                            enabled = false
                        )
                    )
                ),
                ComplicationsUserStyleSetting.ComplicationsOption(
                    "RIGHT_COMPLICATION",
                    "Right",
                    null,
                    listOf(
                        ComplicationsUserStyleSetting.ComplicationOverlay(
                            leftComplicationID,
                            enabled = false
                        )
                    )
                )
            ),
            listOf(Layer.COMPLICATIONS)
        )

        val parcel = Parcel.obtain()
        src.toWireFormat().writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val unparceled =
            UserStyleSetting.createFromWireFormat(
                UserStyleSettingWireFormat.CREATOR.createFromParcel(parcel)
            )
        parcel.recycle()

        assert(unparceled is ComplicationsUserStyleSetting)
        assertThat(unparceled.id).isEqualTo("complications_style_setting")

        val options = unparceled.options.filterIsInstance<
            ComplicationsUserStyleSetting.ComplicationsOption>()
        assertThat(options.size).isEqualTo(4)
        assertThat(options[0].id).isEqualTo("LEFT_AND_RIGHT_COMPLICATIONS")
        assertThat(options[0].complicationOverlays.size).isEqualTo(0)

        assertThat(options[1].id).isEqualTo("NO_COMPLICATIONS")
        assertThat(options[1].complicationOverlays.size).isEqualTo(2)
        val options1Overlays = ArrayList(options[1].complicationOverlays)
        assertThat(options1Overlays[0].complicationId).isEqualTo(leftComplicationID)
        assertFalse(options1Overlays[0].enabled!!)
        assertThat(options1Overlays[1].complicationId).isEqualTo(rightComplicationID)
        assertFalse(options1Overlays[1].enabled!!)

        assertThat(options[2].id).isEqualTo("LEFT_COMPLICATION")
        assertThat(options[2].complicationOverlays.size).isEqualTo(1)
        val options2Overlays = ArrayList(options[2].complicationOverlays)
        assertThat(options2Overlays[0].complicationId).isEqualTo(rightComplicationID)
        assertFalse(options2Overlays[0].enabled!!)

        assertThat(options[3].id).isEqualTo("RIGHT_COMPLICATION")
        assertThat(options[3].complicationOverlays.size).isEqualTo(1)
        val options3Overlays = ArrayList(options[3].complicationOverlays)
        assertThat(options3Overlays[0].complicationId).isEqualTo(leftComplicationID)
        assertFalse(options3Overlays[0].enabled!!)
    }
}
