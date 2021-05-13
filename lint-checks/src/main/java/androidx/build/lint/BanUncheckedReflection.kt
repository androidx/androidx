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
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.checks.VersionChecks.Companion.isWithinVersionCheckConditional
import com.android.sdklib.SdkVersionInfo
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
const val METHOD_REFLECTION_CLASS = "java.lang.reflect.Method"
class BanUncheckedReflection : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("invoke")
    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // We are not really monitoring if the reflection call is within the right API check
        // we leave that to the user, and so we check for any API check really. That means
        // any check with an upper bound of the highest known API or a with a lower bound of 1
        // (which should technically include every check) is good enough.
        // Return if not reflection
        if (!context.evaluator.isMemberInClass(method, METHOD_REFLECTION_CLASS)) return
        // If not within an SDK check, flag
        if (!isWithinVersionCheckConditional(
                context, node, SdkVersionInfo.HIGHEST_KNOWN_API, false
            ) && !isWithinVersionCheckConditional(
                    context, node, 1, true
                )
        ) {

            context.report(
                ISSUE, node, context.getLocation(node),
                "Calling Method.invoke without an SDK check"
            )
        }
    }
    companion object {
        val ISSUE = Issue.create(
            "BanUncheckedReflection",
            "Reflection that is not within an SDK check",
            "Use of reflection can be risky and there is never a" +
                " reason to use reflection without" +
                " having to check for the device's SDK (either through SDK_INT comparison or " +
                "methods such as isAtLeastP etc...)" +
                ". Please surround the Method.invoke" +
                " call with the appropriate SDK_INT check.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanUncheckedReflection::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
