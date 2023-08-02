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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.UastVisitor

private const val CREATE_REFS_FOR_NAME = "createRefsFor"
private const val MATCH_PARENT_NAME = "matchParent"
private const val LINK_TO_NAME = "linkTo"
private const val CENTER_TO_NAME = "centerTo"
private const val CENTER_HORIZONTALLY_TO_NAME = "centerHorizontallyTo"
private const val CENTER_VERTICALLY_TO_NAME = "centerVerticallyTo"
private const val WIDTH_NAME = "width"
private const val HEIGHT_NAME = "height"
private const val PARENT_NAME = "parent"
private const val CONSTRAIN_NAME = "constrain"
private const val MARGIN_INDEX_IN_LINK_TO = 1
private const val GONE_MARGIN_INDEX_IN_LINK_TO = 2
private const val CREATE_HORIZONTAL_CHAIN_NAME = "createHorizontalChain"
private const val CREATE_VERTICAL_CHAIN_NAME = "createVerticalChain"
private const val WITH_CHAIN_PARAMS_NAME = "withChainParams"
private const val CHAIN_PARAM_START_MARGIN_NAME = "startMargin"
private const val CHAIN_PARAM_TOP_MARGIN_NAME = "topMargin"
private const val CHAIN_PARAM_END_MARGIN_NAME = "endMargin"
private const val CHAIN_PARAM_BOTTOM_MARGIN_NAME = "bottomMargin"
private const val CHAIN_PARAM_START_GONE_MARGIN_NAME = "startGoneMargin"
private const val CHAIN_PARAM_TOP_GONE_MARGIN_NAME = "topGoneMargin"
private const val CHAIN_PARAM_END_GONE_MARGIN_NAME = "endGoneMargin"
private const val CHAIN_PARAM_BOTTOM_GONE_MARGIN_NAME = "bottomGoneMargin"

private const val CL_COMPOSE_DIMENSION_CLASS_NAME = "Dimension"

private const val DIMENSION_MATCH_PARENT_EXPRESSION_NAME =
    "$CL_COMPOSE_DIMENSION_CLASS_NAME.$MATCH_PARENT_NAME"

private const val CL_COMPOSE_PACKAGE = "androidx.constraintlayout.compose"
private const val CONSTRAINT_SET_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.ConstraintSetScope"
private const val MOTION_SCENE_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.MotionSceneScope"
private const val CONSTRAIN_SCOPE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.ConstrainScope"
private const val CONSTRAINED_LAYOUT_REFERENCE_CLASS_FQ =
    "$CL_COMPOSE_PACKAGE.ConstrainedLayoutReference"
