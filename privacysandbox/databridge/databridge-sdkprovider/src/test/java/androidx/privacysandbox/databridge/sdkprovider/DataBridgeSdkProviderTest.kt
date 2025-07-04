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

package androidx.privacysandbox.databridge.sdkprovider

import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import androidx.privacysandbox.databridge.sdkprovider.util.FakeDataBridgeProxy
import com.google.common.truth.Expect
import java.lang.ClassCastException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeSdkProviderTest {

    private val dataBridgeSdkProviderWithSuccessResult =
        DataBridgeSdkProvider.getInstance(
            FakeDataBridgeProxy(
                resultInternals =
                    listOf(
                        ResultInternal(
                            keyName = "intKey",
                            valueInternal =
                                ValueInternal(value = 1, type = "INT", isValueNull = false),
                            exceptionName = null,
                            exceptionMessage = null,
                        )
                    )
            )
        )

    private val dataBridgeSdkProviderWithFailureResult =
        DataBridgeSdkProvider.getInstance(
            FakeDataBridgeProxy(
                shouldThrowException = true,
                resultInternals =
                    listOf(
                        ResultInternal(
                            keyName = "intKey",
                            valueInternal = null,
                            exceptionName = ClassCastException::class.java.canonicalName,
                            exceptionMessage = "Cannot convert String to Int",
                        )
                    ),
                exceptionName = ClassCastException::class.java.canonicalName,
                exceptionMessage = "Cannot convert String to Int",
            )
        )

    @Rule @JvmField val expect = Expect.create()

    @Test
    fun testGetValues_success() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        val result = dataBridgeSdkProviderWithSuccessResult.getValues(setOf(intKey))

        expect.that(result.size).isEqualTo(1)
        expect.that(result[intKey]?.isSuccess).isTrue()
        expect.that(result[intKey]?.getOrNull()).isEqualTo(1)
    }

    @Test
    fun testGetValues_failure() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        val result = dataBridgeSdkProviderWithFailureResult.getValues(setOf(intKey))

        expect.that(result.size).isEqualTo(1)

        expect.that(result[intKey]?.isFailure).isTrue()
        val throwable = result[intKey]?.exceptionOrNull()

        expect.that(throwable is ClassCastException).isTrue()
        expect.that(throwable?.message).contains("Cannot convert String to Int")
    }

    @Test
    fun testSetValues_success() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        dataBridgeSdkProviderWithSuccessResult.setValues(mapOf(intKey to 1))
    }

    @Test
    fun testSetValues_failure() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        val thrown =
            assertThrows(ClassCastException::class.java) {
                runBlocking { dataBridgeSdkProviderWithFailureResult.setValues(mapOf(intKey to 1)) }
            }
        expect.that(thrown.message).contains("Cannot convert String to Int")
    }

    @Test
    fun testRemoveValues_success() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        dataBridgeSdkProviderWithSuccessResult.removeValues(setOf(intKey))
    }

    @Test
    fun testRemoveValues_failure() = runBlocking {
        val intKey = Key.createIntKey("intKey")
        val thrown =
            assertThrows(ClassCastException::class.java) {
                runBlocking { dataBridgeSdkProviderWithFailureResult.removeValues(setOf(intKey)) }
            }
        expect.that(thrown.message).contains("Cannot convert String to Int")
    }
}
