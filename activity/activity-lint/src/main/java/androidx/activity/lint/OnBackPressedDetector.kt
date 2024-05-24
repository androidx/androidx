/*
 * Copyright 2024 The Android Open Source Project
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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType

class OnBackPressedDetector : Detector(), Detector.UastScanner {
    companion object {
        val ISSUE =
            Issue.create(
                    id = "InvalidUseOfOnBackPressed",
                    briefDescription = "Do not call onBackPressed() within OnBackPressedDisptacher",
                    explanation =
                        """You should not used OnBackPressedCallback for non-UI cases. If you
                |add a callback, you have to handle back completely in the callback.
            """,
                    category = Category.CORRECTNESS,
                    severity = Severity.WARNING,
                    implementation =
                        Implementation(
                            OnBackPressedDetector::class.java,
                            EnumSet.of(Scope.JAVA_FILE),
                            Scope.JAVA_FILE_SCOPE
                        )
                )
                .addMoreInfo(
                    "https://developer.android.com/guide/navigation/custom-back/" +
                        "predictive-back-gesture#ui-logic"
                )
    }

    override fun getApplicableMethodNames(): List<String> = listOf(OnBackPressed)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        method.containingClass ?: return
        if (isCalledInHandledOnBackPressed(node.getParentOfType())) {
            context.report(
                ISSUE,
                context.getLocation(node),
                "Should not call onBackPressed inside of OnBackPressedCallback.handledOnBackPressed"
            )
        }
    }

    private fun isCalledInHandledOnBackPressed(uMethod: UMethod?): Boolean {
        if (uMethod == null) return false
        return HandledOnBackPressed == uMethod.name
    }
}

private val OnBackPressed = "onBackPressed"

private val HandledOnBackPressed = "handledOnBackPressed"
