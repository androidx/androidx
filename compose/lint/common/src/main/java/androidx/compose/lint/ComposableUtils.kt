/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.lint

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingDeclaration
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.withContainingElements

/**
 * Returns whether this [UExpression] is directly invoked within the body of a Composable function
 * or lambda without being `remember`ed.
 */
fun UExpression.isNotRemembered(): Boolean = isNotRememberedWithKeys()

/**
 * Returns whether this [UExpression] is directly invoked within the body of a Composable function
 * or lambda without being `remember`ed, or whether it is invoked inside a `remember call without
 * the provided [keys][keyClassNames].
 * - Returns true if this [UExpression] is directly invoked inside a Composable function or lambda
 *   without being `remember`ed
 * - Returns true if this [UExpression] is invoked inside a call to `remember`, but without all of
 *   the provided [keys][keyClassNames] being used as key parameters to `remember`
 * - Returns false if this [UExpression] is correctly `remember`ed with the provided
 *   [keys][keyClassNames], or is not called inside a `remember` block, and is not called inside a
 *   Composable function or lambda
 *
 * @param keyClassNames [Name]s representing the expected classes that should be used as a key
 *   parameter to the `remember` call
 */
fun UExpression.isNotRememberedWithKeys(vararg keyClassNames: Name): Boolean {
    val visitor = ComposableBodyVisitor(this)
    // The nearest method or lambda expression that contains this call expression
    val boundaryElement = visitor.parentUElements().last()
    // Check if the nearest lambda expression is actually a call to remember
    val rememberCall: UCallExpression? =
        (boundaryElement.uastParent as? UCallExpression)?.takeIf {
            it.methodName == Names.Runtime.Remember.shortName &&
                it.resolve()?.isInPackageName(Names.Runtime.PackageName) == true
        }
    return if (rememberCall == null) {
        visitor.isComposable()
    } else {
        val parameterTypes =
            rememberCall.valueArguments.mapNotNull { it.getExpressionType()?.canonicalText }
        !keyClassNames.all { parameterTypes.contains(it.javaFqn) }
    }
}

/**
 * Returns whether this [UExpression] is invoked within the body of a Composable function or lambda.
 *
 * This searches parent declarations until we find a lambda expression or a function, and looks to
 * see if these are Composable.
 */
fun UExpression.isInvokedWithinComposable(): Boolean {
    return ComposableBodyVisitor(this).isComposable()
}

/** Returns whether this method is @Composable or not */
val PsiMethod.isComposable
    get() = hasAnnotation(Names.Runtime.Composable.javaFqn)

/** Returns whether this variable's type is @Composable or not */
val UVariable.isComposable: Boolean
    get() {
        // Annotation on the lambda
        val annotationOnLambda =
            when (val initializer = uastInitializer) {
                is ULambdaExpression -> {
                    val source = initializer.sourcePsi
                    if (source is KtFunction) {
                        // Anonymous function, val foo = @Composable fun() {}
                        source.hasComposableAnnotation
                    } else {
                        // Lambda, val foo = @Composable {}
                        initializer.findAnnotation(Names.Runtime.Composable.javaFqn) != null
                    }
                }
                else -> false
            }
        // Annotation on the type, foo: @Composable () -> Unit = { }
        val annotationOnType = typeReference?.isComposable == true
        return annotationOnLambda || annotationOnType
    }

/** Returns whether this annotated type or declaration is marked with @Composable or not */
val KaAnnotated.isComposable
    get() = annotations.any { annotation -> annotation.classId == Names.Runtime.Composable.classId }

