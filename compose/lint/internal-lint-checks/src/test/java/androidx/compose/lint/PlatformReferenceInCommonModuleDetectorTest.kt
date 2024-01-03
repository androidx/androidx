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

@file:Suppress("UnstableApiUsage")

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class PlatformReferenceInCommonModuleDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = PlatformReferenceInCommonModuleDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        PlatformReferenceInCommonModuleDetector.IMPORT_ISSUE,
        PlatformReferenceInCommonModuleDetector.REFERENCE_ISSUE
    )

    @Test
    fun detectsImportInCommonMain() {
        val file = kotlin(
            "commonMain/test/TestFile.kt",
            """
                package test

                import java.util.ArrayList as MyList
                import java.util.*
                import java.*
                import android.os.Bundle
                import android.*
            """
        ).within("src")

        lint().files(
            file
        )
            .run()
            .expect(
                """
src/commonMain/test/TestFile.kt:4: Error: Platform-dependent import in a common module [PlatformImportInCommonModule]
                import java.util.ArrayList as MyList
                       ~~~~~~~~~~~~~~~~~~~
src/commonMain/test/TestFile.kt:5: Error: Platform-dependent import in a common module [PlatformImportInCommonModule]
                import java.util.*
                       ~~~~~~~~~
src/commonMain/test/TestFile.kt:6: Error: Platform-dependent import in a common module [PlatformImportInCommonModule]
                import java.*
                       ~~~~
src/commonMain/test/TestFile.kt:7: Error: Platform-dependent import in a common module [PlatformImportInCommonModule]
                import android.os.Bundle
                       ~~~~~~~~~~~~~~~~~
src/commonMain/test/TestFile.kt:8: Error: Platform-dependent import in a common module [PlatformImportInCommonModule]
                import android.*
                       ~~~~~~~
5 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun detectsJavaClassCallsInCommonMain() {
        val file = kotlin(
            "commonMain/test/TestFile.kt",
            """
                package test

                fun test(test: String) {
                    test.javaClass
                    test::class.java
                }

                class AttributeSnapshot {
                  // since data classes can't be subclassed
                  override fun equals(other: Any?): Boolean {
                    if (javaClass != other?.javaClass) return false
                    return true
                  }
                }
            """
        ).within("src")

        lint().files(
            file
        )
            .run()
            .expect(
                """
src/commonMain/test/TestFile.kt:5: Error: Platform reference in a common module [PlatformReferenceInCommonModule]
                    test.javaClass
                         ~~~~~~~~~
src/commonMain/test/TestFile.kt:6: Error: Platform reference in a common module [PlatformReferenceInCommonModule]
                    test::class.java
                                ~~~~
src/commonMain/test/TestFile.kt:12: Error: Platform reference in a common module [PlatformReferenceInCommonModule]
                    if (javaClass != other?.javaClass) return false
                        ~~~~~~~~~
src/commonMain/test/TestFile.kt:12: Error: Platform reference in a common module [PlatformReferenceInCommonModule]
                    if (javaClass != other?.javaClass) return false
                                            ~~~~~~~~~
4 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun ignoresImportInOtherModules() {
        val jvmFile = kotlin(
            "jvmMain/test/TestFile.kt",
            """
                package test

                import java.util.ArrayList as MyList
                import java.util.*
                import java.*
                import android.os.Bundle
                import android.*
            """
        ).within("src")

        val androidFile = kotlin(
            "androidMain/test/TestFile.kt",
            """
                package test

                import java.util.*
                import java.*
                import android.os.Bundle
                import android.*
            """
        ).within("src")

        val file = kotlin(
            "main/test/TestFile.kt",
            """
                package test

                import java.util.*
                import java.*
                import android.os.Bundle
                import android.*
            """
        ).within("src")

        lint().files(
            file,
            androidFile,
            jvmFile
        )
            .run()
            .expectClean()
    }

    @Test
    fun ignoresJavaClassCallsInOtherSourceSets() {
        val jvmFile = kotlin(
            "jvmMain/test/TestFile.kt",
            """
                package test

                fun test(test: String) {
                    test.javaClass
                    test::class.java
                }

                class AttributeSnapshot {
                  // since data classes can't be subclassed
                  override fun equals(other: Any?): Boolean {
                    if (javaClass != other?.javaClass) return false
                    return true
                  }
                }
            """
        ).within("src")

        val androidFile = kotlin(
            "androidMain/test/TestFile.kt",
            """
                package test

                fun test(test: String) {
                    test.javaClass
                    test::class.java
                }

                class AttributeSnapshot {
                  // since data classes can't be subclassed
                  override fun equals(other: Any?): Boolean {
                    if (javaClass != other?.javaClass) return false
                    return true
                  }
                }
            """
        ).within("src")

        val file = kotlin(
            "main/test/TestFile.kt",
            """
                package test

                fun test(test: String) {
                    test.javaClass
                    test::class.java
                }

                class AttributeSnapshot {
                  // since data classes can't be subclassed
                  override fun equals(other: Any?): Boolean {
                    if (javaClass != other?.javaClass) return false
                    return true
                  }
                }
            """
        ).within("src")

        lint().files(
            file,
            androidFile,
            jvmFile
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
