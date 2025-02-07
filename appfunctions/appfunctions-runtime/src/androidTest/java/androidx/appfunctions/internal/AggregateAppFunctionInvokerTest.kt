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

package androidx.appfunctions.internal

import android.content.Context
import android.content.pm.SigningInfo
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class AggregateAppFunctionInvokerTest {

    @Test
    fun testEmptyAggregateInvoker() {
        val aggregateInvoker =
            object : AggregateAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> = emptyList()
            }

        assertThat(aggregateInvoker.supportedFunctionIds).isEmpty()
        Assert.assertThrows(AppFunctionFunctionNotFoundException::class.java) {
            runBlocking {
                aggregateInvoker.unsafeInvoke(
                    FakeAppFunctionContext,
                    "androidx.apfunctions.internal#test1",
                    mapOf()
                )
            }
        }
    }

    @Test
    fun testAggregateInvoker_nonExistFunction() {
        val aggregateInvoker =
            object : AggregateAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> =
                    listOf(
                        Invoker1(),
                        Invoker2(),
                    )
            }

        assertThat(aggregateInvoker.supportedFunctionIds).hasSize(2)
        assertThat(aggregateInvoker.supportedFunctionIds)
            .containsNoneIn(listOf("androidx.appfunctions.internal#test0"))
        Assert.assertThrows(AppFunctionFunctionNotFoundException::class.java) {
            runBlocking {
                aggregateInvoker.unsafeInvoke(
                    FakeAppFunctionContext,
                    "androidx.apfunctions.internal#test0",
                    mapOf()
                )
            }
        }
    }

    @Test
    fun testAggregateInvoker_existFunctions() {
        val aggregateInvoker =
            object : AggregateAppFunctionInvoker() {
                override val invokers: List<AppFunctionInvoker> =
                    listOf(
                        Invoker1(),
                        Invoker2(),
                    )
            }

        val invokeTest1Result = runBlocking {
            aggregateInvoker.unsafeInvoke(
                FakeAppFunctionContext,
                "androidx.appfunctions.internal#test1",
                mapOf()
            )
        }
        val invokeTest2Result = runBlocking {
            aggregateInvoker.unsafeInvoke(
                FakeAppFunctionContext,
                "androidx.appfunctions.internal#test2",
                mapOf()
            )
        }

        assertThat(aggregateInvoker.supportedFunctionIds)
            .containsExactly(
                "androidx.appfunctions.internal#test1",
                "androidx.appfunctions.internal#test2"
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
            parameters: Map<String, Any?>
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
            parameters: Map<String, Any?>
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

        override val callingPackageName: String
            get() = throw RuntimeException("Stub!")

        override val callingPackageSigningInfo: SigningInfo
            get() = throw RuntimeException("Stub!")
    }
}