/** Returns whether this lambda expression is @Composable or not */
val ULambdaExpression.isComposable: Boolean
    get() =
        when (val lambdaParent = uastParent) {
            // Function call with a lambda parameter
            is UCallExpression -> {
                val enclosingCallSource =
                    lambdaParent.sourcePsi as? KtCallExpression ?: return false
                val lambdaSource = sourcePsi
                analyze(enclosingCallSource) {
                    val functionCall =
                        enclosingCallSource.resolveToCall()?.singleFunctionCallOrNull()
                    val lambdaParameterSymbol = functionCall?.argumentMapping[lambdaSource]
                    lambdaParameterSymbol?.symbol?.returnType?.isComposable == true
                }
            }
            // A local / non-local lambda variable
            is UVariable -> {
                lambdaParent.isComposable
            }
            // Either a new UAST type we haven't handled, or non-Kotlin declarations
            else -> false
        }

/**
 * Helper class that visits parent declarations above the provided [expression], until it finds a
 * lambda or method. This 'boundary' is used as the indicator for whether this [expression] can be
 * considered to be inside a Composable body or not.
 *
 * @see isComposable
 * @see parentUElements
 */
private class ComposableBodyVisitor(private val expression: UExpression) {
    /** @return whether the body can be considered Composable or not */
    fun isComposable(): Boolean =
        when (val element = parentUElements.last()) {
            is UMethod -> element.isComposable
            is ULambdaExpression -> element.isComposable
            else -> false
        }

    /** Returns all parent [UElement]s until and including the boundary lambda / method. */
    fun parentUElements() = parentUElements

    /**
     * The outermost UElement that corresponds to the surrounding UDeclaration that contains
     * [expression], with the following special cases:
     * - if the containing UDeclaration is a local property, we ignore it and search above as it
     *   still could be created in the context of a Composable body
     * - if the containing UDeclaration is an anonymous class (object { }), we ignore it and search
     *   above as it still could be created in the context of a Composable body
     */
    private val boundaryUElement by lazy {
        // The nearest property / function / etc declaration that contains this call expression
        var containingDeclaration = expression.getContainingDeclaration()

        fun UDeclaration.isLocalProperty() = (sourcePsi as? KtProperty)?.isLocal == true
        fun UDeclaration.isAnonymousClass() = this is UAnonymousClass
        fun UDeclaration.isPropertyInsideAnonymousClass() =
            getContainingUClass()?.isAnonymousClass() == true

        while (
            containingDeclaration != null &&
                (containingDeclaration.isLocalProperty() ||
                    containingDeclaration.isAnonymousClass() ||
                    containingDeclaration.isPropertyInsideAnonymousClass())
        ) {
            containingDeclaration = containingDeclaration.getContainingDeclaration()
        }

        containingDeclaration
    }

    private val parentUElements by lazy {
        val elements = mutableListOf<UElement>()

        // Look through containing elements until we find a lambda or a method
        for (element in expression.withContainingElements) {
            elements += element
            when (element) {
                // Stop when we reach the parent declaration to avoid escaping the scope.
                boundaryUElement -> break
                is ULambdaExpression -> {
                    // Calls to inline functions (with inlined lambdas) don't affect the
                    // composability / do not count as a boundary to determine composability. We
                    // ignore calls to @Composable inline functions, as this will catch functions
                    // such as `remember` which although inline, do change the semantics of the code
                    // inside. We could instead check for the presence of
                    // `@DisallowComposableCalls`, but this will not be commonly used by external
                    // library code, so checking for @Composable is safer.
                    // We ignore noinline and crossinline lambdas, as these can be executed in a
                    // different context - this is the same behavior as the compose compiler, which
                    // disallows composable calls inside noinline and crossinline lambda parameters.
                    // We could additionally check for the presence of:
                    // ```
                    // contract {
                    //     callsInPlace(...)
                    // }
                    // ```
                    // to ensure that these lambdas are being called in place, but kotlin
                    // contracts are experimental, and are not consistently used in library
                    // code. Ignoring noinline and crossinline should be enough to avoid false
                    // positives.
                    if (element.couldBecomeComposableLambdaViaInlining()) {
                        // Do not treat inlined lambdas as a boundary, continue upwards
                    } else {
                        break
                    }
                }
                is UMethod -> break
            }
        }
        elements
    }
}

