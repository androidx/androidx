/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULiteralExpression

/**
 * This lint check enforces that screenshot test classes are annotated with `@SdkSuppress` to ensure
 * they run exclusively on API level 35 (VanillaIceCream).
 */
class ScreenshotTestSdkSuppressAnnotationEnforcer : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val testPath = context.file.absolutePath
                if (
                    !(testPath.contains("androidTest") ||
                        testPath.contains("androidInstrumentedTest"))
                ) {
                    return
                }
                if (!isScreenshotTestClass(node)) {
                    return
                }

                val sdkSuppressAnnotation = node.findAnnotation(SDK_SUPPRESS_ANNOTATION_FQN)
                if (sdkSuppressAnnotation == null) {
                    context.report(
                        ISSUE,
                        node,
                        context.getNameLocation(node),
                        "Screenshot test class `${node.name}` must be annotated with `@SdkSuppress` to run only on API 35.",
                    )
                    return
                }

                val minSdkVersion = sdkSuppressAnnotation.findAttributeValue("minSdkVersion")
                val maxSdkVersion = sdkSuppressAnnotation.findAttributeValue("maxSdkVersion")

                var minSdkCorrect = false
                if (minSdkVersion is ULiteralExpression) {
                    if (minSdkVersion.value == TARGET_API_LEVEL) {
                        minSdkCorrect = true
                    }
                } else if (
                    minSdkVersion != null &&
                        (minSdkVersion.sourcePsi?.text == TARGET_API_CODENAME_QUALIFIER ||
                            minSdkVersion.sourcePsi?.text ==
                                TARGET_API_CODENAME_QUALIFIER_ANDROID_OS)
                ) {
                    minSdkCorrect = true
                }

                var maxSdkCorrect = false
                if (maxSdkVersion is ULiteralExpression) {
                    if (maxSdkVersion.value == TARGET_API_LEVEL) {
                        maxSdkCorrect = true
                    }
                } else if (
                    maxSdkVersion != null &&
                        (maxSdkVersion.sourcePsi?.text == TARGET_API_CODENAME_QUALIFIER ||
                            maxSdkVersion.sourcePsi?.text ==
                                TARGET_API_CODENAME_QUALIFIER_ANDROID_OS)
                ) {
                    maxSdkCorrect = true
                }

                if (!minSdkCorrect || !maxSdkCorrect) {
                    context.report(
                        ISSUE,
                        sdkSuppressAnnotation,
                        context.getLocation(sdkSuppressAnnotation),
                        "`@SdkSuppress` on screenshot test class `${node.name}` must have `minSdkVersion` and `maxSdkVersion` set to 35.",
                    )
                }
            }
        }
    }

    private fun isScreenshotTestClass(node: UClass): Boolean {
        return node.fields.any { field -> isAndroidXScreenshotTestRule(field) }
    }

    private fun isAndroidXScreenshotTestRule(field: UField): Boolean {
        val fieldType = field.type.canonicalText
        return fieldType == "androidx.test.screenshot.AndroidXScreenshotTestRule"
    }

    companion object {
        private const val SDK_SUPPRESS_ANNOTATION_FQN = "androidx.test.filters.SdkSuppress"
        private const val TARGET_API_LEVEL = 35
        private const val TARGET_API_CODENAME_QUALIFIER = "Build.VERSION_CODES.VANILLA_ICE_CREAM"
        private const val TARGET_API_CODENAME_QUALIFIER_ANDROID_OS =
            "android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM"

        @JvmField
        val ISSUE =
            Issue.create(
                id = "ScreenshotTestSdkSuppress",
                briefDescription = "Screenshot tests must use @SdkSuppress for API 35.",
                explanation =
                    """
                Screenshot test classes should be annotated with `@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)`
                to ensure they only run on API level 35. This helps maintain consistency and manageability of screenshot tests.
            """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        ScreenshotTestSdkSuppressAnnotationEnforcer::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
