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

package androidx.compose.compiler.plugins.kotlin.k2

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

// - DONE Composable suspend functions are forbidden
// - DONE Abstract composable functions may not have default arguments.
// - DONE Check overrides: composable functions can only override composable function
// - TODO Check that the scheme of a function matches its override (with applier inference?)
// - TODO Composable main functions are not allowed (is there a main function detector in FIR?)
//
// There are a number of now superfluous checks in the old frontend:
// FE1.0 also checks value parameters for composable function types, but these are
// are different types in FIR. Also we check that there are no composable suspend function
// types on the value parameters, but again, these are different types in FIR so this
// check is unnecessary.
object ComposableFunctionChecker : FirFunctionChecker() {
    override fun check(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val isComposable = declaration.hasComposableAnnotation(context.session)

        // Check overrides for mismatched composable annotations
        for (override in declaration.getDirectOverriddenFunctions(context)) {
            if (override.isComposable(context.session) != isComposable) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.CONFLICTING_OVERLOADS,
                    listOf(declaration.symbol, override),
                    context
                )
            }

            // TODO Check scheme of override against declaration
        }

        if (!isComposable) return

        // Composable suspend functions are unsupported
        if (declaration.isSuspend) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSABLE_SUSPEND_FUN, context)
        }

        // Check that there are no default arguments in abstract composable functions
        if (declaration.isAbstract) {
            for (valueParameter in declaration.valueParameters) {
                val defaultValue = valueParameter.defaultValue ?: continue
                reporter.reportOn(
                    defaultValue.source,
                    ComposeErrors.ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE,
                    context
                )
            }
        }

        // Composable main functions are not allowed.
        if (declaration.symbol.isMain(context.session)) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSABLE_FUN_MAIN, context)
        }

        // Disallow composable setValue operators
        if (declaration.isOperator
            && declaration.nameOrSpecialName == OperatorNameConventions.SET_VALUE) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSE_INVALID_DELEGATE, context)
        }
    }

    // TODO: Move everything past this point to FirUtils.kt

    // TODO: There's currently no proper MainFunctionDetector for FIR, so move this implementation upstream!
    private fun FirFunctionSymbol<*>.isMain(session: FirSession): Boolean {
        if (this !is FirNamedFunctionSymbol) return false
        if (typeParameterSymbols.isNotEmpty()) return false
        if (!resolvedReturnType.isUnit) return false
        if (jvmNameAsString(session) != "main") return false

        val parameterTypes = explicitParameterTypes
        when (parameterTypes.size) {
            0 -> {
                /*
                assert(DescriptorUtils.isTopLevelDeclaration(descriptor)) { "main without parameters works only for top-level" }
                val containingFile = DescriptorToSourceUtils.getContainingFile(descriptor)
                // We do not support parameterless entry points having JvmName("name") but different real names
                // See more at https://github.com/Kotlin/KEEP/blob/master/proposals/enhancing-main-convention.md#parameterless-main
                if (descriptor.name.asString() != "main") return false
                if (containingFile?.declarations?.any { declaration -> isMainWithParameter(declaration, checkJvmStaticAnnotation) } == true) {
                    return false
                }*/
            }
            1 -> {
                val type = parameterTypes.single()
                if (!type.isArrayType || type.typeArguments.size != 1) return false
                val elementType = type.typeArguments[0].takeIf { it.kind != ProjectionKind.IN }?.type
                    ?: return false
                if (!elementType.isString) return false
            }
            else -> return false
        }
        /*
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) return true

        val containingDeclaration = descriptor.containingDeclaration
        return containingDeclaration is ClassDescriptor
                && containingDeclaration.kind.isSingleton
                && (descriptor.hasJvmStaticAnnotation() || !checkJvmStaticAnnotation)
         */
        // TODO: Complete
        return true
    }

    private fun FirNamedFunctionSymbol.jvmNameAsString(session: FirSession): String =
        getAnnotationStringParameter(StandardClassIds.Annotations.JvmName, session)
            ?: name.asString()

    private val FirFunctionSymbol<*>.explicitParameterTypes: List<ConeKotlinType>
        get() = resolvedContextReceivers.map { it.typeRef.coneType } +
            listOfNotNull(receiverParameter?.typeRef?.coneType) +
            valueParameterSymbols.map {it.resolvedReturnType }

    private fun FirFunction.getDirectOverriddenFunctions(
        context: CheckerContext
    ): List<FirFunctionSymbol<*>> {
        if (!isOverride && (this as? FirPropertyAccessor)?.propertySymbol?.isOverride != true)
            return listOf()

        val scope = (containingClassLookupTag()
            ?.toSymbol(context.session) as? FirClassSymbol<*>)
            ?.unsubstitutedScope(context)
            ?: return listOf()

        return when (val symbol = symbol) {
            is FirNamedFunctionSymbol -> {
                scope.processFunctionsByName(symbol.name) {}
                scope.getDirectOverriddenFunctions(symbol, true)
            }
            is FirPropertyAccessorSymbol -> {
                scope.getDirectOverriddenProperties(symbol.propertySymbol, true).mapNotNull {
                    if (symbol.isGetter) it.getterSymbol else it.setterSymbol
                }
            }
            else -> listOf()
        }
    }
}