private const val LAYOUT_REFERENCE_CLASS_FQ = "$CL_COMPOSE_PACKAGE.LayoutReference"

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
                CREATE_HORIZONTAL_CHAIN_NAME -> detectChainParamsUsage(node, true)
                CREATE_VERTICAL_CHAIN_NAME -> detectChainParamsUsage(node, false)
                // TODO: Detect that `withChainParams` is not called after chains are created
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

        /**
         * Verify that margins for chains are applied correctly.
         *
         * &nbsp;
         *
         * Margins for elements in chains should be applied with `LayoutReference.withChainParams()`
         * instinctively, users may want to create constraints with margins that mimic the chain
         * behavior expecting those margins to be reflected in the chain. But that is not the
         * correct way to do it.
         *
         * So this check detects when users create chain-like constraints, and suggests to use
         * `withChainParams()` and delete conflicting constraints, keeping the intended margins.
         *
         * &nbsp;
         *
         * Example:
         *
         * Before
         * ```
         * val (button, text) = createRefs()
         * createHorizontalChain(button, text)
         *
         * constrain(button) {
         *  end.linkTo(text.start, 8.dp)
         *  end.linkTo(text.start, goneMargin = 16.dp)
         * }
         * ```
         *
         * After
         * ```
         * val (button, text) = createRefs()
         * createHorizontalChain(button.withChainParams(endMargin = 8.dp, endGoneMargin = 16.dp), text)
         *
         * constrain(button) {
         * }
         * ```
         */
        private fun detectChainParamsUsage(node: UCallExpression, isHorizontal: Boolean) {
            // TODO(b/268213648): Don't attempt to fix chain elements that already have
            //  `withChainParams` that may have been defined elsewhere out of scope. A safe
            //  path to take would be to look for the layout reference declaration, and skip this
            //  check if it cannot be found within the current scope (code block). We could also try
            //  to search within the shared scope of the layout reference and chain declarations,
            //  but there's no straight-forward way to do it.

            val containingBlock = node.getParentOfType<UBlockExpression>() ?: return

            var previousNode: ChainNode? = null
            val chainNodes = node.valueArguments
                .filter(UExpression::isOfLayoutReferenceType)
                .mapNotNull { argumentExpression ->
                    argumentExpression.findChildIdentifier()?.let { identifier ->
                        val chainNode = ChainNode(
                            expression = identifier,
                            hasChainParams = argumentExpression is UQualifiedReferenceExpression ||
                                containingBlock.isChainParamsCalledInIdentifier(identifier)
                        )
                        previousNode?.let { prevNode ->
                            chainNode.prev = prevNode
                            prevNode.next = chainNode
                        }
                        previousNode = chainNode
                        chainNode
                    }
                }

            val resolvedChainLikeConstraintsPerNode = chainNodes.map {
                if (it.hasChainParams) {
                    emptyList()
                } else {
                    findChainLikeConstraints(containingBlock, it, isHorizontal)
                }
            }
            resolvedChainLikeConstraintsPerNode.forEachIndexed { index, chainLikeExpressions ->
                val chainParamsBuilder = ChainParamsMethodBuilder()
                val removeLinkToFixes = chainLikeExpressions.map { resolvedExpression ->
                    resolvedExpression.marginExpression?.let {
                        chainParamsBuilder.append(
                            resolvedExpression.marginParamName,
                            resolvedExpression.marginExpression
                        )
                    }
                    resolvedExpression.marginGoneExpression?.let {
                        chainParamsBuilder.append(
                            resolvedExpression.marginGoneParamName,
                            resolvedExpression.marginGoneExpression
                        )
                    }
                    val expressionToDelete =
                        resolvedExpression
                            .fullExpression
                            .getParentOfType<UQualifiedReferenceExpression>()
                    LintFix.create()
                        .replace()
                        .name("Remove conflicting `linkTo` declaration.")
                        .range(context.getLocation(expressionToDelete))
                        .all()
                        .with("")
                        .autoFix()
                        .build()
                }
                val chainNode = chainNodes[index]
                if (!chainParamsBuilder.isEmpty() && removeLinkToFixes.isNotEmpty()) {
                    context.report(
                        issue = IncorrectChainMarginsUsageIssue,
                        scope = node,
                        location = context.getLocation(chainNode.expression.sourcePsi),
                        message = "Margins for elements in a Chain should be applied with " +
                            "`LayoutReference.withChainParams(...)`.",
                        quickfixData = LintFix.create()
                            .composite()
                            .name(
                                "Add `.withChainParams(...)` and remove " +
                                    "(${removeLinkToFixes.size}) conflicting `linkTo` declarations."
                            )
                            // `join` might overwrite previously added fixes, so add grouped fixes
                            // first, then, add the remaining fixes individually with `add`
                            .join(*removeLinkToFixes.toTypedArray())
                            .add(
                                LintFix.create()
                                    .replace()
                                    .name("Add `.withChainParams(...)`.")
                                    .range(context.getLocation(chainNode.expression.sourcePsi))
                                    .end()
                                    .with(chainParamsBuilder.build())
                                    .autoFix()
                                    .build()
                            )
                            .build()
                    )
                }
            }
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

        val IncorrectChainMarginsUsageIssue = Issue.create(
            id = "IncorrectChainMarginsUsage",
            briefDescription = "Use `LayoutReference.withChainParams()` to define margins for " +
                "elements in a Chain.",
            explanation = "If you understand how a chain works, it might seem obvious to add " +
                "margins by re-creating the constraints with the desired margin. However, in " +
                "Compose, helpers will ignore custom constraints in favor of their layout " +
                "implementation. So instead, use `LayoutReference.withChainParams()` " +
                "to define margins for Chains.",
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

internal fun findChainLikeConstraints(
    constraintSetBlock: UBlockExpression,
    chainNode: ChainNode,
    isHorizontal: Boolean
): List<ResolvedChainLikeExpression> {
    val identifier = chainNode.expression
    val constrainTargetExpressions = constraintSetBlock.expressions.filter { cSetExpression ->
        cSetExpression is UCallExpression && cSetExpression.methodName == CONSTRAIN_NAME &&
            cSetExpression.valueArguments.any { argument ->
                argument.sourcePsi?.text == identifier.identifier
            }
    }

    val expectedAnchors =
        if (isHorizontal) horizontalConstraintAnchors else verticalConstraintAnchors

    return constrainTargetExpressions.asSequence().mapNotNull { constrainExpression ->
        (constrainExpression as? UCallExpression)
            ?.valueArguments
            ?.filterIsInstance<ULambdaExpression>()
            ?.lastOrNull()?.body as? UBlockExpression
    }.flatMap {
        it.expressions
    }.filterIsInstance<UQualifiedReferenceExpression>().map {
        it.selector
    }.filterIsInstance<UCallExpression>().filter {
        // No point in considering it if there's no margins applied
        it.methodName == LINK_TO_NAME && it.valueArgumentCount >= 2
    }.mapNotNull {
        it.receiver?.sourcePsi?.text?.let { anchorName ->
            if (expectedAnchors.contains(anchorName)) {
                Pair(it, anchorName)
            } else {
                null
            }
        }
    }.mapNotNull { (linkCallExpression, anchorName) ->
        val nextIdentifier = chainNode.next?.expression?.identifier
        val isNextParent = nextIdentifier == null

        val prevIdentifier = chainNode.prev?.expression?.identifier
        val isPrevParent = prevIdentifier == null
        val expectedNextAnchorTo =
            if (isNextParent) {
                "parent.$anchorName"
            } else {
                "${nextIdentifier!!}.${anchorName.getOppositeAnchorName()}"
            }
        val expectedPrevAnchorTo =
            if (isPrevParent) {
                "parent.$anchorName"
            } else {
                "${prevIdentifier!!}.${anchorName.getOppositeAnchorName()}"
            }

        val targetAnchorExpressionText =
            linkCallExpression.valueArguments[0].sourcePsi?.text
        if (targetAnchorExpressionText == expectedPrevAnchorTo ||
            targetAnchorExpressionText == expectedNextAnchorTo
        ) {
            ResolvedChainLikeExpression(
                linkCallExpression,
                anchorName,
                linkCallExpression.getArgumentForParameter(MARGIN_INDEX_IN_LINK_TO),
                linkCallExpression.getArgumentForParameter(GONE_MARGIN_INDEX_IN_LINK_TO)
            )
        } else {
            null
        }
    }.toList()
}

internal class ChainNode(
    val expression: USimpleNameReferenceExpression,
    val hasChainParams: Boolean
) {
    var prev: ChainNode? = null
    var next: ChainNode? = null
}

internal class ResolvedChainLikeExpression(
    val fullExpression: UCallExpression,
    anchorName: String,
    val marginExpression: UExpression?,
    val marginGoneExpression: UExpression?
) {
    val marginParamName: String = anchorName.asChainParamsArgument(false)
    val marginGoneParamName: String = anchorName.asChainParamsArgument(true)
}

private fun String.getOppositeAnchorName() =
    when (this) {
        "start" -> "end"
        "end" -> "start"
        "absoluteLeft" -> "absoluteRight"
        "absoluteRight" -> "absoluteLeft"
        "top" -> "bottom"
        "bottom" -> "top"
        else -> "start"
    }

internal fun String.asChainParamsArgument(isGone: Boolean = false) =
    if (!isGone) {
        when (this) {
            "absoluteLeft",
            "start" -> CHAIN_PARAM_START_MARGIN_NAME

            "absoluteRight",
            "end" -> CHAIN_PARAM_END_MARGIN_NAME

            "top" -> CHAIN_PARAM_TOP_MARGIN_NAME
            "bottom" -> CHAIN_PARAM_BOTTOM_MARGIN_NAME
            else -> CHAIN_PARAM_START_MARGIN_NAME
        }
    } else {
        when (this) {
            "absoluteLeft",
            "start" -> CHAIN_PARAM_START_GONE_MARGIN_NAME

            "absoluteRight",
            "end" -> CHAIN_PARAM_END_GONE_MARGIN_NAME

            "top" -> CHAIN_PARAM_TOP_GONE_MARGIN_NAME
            "bottom" -> CHAIN_PARAM_BOTTOM_GONE_MARGIN_NAME
            else -> CHAIN_PARAM_START_GONE_MARGIN_NAME
        }
    }

internal fun UBlockExpression.isChainParamsCalledInIdentifier(
    target: USimpleNameReferenceExpression
): Boolean {
    var found = false
    this.accept(
        object : UastVisitor {
            override fun visitQualifiedReferenceExpression(
                node: UQualifiedReferenceExpression
            ): Boolean {
                val identifier = (node.receiver as? USimpleNameReferenceExpression) ?: return true
                if (identifier.identifier == target.identifier &&
                    identifier.getExpressionType() == target.getExpressionType()) {
                    val selector = node.selector
                    if (selector is UCallExpression &&
                        selector.methodName == WITH_CHAIN_PARAMS_NAME) {
                        found = true
                    } else {
                        // skip
                        return true
                    }
                } else {
                    // skip
                    return true
                }
                return super.visitQualifiedReferenceExpression(node)
            }

            override fun visitElement(node: UElement): Boolean {
                return found
            }
        }
    )
    return found
}

internal class ChainParamsMethodBuilder {
    private val modificationMap = mutableMapOf<String, UExpression>()

    fun append(paramName: String, paramExpression: UExpression) {
        modificationMap[paramName] = paramExpression
    }

    fun isEmpty() = modificationMap.isEmpty()

    fun build(): String =
        StringBuilder().apply {
            append('.')
            append(WITH_CHAIN_PARAMS_NAME)
            append('(')
            modificationMap.forEach { (paramName, uExpression) ->
                uExpression.sourcePsi?.text?.let {
                    append("$paramName = $it")
                    append(", ")
                }
            }
            deleteCharAt(this.lastIndex)
            deleteCharAt(this.lastIndex)
            append(')')
        }.toString()
}

internal fun UExpression.findChildIdentifier(): USimpleNameReferenceExpression? {
    var identifier: USimpleNameReferenceExpression? = null
    this.accept(
        object : UastVisitor {
            override fun visitSimpleNameReferenceExpression(
                node: USimpleNameReferenceExpression
            ): Boolean {
                if (node.isOfLayoutReferenceType()) {
                    identifier = node
                }
                return true
            }

            // Only supported element to visit recursively, for the form of `textRef.withChainParams()`
            override fun visitQualifiedReferenceExpression(
                node: UQualifiedReferenceExpression
            ): Boolean = false

            override fun visitElement(node: UElement): Boolean = true
        }
    )
    return identifier
}

/**
 * Simple way to check if the Reference has a supported LayoutReference type. Note it does not do
 * expression resolution and takes the type as is. So we have to manually check for inheritors of
 * LayoutReference.
 */
internal fun UExpression.isOfLayoutReferenceType(): Boolean {
    val typeName = this.getExpressionType()?.canonicalText ?: return false
    return typeName == CONSTRAINED_LAYOUT_REFERENCE_CLASS_FQ ||
        typeName == LAYOUT_REFERENCE_CLASS_FQ
}
