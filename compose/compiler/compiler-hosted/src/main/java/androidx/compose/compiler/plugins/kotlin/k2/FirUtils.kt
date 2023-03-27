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

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

fun FirAnnotationContainer.hasComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.Composable, session)

fun FirBasedSymbol<*>.hasComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.Composable, session)

fun FirAnnotationContainer.hasReadOnlyComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.ReadOnlyComposable, session)

fun FirBasedSymbol<*>.hasReadOnlyComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.ReadOnlyComposable, session)

fun FirAnnotationContainer.hasDisallowComposableCallsAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.DisallowComposableCalls, session)

fun FirCallableSymbol<*>.isComposable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasComposableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.let {
                it.hasComposableAnnotation(session) || it.isComposableDelegate(session)
            } ?: false
        else -> false
    }

fun FirCallableSymbol<*>.isReadOnlyComposable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasReadOnlyComposableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.hasReadOnlyComposableAnnotation(session) ?: false
        else -> false
    }

@OptIn(SymbolInternals::class)
private fun FirPropertyAccessorSymbol.isComposableDelegate(session: FirSession): Boolean {
    if (!propertySymbol.hasDelegate) return false
    return ((fir
        .body
        ?.statements
        ?.singleOrNull() as? FirReturnExpression)
        ?.result as? FirFunctionCall)
        ?.calleeReference
        ?.toResolvedCallableSymbol()
        ?.isComposable(session)
        ?: false
}
