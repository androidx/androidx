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

package androidx.compose.ui.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isInvokedWithinComposable
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

/**
 * [Detector] that checks for usages of `java.util.Locale.getDefault()`,
 * `android.os.LocaleList.getAdjustedDefault()` or
 * `androidx.core.os.LocaleListCompat.getAdjustedDefault() that are called within a composable
 * function. The replacement for these calls should be calling the composition locals which will
 * correctly read the current locale in an observable way.
 */
class NonObservableLocaleDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve() ?: return
                val containingClass = method.containingClass ?: return
                if (!node.isInvokedWithinComposable()) return

                if (
                    method.name == "getDefault" &&
                        containingClass.name == JavaLocale.shortName &&
                        containingClass.isInPackageName(JavaLocale.packageName)
                ) {
                    context.report(
                        issue = NonObservableLocale,
                        location = context.getLocation(node),
                        message = BriefDescription,
                        quickfixData =
                            LintFix.create()
                                .replace()
                                .name("Replace with LocalLocale.current.platformLocale")
                                .all()
                                .with("LocalLocale.current.platformLocale")
                                .imports("androidx.compose.ui.platform.LocalLocale")
                                .autoFix()
                                .build(),
                    )
                } else if (
                    method.name == "getAdjustedDefault" &&
                        containingClass.name == AndroidLocaleList.shortName &&
                        containingClass.isInPackageName(AndroidLocaleList.packageName)
                ) {
                    context.report(
                        issue = NonObservableLocale,
                        location = context.getLocation(node),
                        message = BriefDescription,
                        quickfixData =
                            LintFix.create()
                                .replace()
                                .name("Replace with LocalLocaleList.current")
                                .all()
                                .with(
                                    "android.os.LocaleList(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())"
                                )
                                .imports("androidx.compose.ui.platform.LocalLocaleList")
                                .autoFix()
                                .build(),
                    )
                } else if (
                    method.name == "getAdjustedDefault" &&
                        containingClass.name == AndroidXLocaleListCompat.shortName &&
                        containingClass.isInPackageName(AndroidXLocaleListCompat.packageName)
                ) {
                    context.report(
                        issue = NonObservableLocale,
                        location = context.getLocation(node),
                        message = BriefDescription,
                        quickfixData =
                            LintFix.create()
                                .replace()
                                .name("Replace with LocalLocaleList.current")
                                .all()
                                .with(
                                    "LocaleListCompat.create(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())"
                                )
                                .imports("androidx.compose.ui.platform.LocalLocaleList")
                                .autoFix()
                                .build(),
                    )
                }
            }
        }

    companion object {
        private val JavaUtilPackage = Package("java.util")
        private val JavaLocale = Name(JavaUtilPackage, "Locale")
        private val AndroidOsPackage = Package("android.os")
        private val AndroidLocaleList = Name(AndroidOsPackage, "LocaleList")
        private val AndroidXOsPackage = Package("androidx.core.os")
        private val AndroidXLocaleListCompat = Name(AndroidXOsPackage, "LocaleListCompat")
        private val BriefDescription =
            "Reading locale in a non-observable way in a composable function"

        val NonObservableLocale =
            Issue.create(
                id = "NonObservableLocale",
                briefDescription = BriefDescription,
                explanation =
                    "Calling `java.util.Locale.getDefault()`, " +
                        "`android.os.LocaleList.getAdjustedDefault()` or " +
                        "`androidx.core.os.LocaleListCompat.getAdjustedDefault()` within a " +
                        "composable function is not recommended. Calling these functions does " +
                        "not read observable state and will not be updated if the user changes " +
                        "the locale. This can lead to subtle bugs in your UI where the wrong " +
                        "locale is used for formatting dates, times, numbers, and other " +
                        "locale-sensitive information. To fix this, you should use " +
                        "`androidx.compose.ui.platform.LocalLocale.current.platformLocale` " +
                        "instead. This will ensure that your UI is always up-to-date with the " +
                        "current locale.",
                category = Category.CORRECTNESS,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        NonObservableLocaleDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
