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

package androidx.room.compiler.processing.ksp

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

/**
 * Returns the type of a property as if it is member of the given [ksType].
 */
internal fun KSPropertyDeclaration.typeAsMemberOf(ksType: KSType?): KSType {
    val resolved = type.resolve()
    if (isStatic()) {
        // calling as member with a static would throw as it might be a member of the companion
        // object
        return resolved
    }
    if (ksType == null) {
        return resolved
    }
    // see: https://github.com/google/ksp/issues/107
    // as member of might lose the `isError` information hence we should check before calling
    // asMemberOf.
    if (resolved.isError) {
        return resolved
    }
    return this.asMemberOf(
        containing = ksType
    )
}

internal fun KSValueParameter.typeAsMemberOf(
    functionDeclaration: KSFunctionDeclaration,
    ksType: KSType?
): KSType {
    val resolved = type.resolve()
    if (functionDeclaration.isStatic()) {
        // calling as member with a static would throw as it might be a member of the companion
        // object
        return resolved
    }
    if (resolved.isError) {
        // see: https://github.com/google/ksp/issues/107
        // as member of might lose the `isError` information hence we should check before calling
        // asMemberOf.
        return resolved
    }
    if (ksType == null) {
        return resolved
    }
    val asMember = functionDeclaration.asMemberOf(
        containing = ksType
    )
    // TODO b/173224718
    // this is counter intuitive, we should remove asMemberOf from method parameters.
    val myIndex = functionDeclaration.parameters.indexOf(this)
    return asMember.parameterTypes[myIndex] ?: resolved
}

internal fun KSFunctionDeclaration.returnTypeAsMemberOf(
    ksType: KSType?
): KSType {
    val resolved = returnType?.resolve()
    return when {
        resolved == null -> null
        ksType == null -> resolved
        resolved.isError -> resolved
        isStatic() -> {
            // calling as member with a static would throw as it might be a member of the companion
            // object
            resolved
        }
        else -> this.asMemberOf(
            containing = ksType
        ).returnType
    } ?: error("cannot find return type for $this")
}
