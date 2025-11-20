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

package androidx.appfunctions.service.internal

import android.content.Context
import android.os.Build
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AggregatedAppFunctionInvokerTest {

    @Test
    fun testEmptyAggregatedInvoker() {
        val aggregatedInvoker =
            object : AggregatedAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> = emptyList()
            }

        assertThat(aggregatedInvoker.supportedFunctionIds).isEmpty()
        Assert.assertThrows(AppFunctionFunctionNotFoundException::class.java) {
            runBlocking {
                aggregatedInvoker.unsafeInvoke(
                    FakeAppFunctionContext,
                    "androidx.apfunctions.internal#test1",
                    mapOf(),
                )
            }
        }
    }

    @Test
    fun testAggregatedInvoker_nonExistFunction() {
        val aggregatedInvoker =
            object : AggregatedAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> = listOf(Invoker1(), Invoker2())
            }

        assertThat(aggregatedInvoker.supportedFunctionIds).hasSize(2)
        assertThat(aggregatedInvoker.supportedFunctionIds)
            .containsNoneIn(listOf("androidx.appfunctions.internal#test0"))
        Assert.assertThrows(AppFunctionFunctionNotFoundException::class.java) {
            runBlocking {
                aggregatedInvoker.unsafeInvoke(
                    FakeAppFunctionContext,
                    "androidx.apfunctions.internal#test0",
                    mapOf(),
                )
            }
        }
    }

    @Test
    fun testAggregatedInvoker_existFunctions() {
        val aggregatedInvoker =
            object : AggregatedAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> = listOf(Invoker1(), Invoker2())
            }

        val invokeTest1Result = runBlocking {
            aggregatedInvoker.unsafeInvoke(
                FakeAppFunctionContext,
                "androidx.appfunctions.internal#test1",
                mapOf(),
            )
        }
        val invokeTest2Result = runBlocking {
            aggregatedInvoker.unsafeInvoke(
                FakeAppFunctionContext,
                "androidx.appfunctions.internal#test2",
                mapOf(),
            )
        }

        assertThat(aggregatedInvoker.supportedFunctionIds)
            .containsExactly(
                "androidx.appfunctions.internal#test1",
                "androidx.appfunctions.internal#test2",
            )
        assertThat(invokeTest1Result).isEqualTo("Invoker1#test1")
        assertThat(invokeTest2Result).isEqualTo("Invoker2#test2")
    }

    private class Invoker1 : AppFunctionInvoker {
        override val supportedFunctionIds: Set<String> =
            setOf("androidx.appfunctions.internal#test1")

        override suspend fun unsafeInvoke(
            appFunctionContext: AppFunctionContext,
            functionIdentifier: String,
            parameters: Map<String, Any?>,
        ): Any? {
            return when (functionIdentifier) {
                "androidx.appfunctions.internal#test1" -> "Invoker1#test1"
                else -> throw IllegalArgumentException()
            }
        }
    }

    private class Invoker2 : AppFunctionInvoker {
        override val supportedFunctionIds: Set<String> =
            setOf("androidx.appfunctions.internal#test2")

        override suspend fun unsafeInvoke(
            appFunctionContext: AppFunctionContext,
            functionIdentifier: String,
            parameters: Map<String, Any?>,
        ): Any? {
            return when (functionIdentifier) {
                "androidx.appfunctions.internal#test2" -> "Invoker2#test2"
                else -> throw IllegalArgumentException()
            }
        }
    }

    private object FakeAppFunctionContext : AppFunctionContext {
        override val context: Context
            get() = throw RuntimeException("Stub!")
    }
}
