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
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

internal const val ERROR_PACKAGE_NAME = "androidx.room.compiler.processing.kotlin.error"

// catch-all type name when we cannot resolve to anything.
internal val ERROR_TYPE_NAME = ClassName.get(ERROR_PACKAGE_NAME, "CannotResolve")

/**
 * Turns a KSTypeReference into a TypeName
 *
 * We try to achieve this by first resolving it and iterating.
 * If some types cannot be resolved, we do a best effort name guess from the KSTypeReference's
 * element.
 */
internal fun KSTypeReference?.typeName(): TypeName {
    return if (this == null) {
        ERROR_TYPE_NAME
    } else {
        resolve().typeName()
    }
}

internal fun KSDeclaration.typeName(): ClassName {
    // if there is no qualified name, it is a resolution error so just return shared instance
    // KSP may improve that later and if not, we can improve it in Room
    // TODO: https://issuetracker.google.com/issues/168639183
    val qualified = qualifiedName?.asString() ?: return ERROR_TYPE_NAME
    // get the package name first, it might throw for invalid types, hence we use safeGetPackageName
    val pkg = getNormalizedPackageName()
    // using qualified name and pkg, figure out the short names.
    val shortNames = if (pkg == "") {
        qualified
    } else {
        qualified.substring(pkg.length + 1)
    }.split('.')
    return ClassName.get(pkg, shortNames.first(), *(shortNames.drop(1).toTypedArray()))
}

internal fun KSTypeArgument.typeName(
    param: KSTypeParameter
): TypeName {
    return when (variance) {
        Variance.CONTRAVARIANT -> WildcardTypeName.supertypeOf(type.typeName())
        Variance.COVARIANT -> WildcardTypeName.subtypeOf(type.typeName())
        Variance.STAR -> {
            // for star projected types, JavaPoet uses the name from the declaration if
            // * is not given explicitly
            if (type == null) {
                // explicit *
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            } else {
                TypeVariableName.get(param.name.asString(), type.typeName())
            }
        }
        else -> type.typeName()
    }
}

internal fun KSType.typeName(): TypeName {
    return if (this.arguments.isNotEmpty()) {
        val args: Array<TypeName> = this.arguments.mapIndexed { index, typeArg ->
            typeArg.typeName(
                this.declaration.typeParameters[index]
            )
        }.toTypedArray()
        val className = declaration.typeName()
        ParameterizedTypeName.get(
            className,
            *args
        )
    } else {
        this.declaration.typeName()
    }
}

/**
 * Root package comes as <root> instead of "" so we work around it here.
 */
internal fun KSDeclaration.getNormalizedPackageName(): String {
    return packageName.asString().let {
        if (it == "<root>") {
            ""
        } else {
            it
        }
    }
}

internal fun KSTypeArgument.requireType(): KSType {
    return checkNotNull(type?.resolve()) {
        "KSTypeArgument.type should not have been null, please file a bug. $this"
    }
}