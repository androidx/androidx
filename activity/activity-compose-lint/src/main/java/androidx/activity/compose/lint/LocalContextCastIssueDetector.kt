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

package androidx.activity.compose.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.inheritsFrom
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassOwner
import java.util.EnumSet
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.tryResolveNamed
import org.jetbrains.uast.util.isTypeCast

class LocalContextCastIssueDetector : Detector(), SourceCodeScanner {
    private val activityType = Name(Package("android.app"), "Activity")
    private val contextType = Name(Package("android.content"), "Context")

    override fun getApplicableUastTypes() = listOf(UBinaryExpressionWithType::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) {
                // Cast expression
                if (node.isTypeCast()) {
                    // RHS is Activity
                    if (node.type.inheritsFrom(activityType)) {
                        // LHS is Context
                        if (node.operand.getExpressionType()?.inheritsFrom(contextType) == true) {
                            // Check to see if LHS is a call to LocalContext.current - the receiver
                            // will be LocalContext
                            val resolvedReceiver =
                                (node.operand as? UQualifiedReferenceExpression)
                                    ?.receiver
                                    ?.tryResolveNamed() ?: return
                            if (
                                resolvedReceiver.name == "LocalContext" &&
                                    (resolvedReceiver.containingFile as? PsiClassOwner)
                                        ?.packageName == "androidx.compose.ui.platform"
                            ) {
                                context.report(
                                    ContextCastToActivity,
                                    node,
                                    context.getNameLocation(node),
                                    DESCRIPTION
                                )
                            }
                        }
                    }
                }
            }
        }

    companion object {
        private const val DESCRIPTION =
            "LocalContext should not be cast to Activity, use LocalActivity instead"
        val ContextCastToActivity =
            Issue.create(
                "ContextCastToActivity",
                DESCRIPTION,
                "Casting Context to Activity is an error as Contexts are not always Activities. Use LocalActivity instead",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    LocalContextCastIssueDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
