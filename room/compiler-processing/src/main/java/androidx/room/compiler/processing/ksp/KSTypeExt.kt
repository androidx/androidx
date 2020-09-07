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

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.jetbrains.kotlin.ksp.symbol.KSClassifierReference
import org.jetbrains.kotlin.ksp.symbol.KSDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSName
import org.jetbrains.kotlin.ksp.symbol.KSType
import org.jetbrains.kotlin.ksp.symbol.KSTypeReference

internal val ERROR_PACKAGE_NAME = "androidx.room.compiler.processing.kotlin.error"

// catch-all type name when we cannot resolve to anything.
internal val UNDEFINED = ClassName.get(ERROR_PACKAGE_NAME, "Undefined")

/**
 * Turns a KSTypeReference into a TypeName
 *
 * We try to achieve this by first resolving it and iterating.
 * If some types cannot be resolved, we do a best effort name guess from the KSTypeReference's
 * element.
 */
internal fun KSTypeReference?.typeName(): TypeName {
    return if (this == null) {
        UNDEFINED
    } else {
        resolve()?.typeName() ?: fallbackClassName()
    }
}

private fun KSTypeReference.fallbackClassName(): ClassName {
    return (element as? KSClassifierReference)?.let {
        ClassName.bestGuess(it.referencedName())
    } ?: UNDEFINED
}

private fun KSName.typeName(): ClassName? {
    if (asString().isBlank()) {
        // fallback to reference
        return null
    }
    // TODO KSP currently do not model package names separate from the simple names.
    //  see: https://github.com/android/kotlin/issues/23
    val shortNames = getShortName().split(".")
    return ClassName.get(getQualifier(), shortNames.first(), *(shortNames.drop(1).toTypedArray()))
}

internal fun KSDeclaration.typeName(): ClassName? {
    // if there is no qualified name, it is an error for room
    val qualified = qualifiedName?.asString() ?: return null
    // get the package name first, it might throw for invalid types, hence we use safeGetPackageName
    val pkg = safeGetPackageName() ?: return null
    // using qualified name and pkg, figure out the short names.
    val shortNames = if (pkg == "") {
        qualified
    } else {
        qualified.substring(pkg.length + 1)
    }.split('.')
    return ClassName.get(pkg, shortNames.first(), *(shortNames.drop(1).toTypedArray()))
}

internal fun KSType.typeName(): TypeName? {
    return if (this.arguments.isNotEmpty()) {
        val args: Array<TypeName> = this.arguments.map {
            it.type.typeName()
        }.toTypedArray()
        val className = declaration.typeName() ?: return null
        ParameterizedTypeName.get(
            className,
            *args
        )
    } else {
        this.declaration.typeName()
    }
}

/**
 * KSDeclaration.packageName might throw for error types.
 * https://github.com/android/kotlin/issues/121
 */
internal fun KSDeclaration.safeGetPackageName(): String? {
    return try {
        packageName.asString().let {
            if (it == "<root>") {
                ""
            } else {
                it
            }
        }
    } catch (t: Throwable) {
        null
    }
}

// TODO remove after https://github.com/android/kotlin/issues/123
internal fun KSType?.isAssignableFromWithErrorWorkaround(other: KSType?): Boolean {
    if (other == null || this == null) return false
    if (isError || other.isError) return false
    return isAssignableFrom(other)
}
