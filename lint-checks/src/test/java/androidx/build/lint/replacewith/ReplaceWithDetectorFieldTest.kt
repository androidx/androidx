/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.lint.replacewith

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorFieldTest {

    @Test
    fun staticFieldExplicitClass() {
        val input = arrayOf(
            javaSample("replacewith.ReplaceWithUsageJava"),
            javaSample("replacewith.StaticFieldExplicitClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/StaticFieldExplicitClass.java:25: Information: Replacement available [ReplaceWith]
        System.out.println(ReplaceWithUsageJava.AUTOFILL_HINT_NAME);
                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/StaticFieldExplicitClass.java line 25: Replace with `View.AUTOFILL_HINT_NAME`:
@@ -25 +25
-         System.out.println(ReplaceWithUsageJava.AUTOFILL_HINT_NAME);
+         System.out.println(View.AUTOFILL_HINT_NAME);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun staticFieldImplicitClass() {
        val input = arrayOf(
            javaSample("replacewith.ReplaceWithUsageJava"),
            javaSample("replacewith.StaticFieldImplicitClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/StaticFieldImplicitClass.java:27: Information: Replacement available [ReplaceWith]
        System.out.println(AUTOFILL_HINT_NAME);
                           ~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/StaticFieldImplicitClass.java line 27: Replace with `View.AUTOFILL_HINT_NAME`:
@@ -27 +27
-         System.out.println(AUTOFILL_HINT_NAME);
+         System.out.println(View.AUTOFILL_HINT_NAME);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
