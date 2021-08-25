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

@file:Suppress("KDocUnresolvedReference", "UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * Test for [SampledAnnotationEnforcer]
 *
 * This tests the following module setup:
 *
 * Module 'foo', which lives in foo
 * Module 'samples', which lives in samples, and depends on 'foo'
 *
 * Unfortunately since we cannot test submodules, we cannot verify the case for
 * foo:samples in this test.
 */
class SampledAnnotationEnforcerTest {
    private val fooModuleName = "foo"
    private val sampleModuleName = "samples"

    private val barFilePath = "../foo/src/foo/Bar.kt"

    private val emptySampleFile = kotlin(
        """
            package foo.samples
        """
    )

    private val unannotatedSampleFile = kotlin(
        """
            package foo.samples

            fun sampleBar() {}
        """
    )

    private val multipleMatchingSampleFile = kotlin(
        """
            package foo.samples

            fun sampleBar() {}

            fun sampleBar() {}
        """
    )

    private val correctlyAnnotatedSampleFile = kotlin(
        """
            package foo.samples

            @Sampled
            fun sampleBar() {}
        """
    )

    private fun checkKotlin(
        fooFile: TestFile? = null,
        sampleFile: TestFile? = null
    ): TestLintResult {
        val projectDescriptions = mutableListOf<ProjectDescription>()
        val fooProject = ProjectDescription().apply {
            name = fooModuleName
            fooFile?.let { files = arrayOf(fooFile) }
        }
        projectDescriptions += fooProject

        sampleFile?.let {
            projectDescriptions += ProjectDescription().apply {
                name = sampleModuleName
                files = arrayOf(sampleFile)
                dependsOn(fooProject)
            }
        }
        return lint()
            .projects(*projectDescriptions.toTypedArray())
            .allowMissingSdk(true)
            .issues(
                SampledAnnotationEnforcer.MISSING_SAMPLED_ANNOTATION,
                SampledAnnotationEnforcer.OBSOLETE_SAMPLED_ANNOTATION,
                SampledAnnotationEnforcer.MISSING_SAMPLES_DIRECTORY,
                SampledAnnotationEnforcer.UNRESOLVED_SAMPLE_LINK,
                SampledAnnotationEnforcer.MULTIPLE_FUNCTIONS_FOUND,
                SampledAnnotationEnforcer.INVALID_SAMPLES_LOCATION
            )
            .run()
    }

    @Test
    fun missingSampleDirectory_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val expected =
            "src/foo/Bar.kt:6: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            "$barFilePath:6: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            "$barFilePath:6: Error: sampleBar is not annotated with @Sampled, but is linked to" +
""" from the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            "$barFilePath:6: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleDirectory_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val expected =
            "src/foo/Bar.kt:5: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            "$barFilePath:5: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            "$barFilePath:5: Error: sampleBar is not annotated with @Sampled, but is linked to" +
""" from the KDoc of Bar [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            "$barFilePath:5: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleDirectory_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val expected =
            "src/foo/Bar.kt:6: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            "$barFilePath:6: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            "$barFilePath:6: Error: sampleBar is not annotated with @Sampled, but is linked to" +
""" from the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            "$barFilePath:6: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }
}
