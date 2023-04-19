/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.build.lint.Stubs.Companion.JetpackRequiresOptIn
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UnstableAidlAnnotationDetectorTest : AbstractLintDetectorTest(
    useDetector = UnstableAidlAnnotationDetector(),
    useIssues = listOf(UnstableAidlAnnotationDetector.ISSUE)
) {

    @Test
    fun noAnnotation() {
        val input = aidl(
            "android/support/v4/os/IResultReceiver.aidl",
            """
                package android.support.v4.os;

                import android.os.Bundle;

                /** @hide */
                oneway interface IResultReceiver {
                    void send(int resultCode, in Bundle resultData);
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/main/aidl/android/support/v4/os/IResultReceiver.aidl:6: Error: Unstable AIDL files must be annotated with @RequiresOptIn marker [RequireUnstableAidlAnnotation]
            oneway interface IResultReceiver {
            ^
            1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun wrongAnnotation() {
        val input = arrayOf(
            java(
                "src/androidx/core/MyAnnotation.java",
                """
                    public @interface MyAnnotation {}
                """.trimIndent()
            ),
            aidl(
                "android/support/v4/os/IResultReceiver.aidl",
                """
                    package android.support.v4.os;

                    import android.os.Bundle;

                    /** @hide */
                    @JavaPassthrough(annotation="@androidx.core.MyAnnotation")
                    oneway interface IResultReceiver {
                        void send(int resultCode, in Bundle resultData);
                    }
                """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/main/aidl/android/support/v4/os/IResultReceiver.aidl:7: Error: Unstable AIDL files must be annotated with @RequiresOptIn marker [RequireUnstableAidlAnnotation]
            oneway interface IResultReceiver {
            ^
            1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun multipleAnnotations() {
        val input = arrayOf(
            JetpackRequiresOptIn,
            java(
                "src/androidx/core/MyAnnotation.java",
                """
                    public @interface MyAnnotation {}
                """.trimIndent()
            ),
            java(
                "src/androidx/core/UnstableAidlDefinition.java",
                """
                    @RequiresOptIn
                    public @interface UnstableAidlDefinition {}
                """.trimIndent()
            ),
            aidl(
                "android/support/v4/os/IResultReceiver.aidl",
                """
                    package android.support.v4.os;

                    import android.os.Bundle;

                    /** @hide */
                    @JavaPassthrough(annotation="@androidx.core.UnstableAidlDefinition")
                    @JavaPassthrough(annotation="@androidx.core.MyAnnotation")
                    oneway interface IResultReceiver {
                        void send(int resultCode, in Bundle resultData);
                    }
                """.trimIndent()
            ),
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun correctAnnotation() {
        val input = arrayOf(
            JetpackRequiresOptIn,
            java(
                "src/androidx/core/UnstableAidlDefinition.java",
                """
                    @RequiresOptIn
                    public @interface UnstableAidlDefinition {}
                """.trimIndent()
            ),
            aidl(
                "android/support/v4/os/IResultReceiver.aidl",
                """
                    package android.support.v4.os;

                    import android.os.Bundle;

                    /** @hide */
                    @JavaPassthrough(annotation="@androidx.core.UnstableAidlDefinition")
                    oneway interface IResultReceiver {
                        void send(int resultCode, in Bundle resultData);
                    }
                """.trimIndent()
            ),
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}

fun aidl(to: String, source: String): TestFile = TestFiles
    .source(to, source)
    .within("src/main/aidl")
