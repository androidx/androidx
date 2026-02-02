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

package androidx.build.lint.replacewith

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorKotlinConstructorTest {

    @Test
    fun constructorStaticClass() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                javaSample("replacewith.ConstructorKotlinStaticClass")
            )

        val expected =
            """
src/replacewith/ConstructorKotlinStaticClass.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageKotlin("parameter");
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/ConstructorKotlinStaticClass.java line 25: Replace with `StringBuffer("parameter")`:
@@ -19 +19
+ import java.lang.StringBuffer;
+
@@ -25 +27
-         new ReplaceWithUsageKotlin("parameter");
+         new StringBuffer("parameter");
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun constructorNonStaticClass() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                javaSample("replacewith.ConstructorKotlinNonStaticClass")
            )

        val expected =
            """
src/replacewith/ConstructorKotlinNonStaticClass.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageKotlin().new InnerClass("param");
                                         ~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/ConstructorKotlinNonStaticClass.java line 25: Replace with `InnerClass()`:
@@ -25 +25
-         new ReplaceWithUsageKotlin().new InnerClass("param");
+         new ReplaceWithUsageKotlin().new InnerClass();
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun constructorToStaticMethod() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                javaSample("replacewith.ConstructorKotlinToStaticMethod")
            )

        val expected =
            """
src/replacewith/ConstructorKotlinToStaticMethod.java:25: Information: Replacement available [ReplaceWith]
        new ReplaceWithUsageKotlin(10000);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/ConstructorKotlinToStaticMethod.java line 25: Replace with `ReplaceWithUsageKotlin.obtain(10000)`:
@@ -25 +25
-         new ReplaceWithUsageKotlin(10000);
+         ReplaceWithUsageKotlin.obtain(10000);
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
