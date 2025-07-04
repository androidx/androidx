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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.Collections
import java.util.EnumSet
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/**
 * Enforces that test runners that do not support timeouts and host side tests do not include test
 * size annotations, as these are not used and can be misleading.
 */
class TestSizeAnnotationEnforcer : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        Collections.singletonList(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return TestSizeAnnotationHandler(context)
    }

    class TestSizeAnnotationHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            // Enforce no size annotations for host side tests
            val testPath = context.file.absolutePath
            if (ANDROID_TEST_DIRS.none { testPath.contains(it) }) {
                enforceHasNoSizeAnnotations(node)
                return
            }
        }

        /**
         * Enforces that [node] has no size annotations either on the class, or any test methods.
         */
        private fun enforceHasNoSizeAnnotations(node: UClass) {
            // Report an issue if the class has a size annotation
            node.uAnnotations
                .find { it.qualifiedName in TEST_SIZE_ANNOTATIONS }
                ?.let { annotation ->
                    val incident =
                        Incident(context)
                            .issue(UNEXPECTED_TEST_SIZE_ANNOTATION)
                            .location(context.getNameLocation(annotation))
                            .message("Unexpected test size annotation")
                            .scope(annotation)
                    context.report(incident)
                }

            node.methods
                .filter { it.hasAnnotation(TEST_ANNOTATION) }
                .forEach { method ->
                    // Report an issue if the method has a size annotation
                    method.uAnnotations
                        .find { it.qualifiedName in TEST_SIZE_ANNOTATIONS }
                        ?.let { annotation ->
                            val incident =
                                Incident(context)
                                    .issue(UNEXPECTED_TEST_SIZE_ANNOTATION)
                                    .location(context.getNameLocation(annotation))
                                    .message("Unexpected test size annotation")
                                    .scope(annotation)
                            context.report(incident)
                        }
                }
        }
    }

    companion object {
        /**
         * TODO: b/170214947 Directories that contain device test source. Unfortunately we don't
         *   currently have a better way of figuring out what test we are analyzing, as
         *   [Scope.TEST_SOURCES] includes both host and device side tests.
         */
        private val ANDROID_TEST_DIRS =
            listOf(
                "androidTest",
                "androidInstrumentedTest",
                "androidDeviceTest",
                "androidDeviceTestDebug",
                "androidDeviceTestRelease",
            )
        private const val TEST_ANNOTATION = "org.junit.Test"
        private val TEST_SIZE_ANNOTATIONS =
            listOf(
                "androidx.test.filters.SmallTest",
                "androidx.test.filters.MediumTest",
                "androidx.test.filters.LargeTest",
            )

        val UNEXPECTED_TEST_SIZE_ANNOTATION =
            Issue.create(
                "UnexpectedTestSizeAnnotation",
                "Unexpected test size annotation",
                "Host side tests and device tests with runners that do not support timeouts " +
                    "should not have any test size annotations. Host side tests all run together," +
                    " and device tests with runners that do not support timeouts will be placed in" +
                    " the 'large' test bucket, since we cannot enforce that they will be fast enough" +
                    " to run in the 'small' / 'medium' buckets.",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    TestSizeAnnotationEnforcer::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}
