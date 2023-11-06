/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lint

import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.computeKotlinArgumentMapping
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

class BoxWithConstraintsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        FoundationNames.Layout.BoxWithConstraints.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(FoundationNames.Layout.PackageName)) {
            val contentArgument = computeKotlinArgumentMapping(node, method)
                .orEmpty()
                .filter { (_, parameter) ->
                    parameter.name == "content"
                }
                .keys
                .filterIsInstance<ULambdaExpression>()
                .firstOrNull() ?: return

            var foundValidReference = false
            contentArgument.accept(object : AbstractUastVisitor() {
                // Check for references to any property of BoxWithConstraintsScope
                override fun visitSimpleNameReferenceExpression(
                    node: USimpleNameReferenceExpression
                ): Boolean {
                    val reference = (node.tryResolve() as? PsiMethod)
                        ?: return foundValidReference // No need to continue if already found
                    if (reference.isInPackageName(FoundationNames.Layout.PackageName) &&
                        reference.containingClass?.name == FoundationNames
                            .Layout.BoxWithConstraintsScope.shortName
                    ) {
                        foundValidReference = true
                    }

                    // Check if reference is an extension property on BoxWithConstraintsScope
                    if (reference.hierarchicalMethodSignature
                            .parameterTypes.firstOrNull()?.inheritsFrom(
                                FoundationNames
                                    .Layout.BoxWithConstraintsScope
                            ) == true
                    ) {
                        foundValidReference = true
                    }
                    return foundValidReference
                }

                // If this is referenced in the content lambda then consider
                // the constraints used.
                override fun visitThisExpression(node: UThisExpression): Boolean {
                    foundValidReference = true
                    return foundValidReference
                }

                // Check function calls inside the content lambda to see if they
                // are using BoxWithConstraintsScope
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val receiverType = node.receiverType ?: return foundValidReference

                    // Check for function calls with a BoxWithConstraintsScope receiver type
                    if (receiverType.inheritsFrom(FoundationNames.Layout.BoxWithConstraintsScope)) {
                        foundValidReference = true
                        return foundValidReference
                    }

                    // Check for calls to a lambda with a BoxWithConstraintsScope receiver type
                    // e.g. BoxWithConstraintsScope.() -> Unit
                    val firstChildReceiverType = (receiverType as? PsiClassReferenceType)?.reference
                        ?.typeParameters
                        ?.firstOrNull() ?: return foundValidReference

                    val resolvedWildcardType = (firstChildReceiverType as? PsiWildcardType)?.bound
                    if (
                        resolvedWildcardType?.inheritsFrom(
                            FoundationNames.Layout.BoxWithConstraintsScope
                        ) == true
                    ) {
                        foundValidReference = true
                    }

                    return foundValidReference
                }
            })
            if (!foundValidReference) {
                context.report(
                    UnusedConstraintsParameter,
                    node,
                    context.getLocation(contentArgument),
                    "BoxWithConstraints scope is not used"
                )
            }
        }
    }

    companion object {
        val UnusedConstraintsParameter = Issue.create(
            "UnusedBoxWithConstraintsScope",
            "BoxWithConstraints content should use the constraints provided " +
                "via BoxWithConstraintsScope",
            "The `content` lambda in BoxWithConstraints has a scope " +
                "which will include the incoming constraints. If this " +
                "scope is ignored, then the cost of subcomposition is being wasted and " +
                "this BoxWithConstraints should be replaced with a Box.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                BoxWithConstraintsDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
