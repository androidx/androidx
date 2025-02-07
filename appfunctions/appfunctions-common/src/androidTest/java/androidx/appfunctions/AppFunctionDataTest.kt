/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import androidx.annotation.RequiresApi
import androidx.test.filters.SdkSuppress
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test

@RequiresApi(35)
@SdkSuppress(minSdkVersion = 31)
class AppFunctionDataTest {
    @Test
    fun getSingleProperty_shouldThrowsException_ifNotExist() {
        val appFunctionData = AppFunctionData.EMPTY

        Assert.assertThrows(
            "No elements found under [testLong]",
            NoSuchElementException::class.java,
        ) {
            appFunctionData.getLong("testLong")
        }
        Assert.assertThrows(
            "No elements found under [testDouble]",
            NoSuchElementException::class.java,
        ) {
            appFunctionData.getDouble("testDouble")
        }
        Assert.assertThrows(
            "No elements found under [testBoolean]",
            NoSuchElementException::class.java,
        ) {
            appFunctionData.getBoolean("testBoolean")
        }
        Assert.assertThrows(
            "No elements found under [testString]",
            NoSuchElementException::class.java,
        ) {
            appFunctionData.getString("testString")
        }
        Assert.assertThrows(
            "No elements found under [testData]",
            NoSuchElementException::class.java,
        ) {
            appFunctionData.getAppFunctionData("testData")
        }
    }

    @Test
    fun getSingleProperty_shouldReturnDefaultValue_ifNotExist() {
        val appFunctionData = AppFunctionData.EMPTY

        val longProperty = appFunctionData.getLong("testLong", 2L)
        val doubleProperty = appFunctionData.getDouble("testDouble", 4.0)
        val booleanProperty = appFunctionData.getBoolean("testBoolean", true)
        val stringProperty = appFunctionData.getString("testString", "test")

        Truth.assertThat(longProperty).isEqualTo(2L)
        Truth.assertThat(doubleProperty).isEqualTo(4.0)
        Truth.assertThat(booleanProperty).isEqualTo(true)
        Truth.assertThat(stringProperty).isEqualTo("test")
    }

    @Test
    fun getSingleProperty_shouldReturnResult_ifExist() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLong("testLong", 2L)
                .setDouble("testDouble", 4.0)
                .setBoolean("testBoolean", true)
                .setString("testString", "test")
                .setAppFunctionData(
                    "testData",
                    AppFunctionData.Builder("type").setLong("nestedInt", 10).build(),
                )
                .build()

        val longProperty = appFunctionData.getLong("testLong")
        val doubleProperty = appFunctionData.getDouble("testDouble")
        val booleanProperty = appFunctionData.getBoolean("testBoolean")
        val stringProperty = appFunctionData.getString("testString")
        val dataProperty = appFunctionData.getAppFunctionData("testData")

