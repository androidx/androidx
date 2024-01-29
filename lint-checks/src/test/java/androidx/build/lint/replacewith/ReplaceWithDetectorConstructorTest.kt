/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.lint.replacewith;

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorConstructorTest {

    @Test
    fun constructorStaticClass() {
        val input = arrayOf(
                javaSample("replacewith.ReplaceWithUsageJava"),
                javaSample("replacewith.ConstructorStaticClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/ConstructorStaticClass.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageJava("parameter");
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/ConstructorStaticClass.java line 25: Replace with `StringBuffer("parameter")`:
@@ -25 +25
-         new ReplaceWithUsageJava("parameter");
+         new StringBuffer("parameter");
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun constructorNonStaticClass() {
        val input = arrayOf(
            javaSample("replacewith.ReplaceWithUsageJava"),
            javaSample("replacewith.ConstructorNonStaticClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/ConstructorNonStaticClass.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageJava().new InnerClass("param");
                                       ~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/ConstructorNonStaticClass.java line 25: Replace with `InnerClass()`:
@@ -25 +25
-         new ReplaceWithUsageJava().new InnerClass("param");
+         new ReplaceWithUsageJava().new InnerClass();
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun constructorToStaticMethod() {
        val input = arrayOf(
            javaSample("replacewith.ReplaceWithUsageJava"),
            javaSample("replacewith.ConstructorToStaticMethod")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/replacewith/ConstructorToStaticMethod.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageJava(10000);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/replacewith/ConstructorToStaticMethod.java line 25: Replace with `ReplaceWithUsageJava.newInstance(10000)`:
@@ -25 +25
-         new ReplaceWithUsageJava(10000);
+         ReplaceWithUsageJava.newInstance(10000);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
