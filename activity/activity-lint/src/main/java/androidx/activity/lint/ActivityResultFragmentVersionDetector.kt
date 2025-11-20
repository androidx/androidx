/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.model.LintModelAndroidLibrary
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

class ActivityResultFragmentVersionDetector : Detector(), UastScanner, GradleScanner {
    companion object {
        const val FRAGMENT_VERSION = "1.3.0"

        val ISSUE =
            Issue.create(
                    id = "InvalidFragmentVersionForActivityResult",
                    briefDescription =
                        "Update to Fragment $FRAGMENT_VERSION to use ActivityResult APIs",
                    explanation =
                        """In order to use the ActivityResult APIs you must upgrade your \
                Fragment version to $FRAGMENT_VERSION. Previous versions of FragmentActivity \
                failed to call super.onRequestPermissionsResult() and used invalid request codes""",
                    category = Category.CORRECTNESS,
                    severity = Severity.FATAL,
                    implementation =
                        Implementation(
                            ActivityResultFragmentVersionDetector::class.java,
                            EnumSet.of(Scope.JAVA_FILE, Scope.GRADLE_FILE),
                            Scope.JAVA_FILE_SCOPE,
                            Scope.GRADLE_SCOPE,
                        ),
                )
                .addMoreInfo(
                    "https://developer.android.com/training/permissions/requesting#make-the-request"
                )
    }

    var locations = ArrayList<Location>()
    lateinit var expression: UCallExpression

    private var checkedImplementationDependencies = false

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodIdentifier?.name != "registerForActivityResult") {
                    return
                }
                expression = node
                locations.add(context.getLocation(node))
            }
        }
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
        if (locations.isEmpty()) {
            return
        }
        if (property == "api") {
            // always check api dependencies
            reportIssue(value, context)
        } else if (!checkedImplementationDependencies) {
            context.project.buildVariant.mainArtifact.dependencies.getAll().forEach { lmLibrary ->
                reportIssue(
                    (lmLibrary as? LintModelAndroidLibrary)?.resolvedCoordinates.toString(),
                    context,
                    false,
                )
            }
            checkedImplementationDependencies = true
        }
    }

    private fun reportIssue(value: String, context: GradleContext, removeQuotes: Boolean = true) {
        val library =
            if (removeQuotes) {
                getStringLiteralValue(value)
            } else {
                value
            }

        if (library.isNotEmpty()) {
            val currentVersion =
                library.substringAfter("androidx.fragment:fragment:").substringBeforeLast("-")
            if (library != currentVersion && currentVersion < FRAGMENT_VERSION) {
                locations.forEach { location ->
                    context.report(
                        ISSUE,
                        expression,
                        location,
                        "Upgrade Fragment version to at least $FRAGMENT_VERSION.",
                    )
                }
            }
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