        Truth.assertThat(longProperty).isEqualTo(2L)
        Truth.assertThat(doubleProperty).isEqualTo(4.0)
        Truth.assertThat(booleanProperty).isEqualTo(true)
        Truth.assertThat(stringProperty).isEqualTo("test")
        Truth.assertThat(dataProperty.qualifiedName).isEqualTo("type")
        Truth.assertThat(dataProperty.genericDocument.schemaType).isEqualTo("type")
        Truth.assertThat(dataProperty.getLong("nestedInt")).isEqualTo(10)
    }

    @Test
    fun getSingleProperty_shouldThrowException_ifContainsMoreThanOneProperty() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLongArray("testLong", longArrayOf(3, 4))
                .setDoubleArray("testDouble", doubleArrayOf(7.0, 8.0))
                .setBooleanArray("testBoolean", booleanArrayOf(false, true))
                .setStringList("testString", listOf("test1", "test2"))
                .setAppFunctionDataList(
                    "testData",
                    listOf(AppFunctionData.EMPTY, AppFunctionData.EMPTY),
                )
                .build()

        Assert.assertThrows(
            "Property under [testLong] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getLong("testLong")
        }
        Assert.assertThrows(
            "Property under [testDouble] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getDouble("testDouble")
        }
        Assert.assertThrows(
            "Property under [testBoolean] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getBoolean("testBoolean")
        }
        Assert.assertThrows(
            "Property under [testString] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getString("testString")
        }
        Assert.assertThrows(
            "Property under [testData] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getAppFunctionData("testData")
        }
    }

    @Test
    fun getSingleProperty_shouldThrowException_ifTypeMismatched() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLongArray("testLong", longArrayOf(3, 4))
                .setDoubleArray("testDouble", doubleArrayOf(7.0, 8.0))
                .setBooleanArray("testBoolean", booleanArrayOf(false, true))
                .setStringList("testString", listOf("test1", "test2"))
                .setAppFunctionDataList(
                    "testData",
                    listOf(AppFunctionData.EMPTY, AppFunctionData.EMPTY),
                )
                .build()

        Assert.assertThrows(
            "Found the property under [testLong] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getDouble("testLong")
        }
        Assert.assertThrows(
            "Found the property under [testDouble] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getLong("testDouble")
        }
        Assert.assertThrows(
            "Found the property under [testBoolean] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getString("testBoolean")
        }
        Assert.assertThrows(
            "Found the property under [testString] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getBoolean("testString")
        }
        Assert.assertThrows(
            "Found the property under [testData] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getAppFunctionData("testData")
        }
    }

    @Test
    fun getResultSingleProperty_shouldReturnResult_ifExist() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setAppFunctionData(
                    PROPERTY_RETURN_VALUE,
                    AppFunctionData.Builder("type")
                        .setLong("nestedLong", 20L)
                        .setString("nestedString", "testString")
                        .build(),
                )
                .build()

        val resultProperty = appFunctionData.getAppFunctionData(PROPERTY_RETURN_VALUE)

        Truth.assertThat(resultProperty.getLong("nestedLong")).isEqualTo(20L)
        Truth.assertThat(resultProperty.getString("nestedString")).isEqualTo("testString")
    }

    @Test
    fun getResultSingleProperty_shouldThrowException_ifContainsMoreThanOneProperty() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setStringList(PROPERTY_RETURN_VALUE, listOf("string1", "string2"))
                .build()

        Assert.assertThrows(
            "Property under [$PROPERTY_RETURN_VALUE] does not match request",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getString(PROPERTY_RETURN_VALUE)
        }
    }

    @Test
    fun getProperties_shouldReturnNull_ifNotSet() {
        val appFunctionData = AppFunctionData.EMPTY

        val longProperty = appFunctionData.getLongArray("testLongArray")
        val doubleProperty = appFunctionData.getDoubleArray("testDoubleArray")
        val booleanProperty = appFunctionData.getBooleanArray("testBooleanArray")
        val stringProperty = appFunctionData.getStringList("testStringArray")
        val dataProperty = appFunctionData.getAppFunctionDataList("testDataList")

        Truth.assertThat(longProperty).isNull()
        Truth.assertThat(doubleProperty).isNull()
        Truth.assertThat(booleanProperty).isNull()
        Truth.assertThat(stringProperty).isNull()
        Truth.assertThat(dataProperty).isNull()
    }

    @Test
    fun getProperties_shouldReturnEmpty_ifSetEmpty() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLongArray("testLongArray", longArrayOf())
                .setDoubleArray("testDoubleArray", doubleArrayOf())
                .setBooleanArray("testBooleanArray", booleanArrayOf())
                .setStringList("testStringArray", listOf())
                .setAppFunctionDataList("testDataList", listOf())
                .build()

        val longProperty = appFunctionData.getLongArray("testLongArray")
        val doubleProperty = appFunctionData.getDoubleArray("testDoubleArray")
        val booleanProperty = appFunctionData.getBooleanArray("testBooleanArray")
        val stringProperty = appFunctionData.getStringList("testStringArray")
        val dataProperty = appFunctionData.getAppFunctionDataList("testDataList")

        Truth.assertThat(longProperty).isEmpty()
        Truth.assertThat(doubleProperty).isEmpty()
        Truth.assertThat(booleanProperty).isEmpty()
        Truth.assertThat(stringProperty).isEmpty()
        Truth.assertThat(dataProperty).isEmpty()
    }

    @Test
    fun getProperties_shouldThrowException_ifTypeMismatched() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLongArray("testLongArray", longArrayOf(4L, 5L, 6L))
                .setDoubleArray("testDoubleArray", doubleArrayOf(10.0, 11.0, 12.0))
                .setBooleanArray("testBooleanArray", booleanArrayOf(true, false, true))
                .setStringList("testStringArray", listOf("test1", "test2", "test3"))
                .setAppFunctionDataList(
                    "testDataList",
                    listOf(
                        AppFunctionData.Builder("type").setLong("nestedIntArray", 10).build(),
                        AppFunctionData.Builder("type").setLong("nestedIntArray", 20).build(),
                    ),
                )
                .build()

        Assert.assertThrows(
            "Found the property under [testLongArray] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getDoubleArray("testLongArray")
        }
        Assert.assertThrows(
            "Found the property under [testDoubleArray] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getLongArray("testDoubleArray")
        }
        Assert.assertThrows(
            "Found the property under [testBooleanArray] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getStringList("testBooleanArray")
        }
        Assert.assertThrows(
            "Found the property under [testStringArray] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getBooleanArray("testStringArray")
        }
        Assert.assertThrows(
            "Found the property under [testStringArray] but the data type does not match with the request.",
            IllegalArgumentException::class.java,
        ) {
            appFunctionData.getAppFunctionDataList("testStringArray")
        }
    }

    @Test
    fun getProperties_shouldReturnResult_ifExist() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setLongArray("testLongArray", longArrayOf(4L, 5L, 6L))
                .setDoubleArray("testDoubleArray", doubleArrayOf(10.0, 11.0, 12.0))
                .setBooleanArray("testBooleanArray", booleanArrayOf(true, false, true))
                .setStringList("testStringArray", listOf("test1", "test2", "test3"))
                .setAppFunctionDataList(
                    "testDataList",
                    listOf(
                        AppFunctionData.Builder("type").setLong("nestedInt", 10).build(),
                        AppFunctionData.Builder("type").setLong("nestedInt", 20).build(),
                    ),
                )
                .build()

        val longArray = appFunctionData.getLongArray("testLongArray")
        val doubleArray = appFunctionData.getDoubleArray("testDoubleArray")
        val booleanArray = appFunctionData.getBooleanArray("testBooleanArray")
        val stringArray = appFunctionData.getStringList("testStringArray")
        val dataList = appFunctionData.getAppFunctionDataList("testDataList")

        Truth.assertThat(longArray!!.map { it }).containsExactly(4L, 5L, 6L)
        Truth.assertThat(doubleArray!!.map { it }).containsExactly(10.0, 11.0, 12.0)
        Truth.assertThat(booleanArray!!.map { it }).containsExactly(true, false, true)
        Truth.assertThat(stringArray!!.map { it }).containsExactly("test1", "test2", "test3")
        Truth.assertThat(dataList!![0].getLong("nestedInt")).isEqualTo(10)
        Truth.assertThat(dataList[1].getLong("nestedInt")).isEqualTo(20)
    }

    @Test
    fun getResultProperties_shouldReturnEmpty_ifEmpty() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setDoubleArray(PROPERTY_RETURN_VALUE, doubleArrayOf())
                .build()

        val doubleArray = appFunctionData.getDoubleArray(PROPERTY_RETURN_VALUE)

        Truth.assertThat(doubleArray).isEmpty()
    }

    @Test
    fun getResultProperties_shouldReturnResult_ifExist() {
        val appFunctionData =
            AppFunctionData.Builder("type")
                .setAppFunctionDataList(
                    PROPERTY_RETURN_VALUE,
                    listOf(
                        AppFunctionData.Builder("type").setDouble("nestedDouble", 25.0).build(),
                        AppFunctionData.Builder("type")
                            .setString("nestedString", "testString")
                            .build(),
                    ),
                )
                .build()

        val resultProperties = appFunctionData.getAppFunctionDataList(PROPERTY_RETURN_VALUE)

        Truth.assertThat(resultProperties!![0].getDouble("nestedDouble")).isEqualTo(25.0)
        Truth.assertThat(resultProperties[1].getString("nestedString")).isEqualTo("testString")
    }
}
