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
    private val option1 = ListUserStyleCategory.ListOption("1", "one", icon1)
    private val option2 = ListUserStyleCategory.ListOption("2", "two", icon2)
    private val option3 = ListUserStyleCategory.ListOption("3", "three", icon3)
    private val option4 = ListUserStyleCategory.ListOption("4", "four", icon4)

    @Test
    fun parcelAndUnparcelStyleCategoryAndOption() {
        val categoryIcon = Icon.createWithContentUri("categoryIcon")
        val styleCategory = ListUserStyleCategory(
            "id",
            "displayName",
            "description",
            categoryIcon,
            listOf(option1, option2, option3),
            listOf(Layer.BASE_LAYER)
        )

        val parcelable = ParcelUtils.toParcelable(styleCategory.toWireFormat())

        val unparceled =
            UserStyleCategory.createFromWireFormat(ParcelUtils.fromParcelable(parcelable))
        assert(unparceled is ListUserStyleCategory)

        assertThat(unparceled.id).isEqualTo("id")
        assertThat(unparceled.displayName).isEqualTo("displayName")
        assertThat(unparceled.description).isEqualTo("description")
        assertThat(unparceled.icon!!.uri.toString()).isEqualTo("categoryIcon")
        assertThat(unparceled.affectsLayers.size).isEqualTo(1)
        assertThat(unparceled.affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
        val optionArray =
            unparceled.options.filterIsInstance<ListUserStyleCategory.ListOption>()
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

        val unmarshalled1 = UserStyleCategory.Option.createFromWireFormat(wireFormat1)
            as ListUserStyleCategory.ListOption
        val unmarshalled2 = UserStyleCategory.Option.createFromWireFormat(wireFormat2)
            as ListUserStyleCategory.ListOption
        val unmarshalled3 = UserStyleCategory.Option.createFromWireFormat(wireFormat3)
            as ListUserStyleCategory.ListOption

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
        val categoryIcon1 = Icon.createWithContentUri("categoryIcon1")
        val categoryIcon2 = Icon.createWithContentUri("categoryIcon2")
        val styleCategory1 = ListUserStyleCategory(
            "id1",
            "displayName1",
            "description1",
            categoryIcon1,
            listOf(option1, option2),
            listOf(Layer.BASE_LAYER)
        )
        val styleCategory2 = ListUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            categoryIcon2,
            listOf(option3, option4),
            listOf(Layer.TOP_LAYER)
        )
        val styleCategory3 = BooleanUserStyleCategory(
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
                    styleCategory1.toWireFormat(),
                    styleCategory2.toWireFormat(),
                    styleCategory3.toWireFormat()
                )
            )
        )

        val unparceled: UserStyleSchemaWireFormat = ParcelUtils.fromParcelable(parceled)
        val schema = unparceled.mSchema.map { UserStyleCategory.createFromWireFormat(it) }

        assert(schema[0] is ListUserStyleCategory)
        assertThat(schema[0].id).isEqualTo("id1")
        assertThat(schema[0].displayName).isEqualTo("displayName1")
        assertThat(schema[0].description).isEqualTo("description1")
        assertThat(schema[0].icon!!.uri.toString()).isEqualTo("categoryIcon1")
        assertThat(schema[0].affectsLayers.size).isEqualTo(1)
        assertThat(schema[0].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
        val optionArray1 =
            schema[0].options.filterIsInstance<ListUserStyleCategory.ListOption>()
                .toTypedArray()
        assertThat(optionArray1.size).isEqualTo(2)
        assertThat(optionArray1[0].id).isEqualTo("1")
        assertThat(optionArray1[0].displayName).isEqualTo("one")
        assertThat(optionArray1[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray1[1].id).isEqualTo("2")
        assertThat(optionArray1[1].displayName).isEqualTo("two")
        assertThat(optionArray1[1].icon!!.uri.toString()).isEqualTo("icon2")

        assert(schema[1] is ListUserStyleCategory)
        assertThat(schema[1].id).isEqualTo("id2")
        assertThat(schema[1].displayName).isEqualTo("displayName2")
        assertThat(schema[1].description).isEqualTo("description2")
        assertThat(schema[1].icon!!.uri.toString()).isEqualTo("categoryIcon2")
        assertThat(schema[1].affectsLayers.size).isEqualTo(1)
        assertThat(schema[1].affectsLayers.first()).isEqualTo(Layer.TOP_LAYER)
        val optionArray2 =
            schema[1].options.filterIsInstance<ListUserStyleCategory.ListOption>()
                .toTypedArray()
        assertThat(optionArray2.size).isEqualTo(2)
        assertThat(optionArray2[0].id).isEqualTo("3")
        assertThat(optionArray2[0].displayName).isEqualTo("three")
        assertThat(optionArray2[0].icon!!.uri.toString()).isEqualTo("icon3")
        assertThat(optionArray2[1].id).isEqualTo("4")
        assertThat(optionArray2[1].displayName).isEqualTo("four")
        assertThat(optionArray2[1].icon!!.uri.toString()).isEqualTo("icon4")

        assert(schema[2] is BooleanUserStyleCategory)
        assertThat(schema[2].id).isEqualTo("id3")
        assertThat(schema[2].displayName).isEqualTo("displayName3")
        assertThat(schema[2].description).isEqualTo("description3")
        assertThat(schema[2].icon).isEqualTo(null)
        assertThat(schema[2].affectsLayers.size).isEqualTo(1)
        assertThat(schema[2].affectsLayers.first()).isEqualTo(Layer.BASE_LAYER)
    }

    @Test
    fun parcelAndUnparcelUserStyle() {
        val categoryIcon1 = Icon.createWithContentUri("categoryIcon1")
        val categoryIcon2 = Icon.createWithContentUri("categoryIcon2")
        val styleCategory1 = ListUserStyleCategory(
            "id1",
            "displayName1",
            "description1",
            categoryIcon1,
            listOf(option1, option2),
            listOf(Layer.BASE_LAYER)
        )
        val styleCategory2 = ListUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            categoryIcon2,
            listOf(option3, option4),
            listOf(Layer.TOP_LAYER)
        )
        val schema = listOf(styleCategory1, styleCategory2)
        val userStyle = UserStyle(
            hashMapOf(
                styleCategory1 as UserStyleCategory to option2 as UserStyleCategory.Option,
                styleCategory2 as UserStyleCategory to option3 as UserStyleCategory.Option
            )
        )
        val parceled = ParcelUtils.toParcelable(userStyle.toWireFormat())

        val unparcelled =
            UserStyle(ParcelUtils.fromParcelable(parceled) as UserStyleWireFormat, schema)
        assertThat(unparcelled.options.size).isEqualTo(2)
        assertThat(unparcelled.options[styleCategory1]!!.id).isEqualTo(option2.id)
        assertThat(unparcelled.options[styleCategory2]!!.id).isEqualTo(option3.id)
    }

    @Test
    fun booleanUserStyleCategory_defaultValue() {
        val booleanUserStyleCategoryDefaultTrue = BooleanUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            true,
            listOf(Layer.BASE_LAYER)
        )
        assertTrue(booleanUserStyleCategoryDefaultTrue.getDefaultValue())

        val booleanUserStyleCategoryDefaultFalse = BooleanUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            false,
            listOf(Layer.BASE_LAYER)
        )
        assertFalse(booleanUserStyleCategoryDefaultFalse.getDefaultValue())
    }

    @Test
    fun doubleRangeUserStyleCategory_defaultValue() {
        val doubleRangeUserStyleCategoryDefaultMin = DoubleRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            -1.0,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleCategoryDefaultMin.getDefaultValue()).isEqualTo(-1.0)

        val doubleRangeUserStyleCategoryDefaultMid = DoubleRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            0.5,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleCategoryDefaultMid.getDefaultValue()).isEqualTo(0.5)

        val doubleRangeUserStyleCategoryDefaultMax = DoubleRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1.0,
            1.0,
            1.0,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(doubleRangeUserStyleCategoryDefaultMax.getDefaultValue()).isEqualTo(1.0)
    }

    @Test
    fun longRangeUserStyleCategory_defaultValue() {
        val longRangeUserStyleCategoryDefaultMin = LongRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            -1,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleCategoryDefaultMin.getDefaultValue()).isEqualTo(-1)

        val longRangeUserStyleCategoryDefaultMid = LongRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            5,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleCategoryDefaultMid.getDefaultValue()).isEqualTo(5)

        val longRangeUserStyleCategoryDefaultMax = LongRangeUserStyleCategory(
            "id2",
            "displayName2",
            "description2",
            null,
            -1,
            10,
            10,
            listOf(Layer.BASE_LAYER)
        )
        assertThat(longRangeUserStyleCategoryDefaultMax.getDefaultValue()).isEqualTo(10)
    }
}