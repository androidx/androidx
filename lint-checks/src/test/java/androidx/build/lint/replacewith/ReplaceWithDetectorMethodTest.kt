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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorMethodTest {

    @Test
    fun staticMethodExplicitClass() {
        val input = arrayOf(
            javaSample("replacewith.ReplaceWithUsageJava"),
            javaSample("replacewith.StaticMethodExplicitClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/StaticMethodExplicitClass.java:25: Information: Replacement available [ReplaceWith]
        ReplaceWithUsageJava.toString(this);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/StaticMethodExplicitClass.java line 25: Replace with `this.toString()`:
@@ -25 +25
-         ReplaceWithUsageJava.toString(this);
+         this.toString();
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodImplicitThis() {
        val input = arrayOf(
            javaSample("replacewith.MethodImplicitThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/MethodImplicitThis.java:33: Information: Replacement available [ReplaceWith]
        oldMethod(null);
        ~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/MethodImplicitThis.java line 33: Replace with `newMethod(null)`:
@@ -33 +33
-         oldMethod(null);
+         newMethod(null);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodExplicitThis() {
        val input = arrayOf(
            javaSample("replacewith.MethodExplicitThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/MethodExplicitThis.java:33: Information: Replacement available [ReplaceWith]
        this.oldMethod(null);
             ~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/MethodExplicitThis.java line 33: Replace with `newMethod(null)`:
@@ -33 +33
-         this.oldMethod(null);
+         this.newMethod(null);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
