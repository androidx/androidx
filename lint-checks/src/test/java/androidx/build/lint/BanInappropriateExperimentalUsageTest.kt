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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestFiles.gradle
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanInappropriateExperimentalUsageTest {

    @Test
    fun `Test within-module Experimental usage via Gradle model`() {
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
                OPT_IN_KT,
            )

        lint()
            .projects(provider)
            .issues(BanInappropriateExperimentalUsage.ISSUE)
            .run()
            .expect(
                """
                No warnings.
                """.trimIndent()
            )
    }

    @Test
    fun `Test cross-module Experimental usage via Gradle model`() {
        val provider = project()
            .name("provider")
            .report(false)
            .files(
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
                OPT_IN_KT,
            )

        val consumer = project()
            .name("consumer")
            .dependsOn(provider)
            .files(
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented()
            )

        lint()
            .projects(provider, consumer)
            .issues(BanInappropriateExperimentalUsage.ISSUE)
            .run()
            .expect(
                /* ktlint-enable max-line-length */
                """
                src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:25: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
                    @ExperimentalSampleAnnotationJava
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
                /* ktlint-enable max-line-length */
            )
    }
}

/* ktlint-disable max-line-length */

/**
 * [TestFile] containing OptIn.kt from the Kotlin standard library.
 *
 * This is a workaround for the Kotlin standard library used by the Lint test harness not
 * including the Experimental annotation by default.
 */
private val OPT_IN_KT: TestFile = TestFiles.kotlin(
    """
    package kotlin

    import kotlin.annotation.AnnotationRetention.BINARY
    import kotlin.annotation.AnnotationRetention.SOURCE
    import kotlin.annotation.AnnotationTarget.*
    import kotlin.internal.RequireKotlin
    import kotlin.internal.RequireKotlinVersionKind
    import kotlin.reflect.KClass

    @Target(ANNOTATION_CLASS)
    @Retention(BINARY)
    @SinceKotlin("1.3")
    @RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
    public annotation class RequiresOptIn(
        val message: String = "",
        val level: Level = Level.ERROR
    ) {
        public enum class Level {
            WARNING,
            ERROR,
        }
    }

    @Target(
        CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
    )
    @Retention(SOURCE)
    @SinceKotlin("1.3")
    @RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
    public annotation class OptIn(
        vararg val markerClass: KClass<out Annotation>
    )
    """.trimIndent()
)

/* ktlint-enable max-line-length */
