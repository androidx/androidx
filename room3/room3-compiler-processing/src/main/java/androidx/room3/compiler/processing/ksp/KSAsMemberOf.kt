/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room3.compiler.processing.ksp

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

/** Returns the type of a property as if it is member of the given [ksType]. */
internal fun KSPropertyDeclaration.typeAsMemberOf(ksType: KSType?): KSType {
    val resolved = type.resolve()
    return when {
        canSkipAsMemberOf() ||
            ksType == null ||
            // see: https://github.com/google/ksp/issues/107
            // asMemberOf might lose the `isError` information hence we should check before calling.
            resolved.isError -> resolved
        else -> asMemberOf(containing = ksType)
    }
}

internal fun KSValueParameter.typeAsMemberOf(
    functionDeclaration: KSFunctionDeclaration,
    ksType: KSType?,
    resolved: KSType = type.resolve(),
): KSType {
    return when {
        functionDeclaration.canSkipAsMemberOf() ||
            ksType == null ||
            // see: https://github.com/google/ksp/issues/107
            // asMemberOf might lose the `isError` information hence we should check before calling.
            resolved.isError -> resolved
        else -> {
            // TODO b/173224718
            //  this is counter intuitive, we should remove asMemberOf from method parameters.
            val myIndex = functionDeclaration.parameters.indexOf(this)
            functionDeclaration.asMemberOf(containing = ksType).parameterTypes[myIndex] ?: resolved
        }
    }
}

internal fun KSFunctionDeclaration.returnTypeAsMemberOf(ksType: KSType?): KSType {
    val resolved = returnType?.resolve() ?: error("cannot find return type for $this")
    return when {
        canSkipAsMemberOf() ||
            ksType == null ||
            // see: https://github.com/google/ksp/issues/107
            // asMemberOf might lose the `isError` information hence we should check before calling.
            resolved.isError -> resolved
        else ->
            asMemberOf(containing = ksType).returnType
                ?: error("cannot find return type for $this as member of $ksType")
    }
}

internal fun KSFunctionDeclaration.receiverTypeAsMemberOf(ksType: KSType?): KSType {
    val resolved = extensionReceiver?.resolve() ?: error("cannot find receiver type for $this")
    return when {
        canSkipAsMemberOf() ||
            ksType == null ||
            // see: https://github.com/google/ksp/issues/107
            // asMemberOf might lose the `isError` information hence we should check before calling.
            resolved.isError -> resolved
        else ->
            asMemberOf(containing = ksType).extensionReceiverType
                ?: error("cannot find receiver type for $this as member of $ksType")
    }
}

/**
 * Returns `true` if calling `asMemberOf` isn't required for the given declaration.
 *
 * In particular, java static declarations, top level declarations, and declarations within
 * object/companion object don't require asMemberOf since they can't be overridden. Skipping it
 * should be more efficient, and avoids cases where calling asMemberOf will throw if passed a
 * companion object (see: https://issuetracker.google.com/443342969).
 */
private fun KSDeclaration.canSkipAsMemberOf() =
    isJavaStatic() || isTopLevel() || isEnclosedInObject()
