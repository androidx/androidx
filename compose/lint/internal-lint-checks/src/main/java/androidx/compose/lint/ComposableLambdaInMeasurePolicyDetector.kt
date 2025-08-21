/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.findParentInFile
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.uast.UCallExpression

/**
 * Lint [Detector] to ensure that we are not recreating lambdas on subcomposition remeasure.
 *
 * `@Composable` lambdas provided to a `subcompose` call cannot be automatically optimized by the
 * Compose compiler since they are created outside of the composition. This causes those lambdas to
 * be recreated on each remeasure, causing performance issues.
 *
 * Ideally, we should create all content lambdas in composition and them use the same instance in
 * remeasure, so that `subcompose` and inner composition can noop.
 *
 * For example:
 * ```
 * SubcomposeLayout { constraints ->
 *   val topBarPlaceable =
 *     subcompose(ScaffoldLayoutContent.TopBar) { Box { topBar() } }
 * }
 * ```
 *
 * Can be outlined to:
 * ```
 * val topBarContent: @Composable () -> Unit = remember(topBar) { { Box { topBar() } } }
 *
 * SubcomposeLayout { constraints ->
 *   val topBarPlaceable =
 *     subcompose(ScaffoldLayoutContent.TopBar, topBarContent)
 * }
 * ```
 */
class ComposableLambdaInMeasurePolicyDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("subcompose")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (
            !context.evaluator.isMemberInClass(
                method,
                "androidx.compose.ui.layout.SubcomposeMeasureScope",
            )
        )
            return

        // Find the content argument, which we'll assume is the first @Composable argument.
        val sourcePsi = node.sourcePsi as? KtElement ?: return
        val contentArgument =
            analyze(sourcePsi) {
                sourcePsi
                    .resolveToCall()
                    ?.singleFunctionCallOrNull()
                    ?.argumentMapping
                    ?.filter { it.value.symbol.returnType.isComposable() }
                    ?.keys
                    ?.firstOrNull()
            } ?: return

        // If the content argument is defined as an inline lambda, report the incident at that
        // location. Otherwise, check whether the lambda is defined within the scope of a measure
        // policy and report the incident at the definition.
        val incidentLocation =
            contentArgument as? KtLambdaExpression
                ?: contentArgument.findDeclarationWithinMeasurePolicy()
                ?: return

        val incident =
            Incident(context)
                .issue(ISSUE)
                .location(context.getLocation(incidentLocation))
                .message("Creating a subcompose content lambda inside a measure policy")
                .scope(incidentLocation)
        context.report(incident)
    }

    private fun KtExpression.findDeclarationWithinMeasurePolicy(): PsiElement? {
        val expr = this as? KtNameReferenceExpression ?: return null
        return analyze(expr) {
            (expr.reference as? KtReference)?.resolveToSymbol()?.psi?.takeIf { declaration ->
                declaration.findParentInFile { parent ->
                    (parent as? KtCallExpression)
                        ?.resolveToCall()
                        ?.singleFunctionCallOrNull()
                        ?.symbol
                        ?.callableId
                        ?.asSingleFqName()
                        ?.toString() == "androidx.compose.ui.layout.SubcomposeLayout"
                } != null
            }
        }
    }

    private fun KaType.isComposable(): Boolean =
        annotations.any { annotation ->
            annotation.classId?.asFqNameString() == Names.Runtime.Composable.javaFqn
        }

    companion object {
        private const val Explanation =
            "Composable lambdas which have been created outside of composition cannot be " +
                "automatically optimized by the Compose compiler. This causes the lambdas to be " +
                "recreated on each remeasure, causing performance issues. Instead, create all " +
                "composable lambdas in composition (thus outside of the measure policy) and use " +
                "those instances in remeasure."

        val ISSUE =
            Issue.create(
                "ComposableLambdaInMeasurePolicy",
                "Creating a composable lambda during measurement",
                Explanation,
                Category.PERFORMANCE,
                5,
                Severity.ERROR,
                Implementation(
                    ComposableLambdaInMeasurePolicyDetector::class.java,
                    Scope.JAVA_FILE_SCOPE,
                ),
            )
    }
}
