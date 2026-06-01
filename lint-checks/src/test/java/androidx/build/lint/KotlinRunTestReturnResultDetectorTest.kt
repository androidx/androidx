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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestFiles
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinRunTestReturnResultDetectorTest :
    AbstractLintDetectorTest(
        useDetector = KotlinRunTestReturnResultDetector(),
        useIssues = listOf(KotlinRunTestReturnResultDetector.RUN_TEST_RESULT_UNUSED_ISSUE),
        stubs = arrayOf(runTestStub, Stubs.TestAnnotation),
    ) {

    @Test
    fun `Assert runTest return result checked used in Kotlin-JVM`() {
        val input = arrayOf(ktSample("androidx.KotlinRunTestReturnResultKotlin"))

        val expected =
            """
src/androidx/KotlinRunTestReturnResultKotlin.kt:35: Error: Result of runTest is ignored. [KotlinRunTestResultUnused]
        runTest {}
        ~~~~~~~
src/androidx/KotlinRunTestReturnResultKotlin.kt:42: Error: Result of runTest is ignored. [KotlinRunTestResultUnused]
        propagatedUtilityFun()
        ~~~~~~~~~~~~~~~~~~~~
src/androidx/KotlinRunTestReturnResultKotlin.kt:47: Error: Result of runTest is ignored. [KotlinRunTestResultUnused]
        runTest {}
        ~~~~~~~
3 errors
        """
        lint()
            .files(*stubs, *input)
            .allowDuplicates()
            .run()
            .expect(expected)
            .expectFixDiffs(
                """
Fix for src/androidx/KotlinRunTestReturnResultKotlin.kt line 35: Insert `return`:
@@ -35 +35 @@
-        runTest {}
+        return runTest {}
Fix for src/androidx/KotlinRunTestReturnResultKotlin.kt line 42: Insert `return`:
@@ -42 +42 @@
-        propagatedUtilityFun()
+        return propagatedUtilityFun()
Fix for src/androidx/KotlinRunTestReturnResultKotlin.kt line 47: Insert `return`:
@@ -47 +47 @@
-        runTest {}
+        return runTest {}
                """
            )
    }

    companion object {
        private val runTestStub =
            TestFiles.bytecode(
                into = "libs/CoroutinesTest.jar",
                source =
                    kotlin(
                            """
            package kotlinx.coroutines.test

            typealias TestResult = Unit

            fun runTest(body: () -> Unit): TestResult {
                TODO()
            }
            """
                        )
                        .to("kotlinx/coroutines/test/RunTest.kt"),
                checksum = 0xde69983,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgYoDQYMClwiWenV+Sk5lXoZecX5RfWpKZl1qs
                V5JaXCLEGVSaFwJkeJcoMWgxAACzhPh3PgAAAA==
                """,
                """
                kotlinx/coroutines/test/RunTestKt.class:
                H4sIAAAAAAAA/4VSW08TQRT+Zlu6dK1SVlGoityUgsqWxrcixoAkGwsaQBLD
                i9PtUKfdi5mdbfCNf6XRRHn2RxnP9iKNl/Cw55w5+33fOWfm/Pj55RuAJ3AY
                5juR9mV46niRihItQxE7WsTa2U/CQ/IvtQnGUGzzLnd8HracV4228CibYTBV
                H8SwWK73hZx2N3BOktDTMgpjZ2cQVWorRwzVy1Abw/9vQqlrmz3SYj1SLact
                dENxSWAehpHmfeJepPcS368xZBtR8+M48gyzIyVkqIUKue+4oVZEll5s4grD
                lPdeeJ0B+zVXPBAEZFgu1/8ctDaSOUhFWtRVAVdxzUIBEwylQT1Sc4MPvggE
                FW2+UCpSJiYZchuShtlk2C7/LeXW/9Xttjjhia+3aEStEk9HaperjlC9ytdx
                w4KNKYa5y+6cYXII2RWaN7nmlDOCboae30hNJjVgYB3Kn8r0VKGouc7w7PzM
                ts7PLKNo9Nz00JWmi0YpV2Ft412mWihmS+N21jYqrDK2ahXPzxYorNKXylQZ
                rHRB9kVM87C00lL9fxt3AaQm7WHjo9NYg6Vc65BWditqCoaJOinsJUFDqEPe
                8EVKjTzuH3El0/MgmT+QrZDrRFG8RDJaBsINuzKW9Pv3Bjy/2C2qdhAlyhM7
                MuXPDDhHfcYIEOswkEX/HmcwhhwyKNOpRjGNjMlV2/qM4lfYb1mWfcLN7+nt
                Y4VsjgA5mFiluNAHI49b5B/2MCYekTfpfTDeSzzu2WWskX9K2WmqMXOMjIuS
                i9su7uAuhZh1cQ9zx2Ax5rFwjFyMxRhLMcZi3I+Rj/HgF4UUZScFBAAA
                """,
            )
    }
}
