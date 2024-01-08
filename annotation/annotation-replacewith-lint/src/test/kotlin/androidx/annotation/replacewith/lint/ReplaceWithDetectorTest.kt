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

package androidx.annotation.replacewith.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorTest {

    private fun check(vararg testFiles: TestFile): TestLintResult {
        return lint()
            .files(
                ANDROIDX_REPLACE_WITH_KT,
                *testFiles
            )
            .issues(ReplaceWithDetector.ISSUE)
            .run()
    }

    @Test
    fun usageJavaFromJava() {
        val input = arrayOf(
            javaSample("sample.ReplaceMethodJava"),
            javaSample("sample.StaticMethodExplicitClass")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/StaticMethodExplicitClass.java:25: Information: $MESSAGE [ReplaceWith]
        ReplaceMethodJava.toString(this);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/sample/StaticMethodExplicitClass.java line 25: Replace with `this.toString()`:
@@ -25 +25
-         ReplaceMethodJava.toString(this);
+         this.toString();
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun implicitThis() {
        val input = arrayOf(
            javaSample("sample.ImplicitThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/ImplicitThis.java:33: Information: $MESSAGE [ReplaceWith]
        oldMethod(null);
        ~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/sample/ImplicitThis.java line 33: Replace with `newMethod(null)`:
@@ -33 +33
-         oldMethod(null);
+         newMethod(null);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun explicitThis() {
        val input = arrayOf(
            javaSample("sample.ExplicitThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/ExplicitThis.java:33: Information: $MESSAGE [ReplaceWith]
        this.oldMethod(null);
             ~~~~~~~~~~~~~~~
0 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
Fix for src/sample/ExplicitThis.java line 33: Replace with `newMethod(null)`:
@@ -33 +33
-         this.oldMethod(null);
+         this.newMethod(null);
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    /* ktlint-disable max-line-length */
    companion object {
        const val MESSAGE = "Replacement available"

        /**
         * [TestFile] containing ReplaceWith.kt from the ReplaceWith annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val ANDROIDX_REPLACE_WITH_KT: TestFile = kotlin(
            """
            package androidx.annotation

            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.TYPEALIAS
            )
            @java.lang.annotation.Target(
                ElementType.CONSTRUCTOR,
                ElementType.FIELD,
                ElementType.METHOD,
                ElementType.TYPE,
            )
            annotation class ReplaceWith(
                val expression: String,
                vararg val imports: String
            )
            """.trimIndent()
        )
    }
    /* ktlint-enable max-line-length */
}