val PsiType.hasComposableAnnotation: Boolean
    get() = hasAnnotation(Names.Runtime.Composable.javaFqn)

/** Returns whether this type reference is @Composable or not */
val UTypeReferenceExpression.isComposable: Boolean
    get() {
        if (type.hasComposableAnnotation) return true

        // Annotations on the types of local properties (val foo: @Composable () -> Unit = {})
        // are currently not present on the PsiType, we so need to manually check the underlying
        // type reference. (https://youtrack.jetbrains.com/issue/KTIJ-18821)
        return (sourcePsi as? KtTypeReference)?.hasComposableAnnotation == true
    }

/** Returns whether this annotated declaration has a Composable annotation */
private val KtAnnotated.hasComposableAnnotation: Boolean
    get() =
        annotationEntries.any {
            (it.toUElement() as UAnnotation).qualifiedName == Names.Runtime.Composable.javaFqn
        }

/**
 * For a function invocation of the shape `fun <T> foo(..., () -> T): T`, this function returns
 * whether an invocation returns Unit. Specifically, this function returns true if `T is Unit` or if
 * the return type of the lambda in the final parameter returns `Unit`.
 */
fun isReallyRememberingUnit(node: UCallExpression, method: PsiMethod): Boolean =
    when {
        node.typeArguments.singleOrNull()?.isVoidOrUnit == true -> {
            // Call with an explicit type argument, e.g., retain<Unit> { 42 }
            true
        }
        node.sourcePsi is KtCallExpression -> {
            // Even though the return type is Unit, we should double check if the type of
            // the lambda expression matches
            @Suppress("UnstableApiUsage")
            val calculationParameterIndex = method.parameters.lastIndex
            val argument = node.getArgumentForParameter(calculationParameterIndex)?.sourcePsi
            // If the argument is a lambda, check the expression inside
            if (argument is KtLambdaExpression) {
                val lastExp = argument.bodyExpression?.statements?.lastOrNull()
                val lastExpType = lastExp?.toUElementOfType<UExpression>()?.getExpressionType()
                // If unresolved (i.e., type error), the expression type will be actually
                // `null`
                node.getExpressionType() == lastExpType
            } else {
                // Otherwise return true, since it is a reference to something else that is
                // unit (such as a variable)
                true
            }
        }
        else -> true
    }

/**
 * @return `true` if this lambda expression is passed as a parameter to a non-Composable inline
 *   function, and the corresponding lambda parameter is _not_ marked `noinline` or `crossinline`.
 *
 * For example:
 *
 * inline fun foo(block: () -> Unit) {} => true
 *
 * inline fun foo(crossinline block: () -> Unit) {} => false
 *
 * inline fun foo(noinline block: () -> Unit) {} => false
 *
 * @Composable inline fun foo(block: () -> Unit) {} => false
 *
 * fun foo(block: () -> Unit) {} => false
 */
private fun ULambdaExpression.couldBecomeComposableLambdaViaInlining(): Boolean {
    val callExpression = uastParent as? UCallExpression
    val ktCallExpression = callExpression?.sourcePsi as? KtCallExpression

    // null if this is a lambda variable
    if (ktCallExpression != null) {
        analyze(ktCallExpression) {
            ktCallExpression.resolveToCall()?.singleFunctionCallOrNull()?.let { functionCall ->
                (functionCall.symbol as? KaNamedFunctionSymbol)?.let { functionSymbol ->
                    val lambdaParameterSymbol = functionCall.argumentMapping[sourcePsi]
                    val isNoinline = lambdaParameterSymbol?.symbol?.isNoinline == true
                    val isCrossinline = lambdaParameterSymbol?.symbol?.isCrossinline == true
                    return !functionSymbol.isComposable &&
                        functionSymbol.isInline &&
                        !isNoinline &&
                        !isCrossinline
                }
            }
        }
    }
    return false
}
