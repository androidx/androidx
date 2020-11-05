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
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
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

        val parcelable = ParcelUtils.toParcelable(styleSetting.toWireFormat())

        val unparceled =
            UserStyleSetting.createFromWireFormat(ParcelUtils.fromParcelable(parcelable))
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

        val parceled = ParcelUtils.toParcelable(
            UserStyleSchemaWireFormat(
                listOf(
                    styleSetting1.toWireFormat(),
                    styleSetting2.toWireFormat(),
                    styleSetting3.toWireFormat()
                )
            )
        )

        val unparceled: UserStyleSchemaWireFormat = ParcelUtils.fromParcelable(parceled)
        val schema = unparceled.mSchema.map { UserStyleSetting.createFromWireFormat(it) }

        assert(schema[0] is ListUserStyleSetting)
        assertThat(schema[0].id).isEqualTo("id1")
        assertThat(schema[0].displayName).isEqualTo("displayName1")
        assertThat(schema[0].description).isEqualTo("description1")
        assertThat(schema[0].icon!!.uri.toString()).isEqualTo("settingIcon1")
        assertThat(schema[0].affectsLayers.size).isEqualTo(1)
        assertThat(schema[0].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
        val optionArray1 =
            schema[0].options.filterIsInstance<ListUserStyleSetting.ListOption>()
                .toTypedArray()
        assertThat(optionArray1.size).isEqualTo(2)
        assertThat(optionArray1[0].id).isEqualTo("1")
        assertThat(optionArray1[0].displayName).isEqualTo("one")
        assertThat(optionArray1[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray1[1].id).isEqualTo("2")
        assertThat(optionArray1[1].displayName).isEqualTo("two")
        assertThat(optionArray1[1].icon!!.uri.toString()).isEqualTo("icon2")

        assert(schema[1] is ListUserStyleSetting)
        assertThat(schema[1].id).isEqualTo("id2")
        assertThat(schema[1].displayName).isEqualTo("displayName2")
        assertThat(schema[1].description).isEqualTo("description2")
        assertThat(schema[1].icon!!.uri.toString()).isEqualTo("settingIcon2")
        assertThat(schema[1].affectsLayers.size).isEqualTo(1)
        assertThat(schema[1].affectsLayers.first()).isEqualTo(Layer.TOP_LAYER)
        val optionArray2 =
            schema[1].options.filterIsInstance<ListUserStyleSetting.ListOption>()
                .toTypedArray()
        assertThat(optionArray2.size).isEqualTo(2)
        assertThat(optionArray2[0].id).isEqualTo("3")
        assertThat(optionArray2[0].displayName).isEqualTo("three")
        assertThat(optionArray2[0].icon!!.uri.toString()).isEqualTo("icon3")
        assertThat(optionArray2[1].id).isEqualTo("4")
        assertThat(optionArray2[1].displayName).isEqualTo("four")
        assertThat(optionArray2[1].icon!!.uri.toString()).isEqualTo("icon4")

        assert(schema[2] is BooleanUserStyleSetting)
        assertThat(schema[2].id).isEqualTo("id3")
        assertThat(schema[2].displayName).isEqualTo("displayName3")
        assertThat(schema[2].description).isEqualTo("description3")
        assertThat(schema[2].icon).isEqualTo(null)
        assertThat(schema[2].affectsLayers.size).isEqualTo(1)
        assertThat(schema[2].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
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
        val parceled = ParcelUtils.toParcelable(userStyle.toWireFormat())

        val unparcelled =
            UserStyle(ParcelUtils.fromParcelable(parceled) as UserStyleWireFormat, schema)
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
}