/*
 * Copyright 2023 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanThreadSleepTest :
    AbstractLintDetectorTest(
        useDetector = BanThreadSleep(),
        useIssues = listOf(BanThreadSleep.ISSUE),
        stubs = arrayOf(Stubs.Keep),
    ) {

    @Test
    fun `Detection of Thread#sleep in Java sources`() {
        val input =
            arrayOf(
                javaSample("androidx.ThreadSleepUsageJava"),
            )

        val expected =
            """
src/androidx/ThreadSleepUsageJava.java:21: Error: Uses Thread.sleep() [BanThreadSleep]
        Thread.sleep(1000);
               ~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of Thread#sleep in Kotlin sources`() {
        val input =
            arrayOf(
                ktSample("androidx.ThreadSleepUsageKotlin"),
            )

        val expected =
            """
src/androidx/ThreadSleepUsageKotlin.kt:21: Error: Uses Thread.sleep() [BanThreadSleep]
        Thread.sleep(1000)
               ~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
