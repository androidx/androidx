/*
 * Copyright 2019 The Android Open Source Project
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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve

/**
 * Lint [Detector] to ensure that we are not creating extra lambdas just to emit already captured
 * lambdas inside Compose code. For example:
 * ```
 * val lambda = @Composable {}
 * Foo {
 *     lambda()
 * }
 * ```
 *
 * Can just be inlined to:
 * ```
 * Foo(lambda)
 * ```
 *
 * This helps avoid object allocation but more importantly helps us avoid extra code generation
 * around composable lambdas.
 */
class UnnecessaryLambdaCreationDetector : Detector(), SourceCodeScanner {
    override fun createUastHandler(context: JavaContext) = UnnecessaryLambdaCreationHandler(context)

    override fun getApplicableUastTypes() = listOf(ULambdaExpression::class.java)

    /**
     * This handler visits every lambda expression and reports an issue if the following criteria
     * (in order) hold true:
     * 1. There is only one expression inside the lambda
     * 2. The lambda literal is created as part of a function call, and not as a property assignment
     *    such as val foo = @Composable {}
     * 3. The expression is an invoke() call
     * 4. The receiver type of the invoke call is a functional type, and it is a subtype of (i.e
     *    compatible to cast to) the lambda parameter functional type
     * 5. The lambda parameter and literal have matching composability
     */
    class UnnecessaryLambdaCreationHandler(private val context: JavaContext) : UElementHandler() {

        override fun visitLambdaExpression(node: ULambdaExpression) {
            val expressions = (node.body as? UBlockExpression)?.expressions ?: return

            if (expressions.size != 1) return

            val expression =
                when (val expr = expressions.first().skipParenthesizedExprDown()) {
                    is UCallExpression -> expr
                    is UReturnExpression -> {
                        if (expr.sourcePsi == null) { // implicit return
                            expr.returnExpression?.skipParenthesizedExprDown() as? UCallExpression
                        } else null
                    }
                    else -> null
                } ?: return

            // This is the parent function call that contains the lambda expression.
            // I.e in Foo { bar() } this will be the call to `Foo`.
            // We want to make sure this lambda is being invoked in the context of a function call,
            // and not as a property assignment - so we cast to KotlinUFunctionCallExpression to
            // filter out such cases.
            val parentExpression = (node.uastParent as? UCallExpression) ?: return

            // If we can't resolve the parent call, then the parent function is defined in a
            // separate module, so we don't have the right metadata - and hence the argumentType
            // below will be Function0 even if in the actual source it has a scope. Return early to
            // avoid false positives.
            parentExpression.resolve() ?: return
            val resolved = expression.resolve() ?: return
            if (resolved.name != OperatorNameConventions.INVOKE.identifier) return

            // Return if the receiver of the lambda argument and the lambda itself don't match. This
            // happens if the functional types are different, for example a lambda with 0 parameters
            // (Function0) and a lambda with 1 parameter (Function1). Similarly for two lambdas
            // with 0 parameters, but one that has a receiver scope (SomeScope.() -> Unit).
            val expressionSourcePsi = expression.sourcePsi as? KtCallElement ?: return
            analyze(expressionSourcePsi) {
                val functionType = dispatchReceiverType(expressionSourcePsi) ?: return
                val argumentType = toLambdaFunctionalType(node) ?: return
                if (!(functionType.isSubtypeOf(argumentType))) return
            }

            val expectedComposable = node.isComposable

            // Try and get the UElement for the source of the lambda
            val sourcePsi = expression.sourcePsi as? KtCallElement ?: return
            val resolvedLambdaSource =
                sourcePsi.calleeExpression
                    ?.toUElement()
                    ?.tryResolve()
                    ?.toUElement()
                    // Sometimes the above will give us a method (representing the getter for a`
                    // property), when the actual backing element is a property. Going to the source
                    // and back should give us the actual UVariable we are looking for.
                    ?.sourcePsi
                    .toUElement()

            val isComposable =
                when (resolvedLambdaSource) {
                    is UVariable -> resolvedLambdaSource.isComposable
                    // If the source is a method, then the lambda is the return type of the method,
                    // so
                    // check the return type
                    is UMethod -> resolvedLambdaSource.returnTypeReference?.isComposable == true
                    // Safe return if we failed to resolve. This can happen for implicit `it`
                    // parameters
                    // that are lambdas, but this should only happen exceptionally for lambdas with
                    // an `Any` parameter, such as { any: Any -> }.let { it(Any()) }, since this
                    // passes
                    // the isSubTypeOf check above. In this case it isn't possible to inline this
                    // call,
                    // so no need to handle these implicit parameters.
                    null -> return
                    // Throw since this is an internal check, and we want to fix this for unknown
                    // types.
                    // If making this check public, it's safer to return instead without throwing.
                    else -> error(parentExpression.asSourceString())
                }

            if (isComposable != expectedComposable) return

            context.report(
                ISSUE,
                node,
                context.getNameLocation(expression as UElement),
                "Creating an unnecessary lambda to emit a captured lambda"
            )
        }
    }

    companion object {
        private const val Explanation =
            "Creating this extra lambda instead of just passing the already captured lambda means" +
                " that during code generation the Compose compiler will insert code around " +
                "this lambda to track invalidations. This adds some extra runtime cost so you" +
                " should instead just directly pass the lambda as a parameter to the function."

        val ISSUE =
            Issue.create(
                "UnnecessaryLambdaCreation",
                "Creating an unnecessary lambda to emit a captured lambda",
                Explanation,
                Category.PERFORMANCE,
                5,
                Severity.ERROR,
                Implementation(UnnecessaryLambdaCreationDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }
}

private fun KaSession.dispatchReceiverType(callElement: KtCallElement): KaFunctionType? =
    callElement
        .resolveToCall()
        ?.singleFunctionCallOrNull()
        ?.takeIf { it is KaSimpleFunctionCall && it.isImplicitInvoke }
        ?.partiallyAppliedSymbol
        ?.dispatchReceiver
        ?.type as? KaFunctionType

private fun KaSession.toLambdaFunctionalType(lambdaExpression: ULambdaExpression): KaFunctionType? {
    val sourcePsi = lambdaExpression.sourcePsi as? KtLambdaExpression ?: return null
    return sourcePsi.expressionType as? KaFunctionType
}
