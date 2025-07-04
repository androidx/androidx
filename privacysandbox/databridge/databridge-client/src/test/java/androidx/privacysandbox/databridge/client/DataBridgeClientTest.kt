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
import androidx.privacysandbox.databridge.client.util.KeyValueUtil
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.integration.testutils.KeyUpdateCallbackImpl
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Expect
import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val intKey = Key.createIntKey("intKey")
private val doubleKey = Key.createDoubleKey("doubleKey")
private val longKey = Key.createLongKey("longKey")
private val floatKey = Key.createFloatKey("floatKey")
private val booleanKey = Key.createBooleanKey("booleanKey")
private val stringKey = Key.createStringKey("stringKey")
private val stringSetKey = Key.createStringSetKey("stringSetKey")
private val byteArrayKey = Key.createByteArrayKey("byteArrayKey")

private val setOfKeys =
    setOf(intKey, doubleKey, longKey, floatKey, booleanKey, stringKey, stringSetKey, byteArrayKey)

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientGetUnsetKeyReturnsNullParameterizedTest(private val key: Key) {
    private var dataBridgeClient =
        DataBridgeClient.getInstance(ApplicationProvider.getApplicationContext())

    @Test
    fun testGetValueForUnsetValueReturnsNull() = runBlocking {
        val result = dataBridgeClient.getValue(key)
        KeyValueUtil.assertKeyIsMissing(result)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: key={0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(intKey),
                arrayOf(doubleKey),
                arrayOf(longKey),
                arrayOf(floatKey),
                arrayOf(booleanKey),
                arrayOf(stringKey),
                arrayOf(stringSetKey),
                arrayOf(byteArrayKey),
            )
        }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientSetAndGetValueParameterizedTest(
    private val key: Key,
    private val value: Any,
) {
    private val dataBridgeClient =
        DataBridgeClient.getInstance(ApplicationProvider.getApplicationContext())

    @After fun tearDown() = runBlocking { dataBridgeClient.removeValue(key) }

    @Test
    fun testSetValueAndGetValueReturnsSetValue() = runBlocking {
        dataBridgeClient.setValue(key, value)
        val result = dataBridgeClient.getValue(key)
        KeyValueUtil.assertKeySetSuccessfully(value, result)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: key={0}, value={1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(intKey, 1),
                arrayOf(doubleKey, 1.1),
                arrayOf(longKey, 1L),
                arrayOf(floatKey, 1.1f),
                arrayOf(booleanKey, true),
                arrayOf(stringKey, "stringValue"),
                arrayOf(stringSetKey, setOf("stringValue1", "stringValue2")),
                arrayOf(byteArrayKey, byteArrayOf(1, 2, 3, 4)),
            )
        }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientOverrideWithDifferentTypeParameterizedTest(
    private val key: Key,
    private val originalValue: Any,
    private val overwriteKey: Key,
    private val overwriteValue: Any,
) {
    private val dataBridgeClient =
        DataBridgeClient.getInstance(ApplicationProvider.getApplicationContext())

    @After fun tearDown() = runBlocking { dataBridgeClient.removeValues(setOf(key, overwriteKey)) }

    @Test
    fun testSetValueCanBeOverwrittenByDifferentType() = runBlocking {
        dataBridgeClient.setValue(key, originalValue)

        dataBridgeClient.setValue(overwriteKey, overwriteValue)
        val result = dataBridgeClient.getValue(overwriteKey)
        KeyValueUtil.assertKeySetSuccessfully(overwriteValue, result)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "{index}: key={0}, value={1}, overwrittenKey={2}, overwrittenValue={3}"
        )
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(intKey, 1, Key.createStringKey(intKey.name), "stringValue"),
                arrayOf(doubleKey, 1.1, Key.createStringKey(doubleKey.name), "stringValue"),
                arrayOf(longKey, 1L, Key.createStringKey(longKey.name), "stringValue"),
                arrayOf(floatKey, 1.1f, Key.createStringKey(floatKey.name), "stringValue"),
                arrayOf(booleanKey, true, Key.createStringKey(booleanKey.name), "stringValue"),
                arrayOf(stringKey, "stringValue", Key.createBooleanKey(stringKey.name), true),
                arrayOf(
                    stringSetKey,
                    setOf("stringValue1", "stringValue2"),
                    Key.createBooleanKey(stringSetKey.name),
                    true,
                ),
                arrayOf(
                    byteArrayKey,
                    byteArrayOf(1, 2, 3, 4),
                    Key.createBooleanKey(byteArrayKey.name),
                    true,
                ),
            )
        }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientGetWrongTypeAfterSetParameterizedTest(
    private val key: Key,
    private val wrongTypeKey: Key,
    private val value: Any,
) {
    private val dataBridgeClient =
        DataBridgeClient.getInstance(ApplicationProvider.getApplicationContext())

    @After fun tearDown() = runBlocking { dataBridgeClient.removeValues(setOf(key, wrongTypeKey)) }

    @Test
    fun testGetValueForWrongTypeAfterValueSetReturnsFailureWithClassCastException() = runBlocking {
        dataBridgeClient.setValue(key, value)
        val result = dataBridgeClient.getValue(wrongTypeKey)
        KeyValueUtil.assertGetValueThrowsClassCastException(result)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "{index}: key={0}, wrongTypeKey={1}, value={2}"
        )
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(intKey, Key.createStringKey(intKey.name), 1),
                arrayOf(doubleKey, Key.createStringKey(doubleKey.name), 1.1),
                arrayOf(longKey, Key.createStringKey(longKey.name), 1L),
                arrayOf(floatKey, Key.createStringKey(floatKey.name), 1.1f),
                arrayOf(booleanKey, Key.createStringKey(booleanKey.name), true),
                arrayOf(stringKey, Key.createBooleanKey(stringKey.name), "stringValue"),
                arrayOf(
                    stringSetKey,
                    Key.createBooleanKey(stringSetKey.name),
                    setOf("stringValue1", "stringValue2"),
                ),
                arrayOf(
                    byteArrayKey,
                    Key.createBooleanKey(byteArrayKey.name),
                    byteArrayOf(1, 2, 3, 4),
                ),
            )
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dataBridgeClient = DataBridgeClient.getInstance(context)
    @Rule @JvmField val expect = Expect.create()

    private val currentThreadExecutor = Executor { command -> command.run() }

    @After
    fun tearDown() {
        runBlocking { dataBridgeClient.removeValues(setOfKeys) }
        DataBridgeClient.resetInstanceForTesting()
    }

    @Test
    fun testDatBridgeProxyRegisteredToAppOwnedSdkSandboxInterfaceCompat() {
        val sdkSandboxManagerCompat = SdkSandboxManagerCompat.from(context)
        val appOwnedSdkSandboxInterfaces = sdkSandboxManagerCompat.getAppOwnedSdkSandboxInterfaces()
        expect.that(appOwnedSdkSandboxInterfaces.size).isEqualTo(1)
        expect
            .that(appOwnedSdkSandboxInterfaces[0].getName())
            .isEqualTo("androidx.privacysandbox.databridge")
        expect.that(appOwnedSdkSandboxInterfaces[0].getVersion()).isEqualTo(1)
    }

    @Test
    fun testGetInstanceSingleton() {
        expect.that(dataBridgeClient).isNotNull()
        val dataBridgeClient2 = DataBridgeClient.getInstance(context)

        assertSame(
            "Both references should point to the same singleton instance",
            dataBridgeClient,
            dataBridgeClient2,
        )
    }

    @Test
    fun testRemoveValue() = runBlocking {
        dataBridgeClient.setValue(intKey, 1)
        var result = dataBridgeClient.getValue(intKey)
        KeyValueUtil.assertKeySetSuccessfully(1, result)

        dataBridgeClient.removeValue(intKey)
        result = dataBridgeClient.getValue(intKey)
        KeyValueUtil.assertKeyIsMissing(result)
    }

    @Test
    fun testRemoveValues() = runBlocking {
        var result = setAndGetValue(intKey, 1)
        KeyValueUtil.assertKeySetSuccessfully(1, result)

        result = setAndGetValue(booleanKey, true)
        KeyValueUtil.assertKeySetSuccessfully(true, result)

        dataBridgeClient.removeValues(setOf(intKey, booleanKey))

        KeyValueUtil.assertKeyIsMissing(dataBridgeClient.getValue(intKey))
        KeyValueUtil.assertKeyIsMissing(dataBridgeClient.getValue(booleanKey))
    }

    @Test
    fun testGetValues_NullValues() = runBlocking {
        val keyValueMap = dataBridgeClient.getValues(setOfKeys)

        setOfKeys.forEach {
            expect.that(keyValueMap[it]?.isSuccess).isTrue()
            expect.that(keyValueMap[it]?.getOrNull()).isNull()
        }
    }

    @Test
    fun testGetValues() = runBlocking {
        var result = setAndGetValue(intKey, 1)
        KeyValueUtil.assertKeySetSuccessfully(1, result)

        result = setAndGetValue(booleanKey, false)
        KeyValueUtil.assertKeySetSuccessfully(false, result)

        result = setAndGetValue(stringKey, "stringValue")
        KeyValueUtil.assertKeySetSuccessfully("stringValue", result)

        val keyValueMap = dataBridgeClient.getValues(setOfKeys)
        expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)
        expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(false)
        expect.that(keyValueMap[stringKey]?.getOrNull()).isEqualTo("stringValue")
        expect.that(keyValueMap[floatKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[doubleKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[longKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[stringSetKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[byteArrayKey]?.getOrNull()).isNull()
    }

    @Test
    fun testSetValues_andGetValues_sameKeyUpdateInSingleCall() {
        runBlocking {
            val tempStringKey = Key.createStringKey("intKey")

            assertThrows(ClassCastException::class.java) {
                runBlocking {
                    // This will try to override an int key "intKey" to the string value
                    // "tempString" which will result in a ClassCastException.
                    dataBridgeClient.setValues(mapOf(intKey to 1, tempStringKey to "tempString"))
                }
            }
        }
    }

    @Test
    fun testSetValues() = runBlocking {
        dataBridgeClient.setValues(
            mapOf(
                intKey to 1,
                longKey to 1L,
                booleanKey to true,
                stringSetKey to setOf("string1", "string2"),
            )
        )

        val keyValueMap = dataBridgeClient.getValues(setOfKeys)
        expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)
        expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(true)
        expect.that(keyValueMap[stringKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[floatKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[doubleKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[longKey]?.getOrNull()).isEqualTo(1L)
        expect.that(keyValueMap[stringSetKey]?.getOrNull()).isEqualTo(setOf("string1", "string2"))
        expect.that(keyValueMap[byteArrayKey]?.getOrNull()).isNull()
    }

    @Test
    fun testSetValues_wrongType() {
        assertThrows(ClassCastException::class.java) {
            runBlocking {
                dataBridgeClient.setValues(
                    mapOf(
                        intKey to "1",
                        longKey to 1L,
                        booleanKey to true,
                        stringSetKey to setOf("string1", "string2"),
                    )
                )
            }
        }

        // Verify that the action is atomic and none of the keys are set
        val keySet = setOf(intKey, longKey, booleanKey, stringSetKey)
        runBlocking {
            keySet.forEach {
                val result = dataBridgeClient.getValue(it)
                KeyValueUtil.assertKeyIsMissing(result)
            }
        }
    }

    @Test
    fun testGetValues_wrongType() {
        runBlocking {
            val tempIntKey = Key.createIntKey("tempIntKey")
            dataBridgeClient.setValue(tempIntKey, 10)
            dataBridgeClient.setValues(mapOf(intKey to 1, booleanKey to true))

            val tempStringKey = Key.createStringKey("tempIntKey")
            val keyValueMap = dataBridgeClient.getValues(setOf(intKey, booleanKey, tempStringKey))

            expect.that(keyValueMap[intKey]?.isSuccess).isTrue()
            expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)

            expect.that(keyValueMap[booleanKey]?.isSuccess).isTrue()
            expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(true)

            expect.that(keyValueMap[tempStringKey]?.isFailure).isTrue()
            expect
                .that(keyValueMap[tempStringKey]?.exceptionOrNull() is ClassCastException)
                .isTrue()
        }
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_oneKey() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.removeValue(intKey)
        verifyCountAndValue(callback, intKey, 3, null)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_multipleKeys_registeredMultipleTimes() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.removeValue(intKey)
        verifyCountAndValue(callback, intKey, 3, null)

        callback.initializeLatch(listOf(stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(stringKey),
            currentThreadExecutor,
            callback,
        )
        verifyCountAndValue(callback, stringKey, 1, null)

        callback.initializeLatch(listOf(stringKey))
        dataBridgeClient.setValue(stringKey, "stringValue")
        verifyCountAndValue(callback, stringKey, 2, "stringValue")

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_multipleKeys() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(intKey, stringKey),
            currentThreadExecutor,
            callback,
        )
        verifyCountAndValue(callback, intKey, 1, null)
        verifyCountAndValue(callback, stringKey, 1, null)

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.setValues(mapOf(intKey to 123, stringKey to "stringValue"))
        verifyCountAndValue(callback, intKey, 2, 123)
        verifyCountAndValue(callback, stringKey, 2, "stringValue")

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.removeValues(setOf(intKey, stringKey))
        verifyCountAndValue(callback, intKey, 3, null)
        verifyCountAndValue(callback, stringKey, 3, null)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_multipleCallbacks_multipleKeys() = runBlocking {
        val callback1 = KeyUpdateCallbackImpl()
        val callback2 = KeyUpdateCallbackImpl()

        callback1.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(intKey, stringKey),
            currentThreadExecutor,
            callback1,
        )
        verifyCountAndValue(callback1, stringKey, 1, null)

        callback2.initializeLatch(listOf(doubleKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(doubleKey, stringKey),
            currentThreadExecutor,
            callback2,
        )
        verifyCountAndValue(callback2, stringKey, 1, null)

        callback1.initializeLatch(listOf(stringKey))
        callback2.initializeLatch(listOf(stringKey))
        dataBridgeClient.setValue(stringKey, "stringValue")
        verifyCountAndValue(callback1, stringKey, 2, "stringValue")
        verifyCountAndValue(callback2, stringKey, 2, "stringValue")

        dataBridgeClient.unregisterKeyUpdateCallback(callback1)
        dataBridgeClient.unregisterKeyUpdateCallback(callback2)
    }

    @Test(expected = TimeoutException::class)
    fun testUnregisterKeyUpdates() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 11)

        // This throws a TimeoutException exception because it CountDownLatch.awaits returns a
        // boolean as the callback has been unregistered
        val unused = callback.getCounterForKey(intKey)
    }

    private fun verifyCountAndValue(
        callback: KeyUpdateCallbackImpl,
        key: Key,
        count: Int,
        value: Any?,
    ) {
        expect.that(callback.getCounterForKey(key)).isEqualTo(count)
        expect.that(callback.getValueForKey(key)).isEqualTo(value)
    }

    private suspend fun setAndGetValue(key: Key, value: Any?): Result<Any?> {
        dataBridgeClient.setValue(key, value)
        return dataBridgeClient.getValue(key)
    }
}
