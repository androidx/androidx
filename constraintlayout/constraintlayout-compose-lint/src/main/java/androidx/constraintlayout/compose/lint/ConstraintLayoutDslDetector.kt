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
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType

private const val CREATE_REFS_FOR_NAME = "createRefsFor"
private const val MATCH_PARENT_NAME = "matchParent"
private const val LINK_TO_NAME = "linkTo"
private const val CENTER_TO_NAME = "centerTo"
private const val CENTER_HORIZONTALLY_TO_NAME = "centerHorizontallyTo"
private const val CENTER_VERTICALLY_TO_NAME = "centerVerticallyTo"
private const val WIDTH_NAME = "width"
private const val HEIGHT_NAME = "height"
private const val PARENT_NAME = "parent"

private const val CL_COMPOSE_DIMENSION_CLASS_NAME = "Dimension"

private const val DIMENSION_MATCH_PARENT_EXPRESSION_NAME =
    "$CL_COMPOSE_DIMENSION_CLASS_NAME.$MATCH_PARENT_NAME"

private const val CL_COMPOSE_PACKAGE = "androidx.constraintlayout.compose"
private const val CONSTRAINT_SET_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.ConstraintSetScope"
private const val MOTION_SCENE_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.MotionSceneScope"
private const val CONSTRAIN_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.ConstrainScope"

private val knownOwnersOfCreateRefsFor by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        CONSTRAINT_SET_SCOPE_CLASS_FQ,
        MOTION_SCENE_SCOPE_CLASS_FQ
    )
}

private val horizontalConstraintAnchors by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        "start",
        "end",
        "absoluteLeft",
        "absoluteRight"
    )
}

private val verticalConstraintAnchors by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        "top",
        "bottom",
    )
}

private val horizontalCenterMethodNames by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        CENTER_TO_NAME,
        CENTER_HORIZONTALLY_TO_NAME,
    )
}

private val verticalCenterMethodNames by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        CENTER_TO_NAME,
        CENTER_VERTICALLY_TO_NAME,
    )
}

class ConstraintLayoutDslDetector : Detector(), SourceCodeScanner {

    // TODO: Add a case to detect use cases for `ConstrainedLayoutReference.withChainParams()`

