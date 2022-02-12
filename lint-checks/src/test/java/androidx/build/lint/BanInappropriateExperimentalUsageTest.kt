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
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Important:
 *
 * [BanInappropriateExperimentalUsage] uses the atomic library groups list generated from
 * production data.  For tests, we want to overwrite this to provide a different list of
 * atomic library groups; the file location is
 * lint-checks/src/test/resources/atomic-library-groups.txt.
 *
 * Note that the filename must match
 * [BanInappropriateExperimentalUsage.ATOMIC_LIBRARY_GROUPS_FILENAME].
 */
@RunWith(JUnit4::class)
class BanInappropriateExperimentalUsageTest : AbstractLintDetectorTest(
    useDetector = BanInappropriateExperimentalUsage(),
    useIssues = listOf(BanInappropriateExperimentalUsage.ISSUE),
    stubs = arrayOf(Stubs.OptIn),
) {

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
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJavaDuplicate"),
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
            )

        /* ktlint-disable max-line-length */
        val expected = """
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:33: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @ExperimentalSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:38: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @RequiresOptInSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:43: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @OptIn(RequiresOptInSampleAnnotationJava::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:48: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @OptIn(
    ^
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:57: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @OptIn(RequiresOptInSampleAnnotationJava::class, RequiresOptInSampleAnnotationJavaDuplicate::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(expected)
    }
}
