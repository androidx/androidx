/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test

class OperationsRaceJUnitTest {
    @Test
    fun testRace() {
        OperationsRaceTest.runTest()
    }
}

object OperationsRaceTest {
    @Throws(Exception::class)
    private fun resetStaticFields() {
        val clazz: Class<*> = Operations::class.java
        val fields =
            arrayOf<String?>(
                "sMapV6",
                "sMapV7",
                "sMapV7AndroidX",
                "sMapV7AndroidXExperimental",
                "sMapV7AndroidXDeprecated",
                "sMapV7Widgets",
                "sMapV7WidgetsExperimental",
                "sMapV7WidgetsDeprecated",
            )
        for (fieldName in fields) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.setAccessible(true)
                field.set(null, null)
            } catch (e: NoSuchFieldException) {
                // Some fields might not exist in older versions, ignore
                println("Field " + fieldName + " not found, ignoring.")
            }
        }
    }

    @Throws(Exception::class)
    fun runTest() {
        val numThreads = 20
        val numIterations = 1000
        val failureCount = AtomicInteger(0)
        println("Starting race condition test...")
        for (i in 0..<numIterations) {
            resetStaticFields()
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(numThreads)
            val executor = Executors.newFixedThreadPool(numThreads)
            val futures = ArrayList<Future<*>?>()
            for (t in 0..<numThreads) {
                futures.add(
                    executor.submit(
                        Runnable {
                            try {
                                startLatch.await()
                                val map = Operations.getOperations(7, RcProfiles.PROFILE_ANDROIDX)
                                if (map == null) {
                                    System.err.println("Returned map is null!")
                                    failureCount.incrementAndGet()
                                } else {
                                    val op = map.get(Operations.CORE_TEXT)
                                    if (op == null) {
                                        System.err.println("CORE_TEXT is missing from map!")
                                        failureCount.incrementAndGet()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                failureCount.incrementAndGet()
                            } finally {
                                endLatch.countDown()
                            }
                        }
                    )
                )
            }
            startLatch.countDown() // Start all threads
            endLatch.await() // Wait for all threads to finish
            executor.shutdown()
            if (failureCount.get() > 0) {
                println("Failed at iteration " + i)
                break
            }
        }
        if (failureCount.get() != 0) {
            throw AssertionError(
                "Failure: Race condition detected! Total failures: " + failureCount.get()
            )
        }
        println("Success: No race condition detected in " + numIterations + " iterations.")
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            runTest()
            System.exit(0)
        } catch (e: AssertionError) {
            System.err.println(e.message)
            System.exit(1)
        }
    }
}
