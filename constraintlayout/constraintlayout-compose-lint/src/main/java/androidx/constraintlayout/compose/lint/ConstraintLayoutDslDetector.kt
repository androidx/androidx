/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression

private const val CL_COMPOSE_PACKAGE = "androidx.constraintlayout.compose"
private const val CONSTRAINT_SET_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.ConstraintSetScope"
private const val MOTION_SCENE_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.MotionSceneScope"
private const val CREATE_REFS_FOR_NAME = "createRefsFor"

class ConstraintLayoutDslDetector : Detector(), SourceCodeScanner {
    private val knownOwnersOfCreateRefsFor = setOf(
        CONSTRAINT_SET_SCOPE_CLASS_FQ,
        MOTION_SCENE_SCOPE_CLASS_FQ
    )

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodName != CREATE_REFS_FOR_NAME) {
                return
            }
            val destructuringDeclarationElement =
                node.sourcePsi?.getParentOfType<KtDestructuringDeclaration>(true) ?: return

            val argsGiven = node.valueArgumentCount
            val varsReceived = destructuringDeclarationElement.entries.size
            if (argsGiven == varsReceived) {
                // Ids provided to call match the variables assigned, no issue
                return
            }

            // Verify that arguments are Strings, we can't check for correctness if the argument is
            // an array: `val (text1, text2) = createRefsFor(*iDsArray)`
            node.valueArguments.forEach { argExpression ->
                if (argExpression.getExpressionType()?.canonicalText != String::class.java.name) {
                    return
                }
            }

            // Element resolution is relatively expensive, do last
            val classOwnerFqName = node.resolve()?.containingClass?.qualifiedName ?: return

            // Make sure the method corresponds to an expected class
            if (!knownOwnersOfCreateRefsFor.contains(classOwnerFqName)) {
                return
            }

            context.report(
                issue = IncorrectReferencesDeclarationIssue,
                scope = node,
                location = context.getNameLocation(node),
                message = "Arguments of `$CREATE_REFS_FOR_NAME` ($argsGiven) do not match " +
                    "assigned variables ($varsReceived)"
            )
        }
    }

    companion object {
        val IncorrectReferencesDeclarationIssue = Issue.create(
            id = "IncorrectReferencesDeclaration",
            briefDescription = "`$CREATE_REFS_FOR_NAME(vararg ids: Any)` should have at least one" +
                " argument and match assigned variables",
            explanation = "`$CREATE_REFS_FOR_NAME(vararg ids: Any)` conveniently allows creating " +
                "multiple references using destructuring. However, providing an un-equal amount " +
                "of arguments to the assigned variables will result in unexpected behavior since" +
                " the variables may reference a ConstrainedLayoutReference with unknown ID.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                ConstraintLayoutDslDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}