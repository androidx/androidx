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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanTargetApiAnnotationTest : AbstractLintDetectorTest(
    useDetector = BanTargetApiAnnotation(),
    useIssues = listOf(BanTargetApiAnnotation.ISSUE),
) {

    @Test
    fun `Detection of TargetApi usage in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.TargetApiUsageJava"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/TargetApiUsageJava.java:22: Error: Uses @TargetApi annotation [BanTargetApiAnnotation]
@TargetApi(29)
~~~~~~~~~~~~~~
src/androidx/TargetApiUsageJava.java:25: Error: Uses @TargetApi annotation [BanTargetApiAnnotation]
    @TargetApi(30)
    ~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}