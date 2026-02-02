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
class ReplaceWithDetectorPropertyTest {

    @Test
    fun propertyUsage_isIgnored() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                javaSample("replacewith.PropertyJava")
            )

        // TODO(b/323214452): This is incomplete, but we have explicitly suppressed replacement of
        // Kotlin property accessors until we can properly convert the expressions to Java.
        val expected =
            """
src/replacewith/PropertyJava.java:42: Information: Replacement available [ReplaceWith]
        clazz.setMethodDeprecated("value");
              ~~~~~~~~~~~~~~~~~~~
src/replacewith/PropertyJava.java:43: Information: Replacement available [ReplaceWith]
        clazz.getMethodDeprecated();
              ~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """
                .trimIndent()

        // TODO(b/323214452): These are incorrect, but we can't fix them unless we parse the
        // expression as a property reference and (a) convert to Java or (b) ignore them.
        val expectedFixDiffs =
            """
Fix for src/replacewith/PropertyJava.java line 42: Replace with `otherProperty = "value"`:
@@ -42 +42
-         clazz.setMethodDeprecated("value");
+         clazz.otherProperty = "value"("value");
Fix for src/replacewith/PropertyJava.java line 43: Replace with `otherProperty`:
@@ -43 +43
-         clazz.getMethodDeprecated();
+         clazz.otherProperty();
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
