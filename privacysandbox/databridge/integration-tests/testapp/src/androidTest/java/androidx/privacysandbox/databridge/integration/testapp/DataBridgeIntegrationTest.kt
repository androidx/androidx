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

package androidx.privacysandbox.databridge.integration.testapp

import androidx.privacysandbox.databridge.core.Key
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Expect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DataBridgeIntegrationTest {

    private val intKey = Key.createIntKey("intKey")
    private val doubleKey = Key.createDoubleKey("doubleKey")
    private val longKey = Key.createLongKey("longKey")
    private val floatKey = Key.createFloatKey("floatKey")
    private val booleanKey = Key.createBooleanKey("booleanKey")
    private val stringKey = Key.createStringKey("stringKey")
    private val stringSetKey = Key.createStringSetKey("stringSetKey")
    private val byteArrayKey = Key.createByteArrayKey("byteArrayKey")

    private val keyValueMap =
        mapOf(
            intKey to 1,
            doubleKey to 1.1,
            longKey to 1L,
            floatKey to 1.1f,
            booleanKey to true,
            stringKey to "stringValue",
            stringSetKey to setOf("stringValue1", "stringValue2"),
            byteArrayKey to byteArrayOf(1, 2, 3, 4),
        )

    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule @JvmField val expect = Expect.create()

    @Before
    fun setUp() {
        activityScenarioRule.withActivity { runBlocking { testAppApi.loadTestSdk() } }
    }

    @After
    fun tearDown() {
        activityScenarioRule.withActivity {
            runBlocking { testAppApi.removeValuesFromApp(keyValueMap.keys) }
            testAppApi.unloadTestSdk()
        }
    }

    @Test
    fun testGetValueFromApp_nullValue() {
        activityScenarioRule.withActivity {
            runBlocking {
                val result = testAppApi.getValuesFromApp(setOf(intKey, stringKey))

                expect.that(result.size).isEqualTo(2)
                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)
            }
        }
    }

    @Test
    fun testGetValuesFromSdk_nullValue() {
        activityScenarioRule.withActivity {
            runBlocking {
                val result = testAppApi.getValuesFromSdk(setOf(intKey, stringKey))

                expect.that(result.size).isEqualTo(2)
                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)
            }
        }
    }

    @Test
    fun testGetValuesFromApp_wrongType() {
        activityScenarioRule.withActivity {
            runBlocking {
                val tempIntKey = Key.createIntKey("tempKey")
                testAppApi.setValuesFromApp(mapOf(intKey to 1, tempIntKey to 10))

                val tempStringKey = Key.createStringKey("tempKey")
                val keyValueMapResult = testAppApi.getValuesFromApp(setOf(intKey, tempStringKey))

                verifySuccessfulResult(keyValueMapResult[intKey]!!, keyValueMap[intKey])
                verifyClassCastExceptionFailureResult(keyValueMapResult[tempStringKey]!!)
            }
        }
    }

    @Test
    fun testGetValuesFromSdk_wrongType() {
        activityScenarioRule.withActivity {
            runBlocking {
                val tempIntKey = Key.createIntKey("tempKey")
                testAppApi.setValuesFromSdk(mapOf(intKey to 1, tempIntKey to 10))

                val tempStringKey = Key.createStringKey("tempKey")
                val keyValueMapResult = testAppApi.getValuesFromSdk(setOf(intKey, tempStringKey))

                verifySuccessfulResult(keyValueMapResult[intKey]!!, keyValueMap[intKey])
                verifyClassCastExceptionFailureResult(keyValueMapResult[tempStringKey]!!)
            }
        }
    }

    @Test
    fun testSetValuesAndGetValuesFromApp() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromApp(keyValueMap)

                val result = testAppApi.getValuesFromApp(keyValueMap.keys)

                keyValueMap.keys.forEach { key ->
                    verifySuccessfulResult(result[key]!!, keyValueMap[key])
                }
            }
        }
    }

    @Test
    fun testSetValuesAndGetValuesFromSdk() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromSdk(keyValueMap)

                val result = testAppApi.getValuesFromSdk(keyValueMap.keys)

                keyValueMap.keys.forEach { key ->
                    verifySuccessfulResult(result[key]!!, keyValueMap[key])
                }
            }
        }
    }

    @Test
    fun testSetValuesFromAppAndGetValuesFromSdk() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromApp(keyValueMap)

                val result = testAppApi.getValuesFromSdk(keyValueMap.keys)

                keyValueMap.keys.forEach { key ->
                    verifySuccessfulResult(result[key]!!, keyValueMap[key])
                }
            }
        }
    }

    @Test
    fun testSetValuesFromSdkAndGetValuesFromApp() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromSdk(keyValueMap)

                val res = testAppApi.getValuesFromApp(keyValueMap.keys)

                keyValueMap.keys.forEach { key ->
                    verifySuccessfulResult(res[key]!!, keyValueMap[key])
                }
            }
        }
    }

    @Test
    fun testRemoveValuesFromApp() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromApp(
                    mapOf(intKey to keyValueMap[intKey], stringKey to keyValueMap[stringKey])
                )
                var result = testAppApi.getValuesFromApp(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, keyValueMap[intKey])
                verifySuccessfulResult(result[stringKey]!!, keyValueMap[stringKey])

                testAppApi.removeValuesFromApp(setOf(intKey, stringKey))

                result = testAppApi.getValuesFromApp(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)
            }
        }
    }

    @Test
    fun testRemoveValuesFromSdk() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromSdk(
                    mapOf(intKey to keyValueMap[intKey], stringKey to keyValueMap[stringKey])
                )
                var result = testAppApi.getValuesFromSdk(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, keyValueMap[intKey])
                verifySuccessfulResult(result[stringKey]!!, keyValueMap[stringKey])

                testAppApi.removeValuesFromSdk(setOf(intKey, stringKey))

                result = testAppApi.getValuesFromSdk(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)

                // Verify that getValues from App also returns null
                result = testAppApi.getValuesFromApp(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)
            }
        }
    }

    @Test
    fun testRemoveValuesFromApp_fetchFromSdk_returnsNullValue() {
        activityScenarioRule.withActivity {
            runBlocking {
                testAppApi.setValuesFromSdk(
                    mapOf(intKey to keyValueMap[intKey], stringKey to keyValueMap[stringKey])
                )
                var result = testAppApi.getValuesFromSdk(setOf(intKey, stringKey))
                verifySuccessfulResult(result[intKey]!!, keyValueMap[intKey])
                verifySuccessfulResult(result[stringKey]!!, keyValueMap[stringKey])

                testAppApi.removeValuesFromApp(setOf(intKey, stringKey))
                result = testAppApi.getValuesFromSdk(setOf(intKey, stringKey))

                verifySuccessfulResult(result[intKey]!!, null)
                verifySuccessfulResult(result[stringKey]!!, null)
            }
        }
    }

    @Test
    fun testSetValuesFromApp_andGetValues_sameKeyWithDifferentTypeUpdateInSingleCall() {
        activityScenarioRule.withActivity {
            runBlocking {
                val tempStringKey = Key.createStringKey("intKey")
                assertThrows(ClassCastException::class.java) {
                    runBlocking {
                        // This will try to override an int key "intKey" to the string value
                        // "tempString" which will result in a ClassCastException.
                        testAppApi.setValuesFromApp(
                            mapOf(intKey to 1, tempStringKey to "tempString")
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testSetValuesFromSdk_andGetValues_sameKeyWithDifferentTypeUpdateInSingleCall() {
        activityScenarioRule.withActivity {
            runBlocking {
                val tempStringKey = Key.createStringKey("intKey")
                assertThrows(ClassCastException::class.java) {
                    runBlocking {
                        // This will try to override an int key "intKey" to the string value
                        // "tempString" which will result in a ClassCastException.
                        testAppApi.setValuesFromSdk(
                            mapOf(intKey to 1, tempStringKey to "tempString")
                        )
                    }
                }
            }
        }
    }

    private fun verifySuccessfulResult(result: Result<Any?>, expectedVal: Any?) {
        expect.that(result.isSuccess).isTrue()
        expect.that(result.getOrNull()).isEqualTo(expectedVal)
    }

    private fun verifyClassCastExceptionFailureResult(result: Result<Any?>) {
        expect.that(result.isFailure).isTrue()
        expect.that(result.exceptionOrNull() is ClassCastException).isTrue()
    }
}
