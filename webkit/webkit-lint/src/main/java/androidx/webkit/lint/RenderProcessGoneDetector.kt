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
import org.jetbrains.uast.UClass

class RenderProcessGoneDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses(): List<String> =
        listOf("android.webkit.WebViewClient", "androidx.webkit.WebViewClientCompat")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (
            declaration.isInterface ||
                declaration.isEnum ||
                declaration.isAnnotationType ||
                declaration.hasModifierProperty(com.intellij.psi.PsiModifier.ABSTRACT)
        ) {
            return
        }

        val qualifiedName = declaration.qualifiedName
        if (
            qualifiedName == "android.webkit.WebViewClient" ||
                qualifiedName == "androidx.webkit.WebViewClientCompat"
        ) {
            return
        }

        val hasOnRenderProcessGone =
            declaration.javaPsi.findMethodsByName("onRenderProcessGone", true).any {
                it.parameterList.parametersCount == 2 &&
                    it.containingClass?.qualifiedName != "android.webkit.WebViewClient" &&
                    it.containingClass?.qualifiedName != "androidx.webkit.WebViewClientCompat" &&
                    it.containingClass?.qualifiedName != "java.lang.Object"
            }

        if (!hasOnRenderProcessGone) {
            context.report(ISSUE, declaration, context.getNameLocation(declaration), ERROR_MESSAGE)
        }
    }

    override fun getApplicableConstructorTypes(): List<String> =
        listOf("android.webkit.WebViewClient", "androidx.webkit.WebViewClientCompat")

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        context.report(ISSUE, node, context.getLocation(node), ERROR_MESSAGE)
    }

    companion object {
        const val ERROR_MESSAGE =
            "WebViewClient should implement `onRenderProcessGone` to handle render process crashes or out-of-memory errors. " +
                "Otherwise, the app will crash when the render process is shut down."

        val ISSUE =
            Issue.create(
                    id = "MissingOnRenderProcessGone",
                    briefDescription = "WebViewClient does not implement onRenderProcessGone",
                    explanation =
                        "If a WebView's render process is shut down, the app will crash " +
                            "unless the WebViewClient implements `onRenderProcessGone` and returns `true`. " +
                            "It is highly recommended to handle this callback.",
                    category = Category.CORRECTNESS,
                    priority = 5,
                    severity = Severity.WARNING,
                    implementation =
                        Implementation(RenderProcessGoneDetector::class.java, Scope.JAVA_FILE_SCOPE),
                )
                .addMoreInfo(
                    "https://developer.android.com/reference/android/webkit/WebViewClient#onRenderProcessGone(" +
                        "android.webkit.WebView,%20android.webkit.RenderProcessGoneDetail"
                )
    }
}
