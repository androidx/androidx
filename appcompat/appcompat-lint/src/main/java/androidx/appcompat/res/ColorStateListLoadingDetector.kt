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

package androidx.appcompat.res

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

@Suppress("UnstableApiUsage")
class ColorStateListLoadingDetector : Detector(), SourceCodeScanner {
    companion object {
        internal val NOT_USING_COMPAT_LOADING: Issue = Issue.create(
            "UseCompatLoading",
            "Should not call `Resources.getColorStateList` directly",
            "Use Compat loading of color state lists",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(ColorStateListLoadingDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String>? = listOf("getColorStateList")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Only flag Resources.getColorStateList calls with one argument (parameter)
        if (context.evaluator.isMemberInClass(method, "android.content.res.Resources") &&
            (node.valueArgumentCount == 1)
        ) {
            val message = if (context.mainProject.minSdkVersion.featureLevel > 23)
                "Use `ContextCompat.getColorStateList()`" else
                "Use `AppCompatResources.getColorStateList()`"
            context.report(
                NOT_USING_COMPAT_LOADING,
                context.getLocation(node),
                message
            )
        }
    }
}