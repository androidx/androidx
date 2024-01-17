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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

internal fun KSFunctionDeclaration.hasOverloads() = this.annotations.any {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlin.jvm.JvmOverloads"
}

/**
 * A custom ReturnType that return an [XType] while also resolving boxing if necessary (might happen
 * due to overrides).
 */
internal fun KSFunctionDeclaration.returnKspType(
    env: KspProcessingEnv,
    containing: KspType?
): KspType {
    return if (containing?.typeElement?.isAnnotationClass() == true) {
        // Calling #getOriginatingReference() or #returnTypeAsMemberOf() currently fails for
        // annotation classes due to https://github.com/google/ksp/issues/1004. Thus, we avoid
        // calling those methods and just return the return type directly. This should be safe for
        // annotation classes since they can't extend other types or use generics.
        val returnTypeReference = checkNotNull(returnType)
        env.wrap(
            originatingReference = returnTypeReference,
            ksType = returnTypeReference.resolve()
        )
    } else {
        env.wrap(
            originatingReference = checkNotNull(getOriginatingReference()),
            ksType = returnTypeAsMemberOf(ksType = containing?.ksType)
        )
    }
}

private fun KSFunctionDeclaration.getOriginatingReference(): KSTypeReference? {
    // b/160258066
    // we may need to box the return type if it is overriding a generic, hence, we should
    // use the declaration of the overridee if available when deciding nullability
    val overridee = this.findOverridee()
    // when a java method overrides a property, overridee might be a property instead
    // of a function.
    return when (overridee) {
        is KSFunctionDeclaration -> overridee.returnType
        is KSPropertyDeclaration -> {
            overridee.type
        }
        null -> null
        else -> error(
            """
            Unexpected overridee type for $this ($overridee).
            Please file a bug at $ISSUE_TRACKER_LINK.
            """.trimIndent()
        )
    } ?: returnType
}
