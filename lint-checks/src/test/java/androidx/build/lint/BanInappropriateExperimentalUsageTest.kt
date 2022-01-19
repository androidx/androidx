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

@file:Suppress("UnstableApiUsage", "GroovyUnusedAssignment")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestMode
import java.io.FileNotFoundException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanInappropriateExperimentalUsageTest : AbstractLintDetectorTest(
    useDetector = BanInappropriateExperimentalUsage(),
    useIssues = listOf(BanInappropriateExperimentalUsage.ISSUE),
    stubs = arrayOf(Stubs.OptIn),
) {
    // This must match the setting in buildSrc/lint.xml
    private val validLintConfig: TestFile = TestFiles.xml(
        "lint.xml",
        """
<lint>
    <issue id="IllegalExperimentalApiUsage">
        <option name="atomicLibraryGroupFilename" value="atomic-library-groups.txt"/>
    </issue>
</lint>
            """.trimIndent()
    )

    private val libraryGroups = TestFiles.source(
        "atomic-library-groups.txt",
        """
androidx.a
androidx.b
sample.annotation.provider
            """.trimIndent()
    )

    private val UNUSED_PLACEHOLDER = "unused"

    @Test
    fun `Test same atomic module Experimental usage via Gradle model`() {
        val provider = project()
            .name("provider")
            .files(
                ktSample("sample.annotation.provider.WithinGroupExperimentalAnnotatedClass"),
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
                validLintConfig,
                libraryGroups,
            )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(provider).expect(expected)
    }

    @Test
    fun `Test same non-atomic module Experimental usage via Gradle model`() {

        // This is the same as libraryGroups, only with `sample.annotation.provider` removed
        val nonatomicLibraryGroups = TestFiles.source(
            "atomic-library-groups.txt",
            """
androidx.a
androidx.b
            """.trimIndent()
        )

        val provider = project()
            .name("provider")
            .files(
                ktSample("sample.annotation.provider.WithinGroupExperimentalAnnotatedClass"),
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
                validLintConfig,
                nonatomicLibraryGroups,
            )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, testModes = listOf(TestMode.DEFAULT)).expect(expected)
    }

    @Test
    fun `Test cross-module Experimental usage via Gradle model`() {
        val provider = project()
            .name("provider")
            .type(ProjectDescription.Type.LIBRARY)
            .report(false)
            .files(
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJava"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        val consumer = project()
            .name("consumer")
            .type(ProjectDescription.Type.LIBRARY)
            .dependsOn(provider)
            .files(
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented(),
                validLintConfig,
                libraryGroups,
            )

        /* ktlint-disable max-line-length */
        val expected = """
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:32: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @ExperimentalSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:37: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @RequiresOptInSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(expected)
    }

    @Test(expected = RuntimeException::class)
    fun `Missing atomicLibraryGroupFilename property should throw an exception`() {
        val invalidLintConfig: TestFile = TestFiles.xml(
            "lint.xml",
            """
<lint>
    <issue id="IllegalExperimentalApiUsage">
        <option name="foo" value="bar"/>
    </issue>
</lint>
            """.trimIndent()
        )
        val provider = project()
            .name("provider")
            .type(ProjectDescription.Type.LIBRARY)
            .report(false)
            .files(
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJava"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        val consumer = project()
            .name("consumer")
            .type(ProjectDescription.Type.LIBRARY)
            .dependsOn(provider)
            .files(
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented(),
                invalidLintConfig,
            )

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(UNUSED_PLACEHOLDER)
    }

    @Test(expected = FileNotFoundException::class)
    fun `Missing atomic library group file should throw an exception`() {
        val provider = project()
            .name("provider")
            .type(ProjectDescription.Type.LIBRARY)
            .report(false)
            .files(
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJava"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        val consumer = project()
            .name("consumer")
            .type(ProjectDescription.Type.LIBRARY)
            .dependsOn(provider)
            .files(
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented(),
                validLintConfig,
            )

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(UNUSED_PLACEHOLDER)
    }

    @Test(expected = RuntimeException::class)
    fun `Empty atomic library group file should throw an exception`() {
        val emptyLibraryGroups = TestFiles.source("atomic-library-groups.txt", "")

        val provider = project()
            .name("provider")
            .type(ProjectDescription.Type.LIBRARY)
            .report(false)
            .files(
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJava"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        val consumer = project()
            .name("consumer")
            .type(ProjectDescription.Type.LIBRARY)
            .dependsOn(provider)
            .files(
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented(),
                validLintConfig,
                emptyLibraryGroups,
            )

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(UNUSED_PLACEHOLDER)
    }
}
