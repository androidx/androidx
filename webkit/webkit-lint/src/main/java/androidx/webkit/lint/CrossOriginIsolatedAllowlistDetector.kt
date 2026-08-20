/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class CrossOriginIsolatedAllowlistDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf("setCrossOriginIsolatedAllowlist")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInSubClassOf(method, "androidx.webkit.Profile")) {
            context.report(ISSUE, node, context.getLocation(node), ERROR_MESSAGE)
        }
    }

    companion object {
        const val ERROR_MESSAGE =
            "Calling setCrossOriginIsolatedAllowlist enables potentially dangerous cross-origin " +
                "isolated APIs (such as SharedArrayBuffer). Ensure only trusted origins are added to the allowlist."

        val ISSUE =
            Issue.create(
                    id = "CrossOriginIsolatedAllowlist",
                    briefDescription =
                        "Profile.setCrossOriginIsolatedAllowlist enables potentially dangerous APIs",
                    explanation =
                        "Calling `setCrossOriginIsolatedAllowlist` enables cross-origin isolated APIs such as " +
                            "`SharedArrayBuffer`. These APIs can be potentially dangerous if untrusted " +
                            "origins are allowed. " +
                            "Ensure only trusted origins are added to the allowlist.",
                    category = Category.SECURITY,
                    priority = 5,
                    severity = Severity.WARNING,
                    implementation =
                        Implementation(
                            CrossOriginIsolatedAllowlistDetector::class.java,
                            Scope.JAVA_FILE_SCOPE,
                        ),
                )
                .addMoreInfo(
                    "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/SharedArrayBuffer#security_requirements"
                )
    }
}
