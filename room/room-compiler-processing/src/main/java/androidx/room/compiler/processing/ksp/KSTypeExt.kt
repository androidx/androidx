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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.javac.kotlin.typeNameFromJvmSignature
import androidx.room.compiler.processing.tryBox
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName

// Catch-all type name when we cannot resolve to anything. This is what KAPT uses as error type
// and we use the same type in KSP for consistency.
// https://kotlinlang.org/docs/reference/kapt.html#non-existent-type-correction
internal val ERROR_TYPE_NAME = ClassName.get("error", "NonExistentClass")

/**
 * Turns a KSTypeReference into a TypeName in java's type system.
 */
internal fun KSTypeReference?.typeName(resolver: Resolver): TypeName {
    return if (this == null) {
        ERROR_TYPE_NAME
    } else {
        resolve().typeName(resolver)
    }
}

/**
 * Turns a KSDeclaration into a TypeName in java's type system.
 */
@OptIn(KspExperimental::class)
internal fun KSDeclaration.typeName(resolver: Resolver): TypeName {
    if (this is KSTypeAlias) {
        return this.type.typeName(resolver)
    }
    // if there is no qualified name, it is a resolution error so just return shared instance
    // KSP may improve that later and if not, we can improve it in Room
    // TODO: https://issuetracker.google.com/issues/168639183
    val qualified = qualifiedName?.asString() ?: return ERROR_TYPE_NAME
    val jvmSignature = resolver.mapToJvmSignature(this)
    if (jvmSignature != null && jvmSignature.isNotBlank()) {
        return jvmSignature.typeNameFromJvmSignature()
    }
    if (this is KSTypeParameter) {
        return TypeVariableName.get(name.asString())
    }
    // fallback to custom generation, it is very likely that this is an unresolved type
    // get the package name first, it might throw for invalid types, hence we use
    // safeGetPackageName
    val pkg = getNormalizedPackageName()
    // using qualified name and pkg, figure out the short names.
    val shortNames = if (pkg == "") {
        qualified
    } else {
        qualified.substring(pkg.length + 1)
    }.split('.')
    return ClassName.get(pkg, shortNames.first(), *(shortNames.drop(1).toTypedArray()))
}

/**
 * Turns a KSTypeArgument into a TypeName in java's type system.
 */
internal fun KSTypeArgument.typeName(
    param: KSTypeParameter,
    resolver: Resolver
): TypeName {
    return when (variance) {
        Variance.CONTRAVARIANT -> WildcardTypeName.supertypeOf(type.typeName(resolver).tryBox())
        Variance.COVARIANT -> WildcardTypeName.subtypeOf(type.typeName(resolver).tryBox())
        Variance.STAR -> {
            // for star projected types, JavaPoet uses the name from the declaration if
            // * is not given explicitly
            if (type == null) {
                // explicit *
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            } else {
                TypeVariableName.get(param.name.asString(), type.typeName(resolver).tryBox())
            }
        }
        else -> type.typeName(resolver).tryBox()
    }
}

/**
 * Turns a KSType into a TypeName in java's type system.
 */
internal fun KSType.typeName(resolver: Resolver): TypeName {
    return if (this.arguments.isNotEmpty()) {
        val args: Array<TypeName> = this.arguments.mapIndexed { index, typeArg ->
            typeArg.typeName(
                this.declaration.typeParameters[index],
                resolver
            )
        }.map {
            it.tryBox()
        }.toTypedArray()
        when (val typeName = declaration.typeName(resolver).tryBox()) {
            is ArrayTypeName -> ArrayTypeName.of(args.single())
            is ClassName -> ParameterizedTypeName.get(
                typeName,
                *args
            )
            else -> error("Unexpected type name for KSType: $typeName")
        }
    } else {
        this.declaration.typeName(resolver)
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

internal fun KSTypeReference.isTypeParameterReference(): Boolean {
    return this.resolve().declaration is KSTypeParameter
}

fun KSType.isInline() = declaration.modifiers.contains(Modifier.INLINE)

internal fun KSType.withNullability(nullability: XNullability) = when (nullability) {
    XNullability.NULLABLE -> makeNullable()
    XNullability.NONNULL -> makeNotNullable()
    else -> throw IllegalArgumentException("Cannot set KSType nullability to platform")
}
