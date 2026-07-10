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
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClassType
import java.util.EnumSet
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement

/**
 * A detector that ensures Robolectric tests specify an SDK level via the `@Config` annotation.
 *
 * When an SDK is not explicitly defined, Robolectric defaults to a specific API level, which can
 * cause misunderstandings and gaps in test coverage. This detector enforces that all tests using
 * `RobolectricTestRunner` explicitly set an SDK level so that test authors are aware of the exact
 * API level coverage.
 *
 * This can be achieved by setting the `sdk`, `minSdk`, or `maxSdk` properties within the `@Config`
 * annotation.
 *
 * For more information, see the [Robolectric documentation](http://robolectric.org/configuring/).
 */
class MissingRobolectricSdkConfigDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val runWithAnnotation = node.findAnnotation("org.junit.runner.RunWith") ?: return
                val isRobolectricTest =
                    runWithAnnotation.attributeValues.any {
                        val expression =
                            it.expression as? UClassLiteralExpression ?: return@any false
                        val type = expression.type as? PsiClassType ?: return@any false
                        val psiClass = type.resolve() ?: return@any false
                        context.evaluator.inheritsFrom(
                            psiClass,
                            "org.robolectric.RobolectricTestRunner",
                            false,
                        )
                    }

                if (!isRobolectricTest) {
                    return
                }

                val configAnnotation = node.findAnnotation(CONFIG_QUALIFIED)

                if (configAnnotation == null) {
                    report(
                        context,
                        "Robolectric tests require an @Config annotation to explicitly" +
                            " specify an SDK level.",
                        runWithAnnotation,
                    )
                } else {
                    checkConfigAnnotation(context, runWithAnnotation, configAnnotation)
                }
            }
        }
    }

    private fun checkConfigAnnotation(
        context: JavaContext,
        runWithAnnotation: UAnnotation,
        configAnnotation: UAnnotation,
    ) {
        val hasSdk =
            configAnnotation.attributeValues.any {
                it.name == "sdk" || it.name == "minSdk" || it.name == "maxSdk"
            }
        if (!hasSdk) {
            report(
                context,
                "Robolectric @Config must specify an SDK level (e.g., sdk, minSdk, or maxSdk).",
                runWithAnnotation,
                configAnnotation,
            )
        }
    }

    private fun report(
        context: JavaContext,
        message: String,
        runWithAnnotation: UAnnotation,
        configAnnotation: UAnnotation? = null,
    ) {
        val incident =
            Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(configAnnotation ?: runWithAnnotation))
                .message(message)
                .scope(configAnnotation ?: runWithAnnotation)
                .fix(
                    fix()
                        .alternatives(
                            *createLintFixes(context, runWithAnnotation, configAnnotation)
                                .toTypedArray()
                        )
                )
        context.report(incident)
    }

    private fun createLintFixes(
        context: JavaContext,
        runWithAnnotation: UAnnotation,
        configAnnotation: UAnnotation?,
    ) =
        listOf(
            createLintFix(context, runWithAnnotation, configAnnotation, CONFIG_TARGET_SDK),
            createLintFix(context, runWithAnnotation, configAnnotation, CONFIG_ALL_SDKS),
        )

    private fun createLintFix(
        context: JavaContext,
        runWithAnnotation: UAnnotation,
        configAnnotation: UAnnotation?,
        configSdkValue: String,
    ): LintFix {
        val configValues =
            configAnnotation?.attributeValues?.joinToString { namedExpression ->
                "${namedExpression.sourcePsi?.text.toString()}, "
            } ?: ""

        val annotationQualified =
            "@$CONFIG_QUALIFIED(" +
                configValues +
                "sdk = ${
                    runWithAnnotation.wrapByLanguage(
                        qualifiedByRoboAnnotationPkg(
                            configSdkValue
                        )
                    )
                })"

        val annotationShortened =
            "@Config(sdk = ${
            runWithAnnotation.wrapByLanguage(configSdkValue)
        })"

        // Using replace() instead of annotate() directly for the following reasons.
        //
        // 1. shortenNames() can be used which allows adding just "@Config" instead of the fully
        // qualified version.
        // However, this benefit seems to appear only when using the IDE to add the fix, the
        // lintFix gradle command still seems to add as fully qualified only.
        //
        // 2. Retaining the non-sdk values from already-existing fully qualified @Config
        // annotations don't break.
        // If the already existing @Config annotation is not fully qualified, there doesn't
        // seem to be any issue with using annotate(). However, annotate() replacement seems to
        // break if the original @Config annotation was like
        // `@org.robolectric.annotation.Config(fontScale = 0.85f)`.
        val fixBuilderReplace = fix().name("Add $annotationShortened").replace()

        return if (configAnnotation == null) {
            fixBuilderReplace
                .text("")
                .with(System.lineSeparator() + annotationQualified)
                .shortenNames()
                .reformat(true)
                .range(context.getLocation(runWithAnnotation))
                .end()
                .build()
        } else {
            fixBuilderReplace
                .text(configAnnotation.sourcePsi?.text)
                .with(annotationQualified)
                .shortenNames()
                .reformat(true)
                .range(context.getLocation(configAnnotation))
                .build()
        }
    }

    private fun UElement.wrapByLanguage(msg: String): String =
        if (isKotlin(lang)) {
            "[$msg]"
        } else {
            "{$msg}"
        }

    companion object {
        private const val ROBO_ANNOTATION_PKG = "org.robolectric.annotation"
        private const val CONFIG_QUALIFIED = "$ROBO_ANNOTATION_PKG.Config"
        private const val CONFIG_TARGET_SDK = "Config.TARGET_SDK"
        private const val CONFIG_ALL_SDKS = "Config.ALL_SDKS"

        val ISSUE =
            Issue.create(
                id = "RobolectricSdkConfigRequired",
                briefDescription = "Missing SDK level in @Config annotation",
                explanation =
                    """
                    Robolectric tests must explicitly specify an SDK level to run against
                    using the @Config annotation.

                    If no SDK is set, Robolectric defaults to a specific API level, usually the
                    target SDK version, which may not be what you intend and can lead to gaps in
                    test coverage.

                    This requirement can be satisfied by setting the `sdk`, `minSdk`,
                    or `maxSdk` property.

                    For example (Kotlin):
                    ```kotlin
                    @Config(sdk = [Config.TARGET_SDK])
                    ```

                    For example (Java):
                    ```java
                    @Config(sdk = {Config.TARGET_SDK})
                    ```

                    You may also use `Config.ALL_SDKS` to run against all
                    supported SDKs.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        MissingRobolectricSdkConfigDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        private fun qualifiedByRoboAnnotationPkg(v: String) = "$ROBO_ANNOTATION_PKG.$v"
    }
}
