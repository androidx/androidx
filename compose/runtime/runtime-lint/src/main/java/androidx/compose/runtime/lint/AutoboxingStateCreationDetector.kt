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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * Suggests alternative functions to mutableStateOf<T>() if all of the following are true:
 * - a snapshot mutation policy argument is not specified (or it is structural equivalent policy)
 * - `T` is in the [replacements] map
 * - `T` is a non-nullable type
 */
class AutoboxingStateCreationDetector : Detector(), SourceCodeScanner {

    /**
     * Map of canonical PSI types to the fully-qualified function that should be used to
     * create MutableState instances of said type.
     */
    private val replacements = mapOf(
        "kotlin.Int" to Names.Runtime.MutableIntStateOf,
        "kotlin.Long" to Names.Runtime.MutableLongStateOf,
        "kotlin.Float" to Names.Runtime.MutableFloatStateOf,
        "kotlin.Double" to Names.Runtime.MutableDoubleStateOf,
    )

    override fun getApplicableMethodNames() = listOf(Names.Runtime.MutableStateOf.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return

        val replacement = getSuggestedReplacementName(node) ?: return

        context.report(
            issue = AutoboxingStateCreation,
            scope = node,
            location = context.getNameLocation(node),
            message = "Prefer `${replacement.shortName}` instead of `${method.name}`",
            quickfixData = createLintFix(context, node, replacement)
        )
    }

    private fun createLintFix(
        context: JavaContext,
        node: UCallExpression,
        replacementFunction: Name
    ): LintFix {
        val fixes = listOfNotNull(
            // Replace the function name
            LintFix.create()
                .replace()
                .range(context.getNameLocation(node))
                .with(replacementFunction.javaFqn)
                .shortenNames(true)
                .build(),

            // Remove the type arguments list (if present)
            context.getLocationOfTypeArguments(node)
                ?.let { LintFix.create().replace().range(it).with("").build() },

            // Remove the SnapshotMutationPolicy argument (if present)
            context.getLocationOfArgumentsList(node)
                ?.takeIf { node.getArgumentForParameter(MUTATION_POLICY_PARAM_IDX) != null }
                ?.let { argsListLocation ->
                    node.getArgumentForParameter(VALUE_PARAM_IDX)?.sourcePsi?.text
                        ?.let { valueArg ->
                            LintFix.create()
                                .replace()
                                .range(argsListLocation)
                                .with("($valueArg)")
                                .build()
                        }
                }
        )

        return LintFix.create()
            .name("Replace with ${replacementFunction.shortName}")
            .composite(*fixes.toTypedArray())
    }

    private fun JavaContext.getLocationOfTypeArguments(node: UCallExpression): Location? {
        val typeArgsList = node.sourcePsi?.children?.firstIsInstanceOrNull<KtTypeArgumentList>()
            ?: return null
        return getLocation(typeArgsList)
    }

    private fun JavaContext.getLocationOfArgumentsList(node: UCallExpression): Location? {
        val argsList = node.sourcePsi?.children?.firstIsInstanceOrNull<KtValueArgumentList>()
            ?: return null
        return getLocation(argsList)
    }

    private fun getSuggestedReplacementName(
        invocation: UCallExpression
    ): Name? {
        if (!usesStructuralEqualityPolicy(invocation)) return null

        val sourcePsi = invocation.sourcePsi as? KtElement ?: return null
        analyze(sourcePsi) {
            val resolvedCall = sourcePsi.resolveCall()?.singleFunctionCallOrNull() ?: return null
            val stateType = resolvedCall.typeArgumentsMapping.asIterable().single().value
            return when {
                stateType.isMarkedNullable -> null
                else -> {
                    // NB: use expanded class symbol for type alias
                    val fqName = stateType.expandedClassSymbol?.classIdIfNonLocal?.asFqNameString()
                    replacements[fqName]
                }
            }
        }
    }

    private fun usesStructuralEqualityPolicy(
        invocation: UCallExpression
    ): Boolean {
        val policyExpr = invocation.valueArguments.getOrNull(MUTATION_POLICY_PARAM_IDX)
            ?.skipParenthesizedExprDown()
            ?: return true // No argument passed; we're using the default policy

        val policyMethod = (policyExpr as? UCallExpression)?.resolve()
            ?: return false // Argument isn't a direct function call. Assume it's a more complex
                            // policy, or isn't always the structural equality policy.

        return policyMethod.isInPackageName(Names.Runtime.PackageName) &&
            policyMethod.name == Names.Runtime.StructuralEqualityPolicy.shortName
    }

    companion object {
        private const val VALUE_PARAM_IDX = 0
        private const val MUTATION_POLICY_PARAM_IDX = 1

        val AutoboxingStateCreation = Issue.create(
            id = "AutoboxingStateCreation",
            briefDescription = "`State<T>` will autobox values assigned to this state. " +
                "Use a specialized state type instead.",
            explanation = "Calling `mutableStateOf<T>()` when `T` is either backed by a " +
                "primitive type on the JVM or is a value class results in a state implementation " +
                "that requires all state values to be boxed. This usually causes an additional " +
                "allocation for each state write, and adds some additional work to auto-unbox " +
                "values when reading the value of the state. Instead, prefer to use a " +
                "specialized primitive state implementation for `Int`, `Long`, `Float`, and " +
                "`Double` when the state does not need to track null values and does not " +
                "override the default `SnapshotMutationPolicy`.",
            category = Category.PERFORMANCE, priority = 3, severity = Severity.INFORMATIONAL,
            implementation = Implementation(
                AutoboxingStateCreationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}