    override fun getApplicableUastTypes() =
        listOf(UCallExpression::class.java, UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        /**
         * Binary expressions are of the form `foo = bar`.
         */
        override fun visitBinaryExpression(node: UBinaryExpression) {
            val assignedReferenceText = node.rightOperand.sourcePsi?.text ?: return

            when (assignedReferenceText) {
                DIMENSION_MATCH_PARENT_EXPRESSION_NAME -> detectMatchParentUsage(node)
            }
        }

        override fun visitCallExpression(node: UCallExpression) {
            when (node.methodName) {
                CREATE_REFS_FOR_NAME -> detectCreateRefsForUsage(node)
            }
        }

        /**
         * Verify correct usage of `Dimension.matchParent`.
         *
         * &nbsp;
         *
         * When using `Dimension.matchParent`, the user must be careful to not have custom
         * constraints that result in different behavior from `centerTo(parent)`, otherwise, they
         * should use `Dimension.percent(1f)` instead.
         * ```
         *  val (text, button) = createRefsFor("text", "button")
         *  constrain(text) {
         *      width = Dimension.matchParent
         *
         *      // Correct
         *      start.linkTo(parent.start)
         *      centerTo(parent)
         *      centerHorizontallyTo(parent)
         *
         *      // Incorrect
         *      start.linkTo(parent.end)
         *      start.linkTo(button.start)
         *      centerHorizontallyTo(button)
         *      centerTo(button)
         *  }
         * ```
         *
         */
        private fun detectMatchParentUsage(node: UBinaryExpression) {
            val assigneeNode = node.leftOperand
            val assigneeName = assigneeNode.sourcePsi?.text ?: return

            // Must be assigned to either `width` or `height`
            val isHorizontal: Boolean = when (assigneeName) {
                WIDTH_NAME -> true
                HEIGHT_NAME -> false
                else -> return
            }

            // Verify that the context of this Expression is within ConstrainScope
            if (assigneeNode.tryResolveUDeclaration()
                    ?.getContainingUClass()?.qualifiedName != CONSTRAIN_SCOPE_CLASS_FQ
            ) {
                return
            }

            val containingBlock = node.getParentOfType<UBlockExpression>() ?: return

            // Within the Block, look for expressions supported for the check and immediately return
            // if any of those expressions indicate bad usage of `Dimension.matchParent`
            val containsErrorProneUsage = containingBlock.expressions.asSequence()
                .mapNotNull { expression ->
                    EvaluateableExpression.createForMatchParentUsage(
                        expression = expression,
                        isHorizontal = isHorizontal
                    )
                }
                .any(EvaluateableExpression::isErrorProneForMatchParentUsage)

            if (!containsErrorProneUsage) {
                return
            }

            val overrideMethodName =
                if (isHorizontal) CENTER_HORIZONTALLY_TO_NAME else CENTER_VERTICALLY_TO_NAME

            context.report(
                issue = IncorrectMatchParentUsageIssue,
                scope = node.rightOperand,
                location = context.getNameLocation(node.rightOperand),
                message = "`Dimension.matchParent` will override constraints to an equivalent of " +
                    "`$overrideMethodName(parent)`.\nUse `Dimension.percent(1f)` to respect " +
                    "constraints.",
                quickfixData = LintFix.create()
                    .replace()
                    .name("Replace `matchParent` with `percent(1f)`.")
                    .range(context.getNameLocation(node.rightOperand))
                    .all()
                    .with("Dimension.percent(1f)")
                    .autoFix()
                    .build()
            )
        }

        /**
         * Verify correct usage of `createRefsFor("a", "b", "c")`.
         *
         * &nbsp;
         *
         * The number of assigned variables should match the number of given arguments:
         * ```
         * // Correct
         * val (a, b, c) = createRefsFor("a", "b", "c")
         *
         * // Incorrect: Fewer variables than arguments
         * val (a) = createRefsFor("a", "b", "c")
         *
         * // Incorrect: More variables than arguments
         * val (a, b, c, d) = createRefsFor("a", "b", "c")
         *
         * ```
         */
        private fun detectCreateRefsForUsage(node: UCallExpression) {
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

        val IncorrectMatchParentUsageIssue = Issue.create(
            id = "IncorrectMatchParentUsage",
            briefDescription = "Prefer using `Dimension.percent(1f)` when defining custom " +
                "constraints.",
            explanation = "`Dimension.matchParent` forces the constraints to be an equivalent of " +
                "`centerHorizontallyTo(parent)` or `centerVerticallyTo(parent)` according to the " +
                "assigned dimension which can lead to unexpected behavior. To avoid that, prefer " +
                "using `Dimension.percent(1f)`",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                ConstraintLayoutDslDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}

internal class EvaluateableExpression(
    private val expectedArgumentText: String,
    private val expression: UCallExpression
) {
    /**
     * Should only return `true` when we know for certain that there's wrong usage.
     *
     * &nbsp;
     *
     * E.g.: For the following snippet we can't know if usage is incorrect since we don't know what
     * the variable `targetAnchor` represents.
     * ```
     * width = Dimension.matchParent
     *
     * var targetAnchor: HorizontalAnchor
     * start.linkTo(targetAnchor)
     * ```
     */
    fun isErrorProneForMatchParentUsage(): Boolean {
        val argumentText = expression.valueArguments.firstOrNull()?.sourcePsi?.text ?: return false
        return argumentText != expectedArgumentText
    }

    companion object {
        fun createForMatchParentUsage(
            expression: UExpression,
            isHorizontal: Boolean
        ): EvaluateableExpression? {
            if (expression is UQualifiedReferenceExpression) {
                // For the form of `start.linkTo(parent.start)`

                val callExpression = (expression.selector as? UCallExpression) ?: return null
                if (callExpression.methodName != LINK_TO_NAME) {
                    return null
                }
                val receiverAnchorName = expression.receiver.sourcePsi?.text ?: return null
                val supportedAnchors =
                    if (isHorizontal) horizontalConstraintAnchors else verticalConstraintAnchors
                if (!supportedAnchors.contains(receiverAnchorName)) {
                    return null
                }
                return EvaluateableExpression(
                    expectedArgumentText = "$PARENT_NAME.$receiverAnchorName",
                    expression = callExpression
                )
            } else if (expression is UCallExpression) {
                // For the form of `centerTo(parent)`

                val supportedMethodNames =
                    if (isHorizontal) horizontalCenterMethodNames else verticalCenterMethodNames
                val methodName = expression.methodName ?: return null
                if (!supportedMethodNames.contains(methodName)) {
                    return null
                }
                return EvaluateableExpression(
                    expectedArgumentText = PARENT_NAME,
                    expression = expression
                )
            }
            return null
        }
    }
}