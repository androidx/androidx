/*
 * Copyright 2021 The Android Open Source Project
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

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanConcurrentHashMapTest : AbstractLintDetectorTest(
    useDetector = BanConcurrentHashMap(),
    useIssues = listOf(BanConcurrentHashMap.ISSUE),
) {

    @Test
    fun `Detection of ConcurrentHashMap usage in Java sources`() {
        val input = java(
            "src/androidx/ConcurrentHashMapUsageJava.java",
            """
                import androidx.annotation.NonNull;
                import java.util.Map;
                import java.util.concurrent.ConcurrentHashMap;

                public class ConcurrentHashMapUsageJava {

                    private final ConcurrentHashMap<?, ?> mMap = new ConcurrentHashMap<>();

                    @NonNull
                    public <V, K> Map<V, K> createMap() {
                        return new ConcurrentHashMap<>();
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapUsageJava.java:3: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
import java.util.concurrent.ConcurrentHashMap;
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun `Detection of ConcurrentHashMap usage in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/ConcurrentHashMapUsageKotlin.kt",
            """
                package androidx

                import java.util.concurrent.ConcurrentHashMap

                @Suppress("unused")
                class ConcurrentHashMapUsageKotlin {
                    private val mMap: ConcurrentHashMap<*, *> = ConcurrentHashMap<Any, Any>()
                    fun <V, K> createMap(): Map<V, K> {
                        return ConcurrentHashMap()
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapUsageKotlin.kt:3: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
import java.util.concurrent.ConcurrentHashMap
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                input
            )
            .skipTestModes(TestMode.IMPORT_ALIAS) // b/203124716
            .run()
            .expect(expected)
    }
}