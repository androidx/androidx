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
class BanUncheckedReflectionTest : AbstractLintDetectorTest(
    useDetector = BanUncheckedReflection(),
    useIssues = listOf(BanUncheckedReflection.ISSUE),
    stubs = arrayOf(Stubs.ChecksSdkIntAtLeast),
) {

    @Test
    fun `Detection of unchecked reflection in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.app.ActivityRecreator"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/app/ActivityRecreator.java:145: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                    requestRelaunchActivityMethod.invoke(activityThread,
                    ^
src/androidx/sample/core/app/ActivityRecreator.java:262: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity3ParamsMethod.invoke(activityThread,
                        ^
src/androidx/sample/core/app/ActivityRecreator.java:265: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity2ParamsMethod.invoke(activityThread,
                        ^
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Checked reflection in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.app.ActivityRecreatorChecked"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}
