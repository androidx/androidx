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

package androidx.privacysandbox.databridge.client

import android.content.Context
import androidx.privacysandbox.databridge.client.util.GetValuesResultCallbackStub
import androidx.privacysandbox.databridge.client.util.RemoveValuesResultCallbackStub
import androidx.privacysandbox.databridge.client.util.SetValuesResultCallbackStub
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import java.lang.ClassCastException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeProxyTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "androidx.privacysandbox.databridge_test"
    private var mDataBridgeClient = DataBridgeClient.getInstance(context, fileName)

    private var mDataBridgeProxy = DataBridgeProxy(mDataBridgeClient)

    private val intKey = Key.createIntKey("intKey")
    private val stringKey = Key.createStringKey("stringKey")
    private val keyNames = listOf(intKey.name, stringKey.name)
    private val keyTypes = listOf("INT", "STRING")
    private val intValue = 123
    private val stringValue = "stringVal"

    @Rule @JvmField val expect = Expect.create()

    @After fun tearDown() = runBlocking { mDataBridgeClient.removeValues(setOf(intKey, stringKey)) }

    @Test
    fun testGetValues_success() = runBlocking {
        mDataBridgeClient.setValues(mapOf(intKey to intValue, stringKey to stringValue))

        val callback = GetValuesResultCallbackStub()

        mDataBridgeProxy.getValues(keyNames, keyTypes, callback)

        expect.that(callback.getResult().size).isEqualTo(2)

        val resultMap = callback.getResult().associateBy { it.keyName }

        resultMap[intKey.name]!!.verifySuccessfulResult(intKey, intValue, isValueNull = false)

        resultMap[stringKey.name]!!.verifySuccessfulResult(
            stringKey,
            stringValue,
            isValueNull = false,
        )
    }

    @Test
    fun testGetValues_failure() = runBlocking {
        mDataBridgeClient.setValue(intKey, intValue)

        val callback = GetValuesResultCallbackStub()

        mDataBridgeProxy.getValues(listOf(intKey.name), listOf("STRING"), callback)

        expect.that(callback.getResult().size).isEqualTo(1)

        val resultMap = callback.getResult().associateBy { it.keyName }

        resultMap[intKey.name]!!.verifyFailureResult(
            intKey,
            expectedExceptionName = ClassCastException::class.java.canonicalName!!,
            expectedExceptionMessage =
                "class java.lang.Integer cannot be cast to class java.lang.String",
        )
    }

    @Test
    fun setValues_success() = runBlocking {
        val valueInternals =
            listOf(
                ValueInternal(keyTypes[0], false, intValue),
                ValueInternal(keyTypes[1], false, stringValue),
            )

        val callback = SetValuesResultCallbackStub()

        mDataBridgeProxy.setValues(keyNames, valueInternals, callback)

        val exceptionData: List<String?> = callback.getException()

        exceptionData.forEach { expect.that(it).isNull() }

        // Verify the result
        val result = mDataBridgeClient.getValues(setOf(intKey, stringKey))

        expect.that(result[intKey]?.isSuccess).isTrue()
        expect.that(result[intKey]?.getOrNull()).isEqualTo(intValue)

        expect.that(result[stringKey]?.isSuccess).isTrue()
        expect.that(result[stringKey]?.getOrNull()).isEqualTo(stringValue)
    }

    @Test
    fun setValues_failure() = runBlocking {
        val inputKeyNames = listOf(stringKey.name)
        val inputValueInternals = listOf(ValueInternal("INT", false, stringValue))

        val callback = SetValuesResultCallbackStub()

        mDataBridgeProxy.setValues(inputKeyNames, inputValueInternals, callback)

        val exceptionData: List<String?> = callback.getException()

        expect
            .that(exceptionData[0])
            .isEqualTo(java.lang.ClassCastException::class.java.canonicalName)
        expect
            .that(exceptionData[1])
            .contains("class java.lang.String cannot be cast to class java.lang.Integer")

        // Verify the result
        val result = mDataBridgeClient.getValue(stringKey)
        expect.that(result.isSuccess).isTrue()
        expect.that(result.getOrNull()).isNull()
    }

    @Test
    fun removeValues_success() = runBlocking {
        val callback = RemoveValuesResultCallbackStub()
        mDataBridgeProxy.removeValues(keyNames, keyTypes, callback)

        val exceptionData = callback.getException()

        exceptionData.forEach { expect.that(it).isNull() }

        // Verify the result
        val result = mDataBridgeClient.getValues(setOf(intKey, stringKey))

        expect.that(result[intKey]?.isSuccess).isTrue()
        expect.that(result[intKey]?.getOrNull()).isNull()

        expect.that(result[stringKey]?.isSuccess).isTrue()
        expect.that(result[stringKey]?.getOrNull()).isNull()
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeProxyParameterizedTest(private val key: Key, private val value: Any?) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "androidx.privacysandbox.databridge_test"
    private var mDataBridgeClient = DataBridgeClient.getInstance(context, fileName)
    private var mDataBridgeProxy = DataBridgeProxy(mDataBridgeClient)

    @Rule @JvmField val expect = Expect.create()

    @Test
    fun testGetValues() = runBlocking {
        val callback = GetValuesResultCallbackStub()

        // Set value for key in DataBridge
        mDataBridgeClient.setValue(key, value)

        mDataBridgeProxy.getValues(listOf(key.name), listOf(key.type.toString()), callback)

        expect.that(callback.getResult().size).isEqualTo(1)

        val resultMap = callback.getResult().associateBy { it.keyName }

        val result = resultMap[key.name]!!

        result.verifySuccessfulResult(key, value, isValueNull = false)

        // Remove value for key
        mDataBridgeClient.removeValue(key)
    }

    // Static method to provide parameters for the tests
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: key={0}, value={1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(Key.createIntKey("intKey"), 1),
                arrayOf(Key.createDoubleKey("longKey"), 1.1),
                arrayOf(Key.createLongKey("doubleKey"), 1L),
                arrayOf(Key.createFloatKey("floatKey"), 1f),
                arrayOf(Key.createBooleanKey("booleanKey"), true),
                arrayOf(Key.createStringKey("stringKey"), "stringValue"),
                arrayOf(
                    Key.createStringSetKey("stringSetKey"),
                    setOf("stringValue1", "stringValue2"),
                ),
                arrayOf(Key.createByteArrayKey("byteArrayKey"), byteArrayOf(1, 2, 3, 4)),
            )
        }
    }
}

private fun ResultInternal.verifySuccessfulResult(key: Key, value: Any?, isValueNull: Boolean) {
    assertThat(keyName).isEqualTo(key.name)
    assertThat(exceptionName).isNull()
    assertThat(exceptionMessage).isNull()
    assertThat(valueInternal!!.type).isEqualTo(key.type.toString())
    assertThat(valueInternal!!.value).isEqualTo(value)
    assertThat(valueInternal!!.isValueNull).isEqualTo(isValueNull)
}

private fun ResultInternal.verifyFailureResult(
    key: Key,
    expectedExceptionName: String,
    expectedExceptionMessage: String,
) {
    assertThat(keyName).isEqualTo(key.name)
    assertThat(exceptionName).isEqualTo(expectedExceptionName)
    assertThat(exceptionMessage).contains(expectedExceptionMessage)
    assertThat(valueInternal).isNull()
}
