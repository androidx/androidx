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

package androidx.fragment.testing.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/**
 * Lint check for ensuring that the Fragment Testing library is included using the correct
 * debugImplementation configuration.
 */
class GradleConfigurationDetector : Detector(), GradleScanner {
    companion object {
        val ISSUE =
            Issue.create(
                    id = "FragmentGradleConfiguration",
                    briefDescription =
                        "Include the fragment-testing library using the " +
                            "debugImplementation configuration.",
                    explanation =
                        """The fragment-testing library contains a FragmentScenario class that \
                creates an Activity that must exist in the runtime APK. To include the \
                fragment-testing library in the runtime APK it must be added using the \
                debugImplementation configuration.""",
                    category = Category.CORRECTNESS,
                    severity = Severity.ERROR,
                    implementation =
                        Implementation(GradleConfigurationDetector::class.java, Scope.GRADLE_SCOPE),
                    androidSpecific = true,
                )
                .addMoreInfo("https://d.android.com/training/basics/fragments/testing#configure")
    }

    @Deprecated("This is deprecated in the GradleScanner class.")
    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        valueCookie: Any,
        statementCookie: Any,
    ) {
        // Remove enclosing quotes and check starting string to ensure only instances that
        // result in the fragment-testing library being imported are checked.
        // Non-string values cannot be resolved so invalid imports via functions, variables, etc.
        // will not be detected.
        val library = getStringLiteralValue(value)
        if (
            library.startsWith("androidx.fragment:fragment-testing") &&
                property != "debugImplementation"
        ) {
            val incident =
                Incident(context)
                    .issue(ISSUE)
                    .location(context.getLocation(statementCookie))
                    .message("Replace with debugImplementation.")
                    .fix(fix().replace().text(property).with("debugImplementation").build())
            context.report(incident)
        }
    }

    /**
     * Extracts the string value from the DSL value by removing surrounding quotes.
     *
     * Returns an empty string if [value] is not a string literal.
     */
    private fun getStringLiteralValue(value: String): String {
        if (
            value.length > 2 &&
                (value.startsWith("'") && value.endsWith("'") ||
                    value.startsWith("\"") && value.endsWith("\""))
        ) {
            return value.substring(1, value.length - 1)
        }
        return ""
    }
}
