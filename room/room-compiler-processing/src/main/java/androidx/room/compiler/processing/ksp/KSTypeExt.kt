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
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
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
import kotlin.IllegalStateException

// Catch-all type name when we cannot resolve to anything. This is what KAPT uses as error type
// and we use the same type in KSP for consistency.
// https://kotlinlang.org/docs/reference/kapt.html#non-existent-type-correction
internal val ERROR_TYPE_NAME = ClassName.get("error", "NonExistentClass")

/**
 * To handle self referencing types and avoid infinite recursion, we keep a lookup map for
 * TypeVariables.
 */
private typealias TypeArgumentTypeLookup = LinkedHashMap<KSName, TypeName>

/**
 * Turns a KSTypeReference into a TypeName in java's type system.
 */
internal fun KSTypeReference?.typeName(resolver: Resolver): TypeName =
    typeName(
        resolver = resolver,
        typeArgumentTypeLookup = TypeArgumentTypeLookup()
    )

private fun KSTypeReference?.typeName(
    resolver: Resolver,
    typeArgumentTypeLookup: TypeArgumentTypeLookup
): TypeName {
    return if (this == null) {
        ERROR_TYPE_NAME
    } else {
        resolve().typeName(resolver, typeArgumentTypeLookup)
    }
}

/**
 * Turns a KSDeclaration into a TypeName in java's type system.
 */
internal fun KSDeclaration.typeName(resolver: Resolver): TypeName =
    typeName(
        resolver = resolver,
        typeArgumentTypeLookup = TypeArgumentTypeLookup()
    )

@OptIn(KspExperimental::class)
private fun KSDeclaration.typeName(
    resolver: Resolver,
    typeArgumentTypeLookup: TypeArgumentTypeLookup
): TypeName {
    if (this is KSTypeAlias) {
        return this.type.typeName(resolver, typeArgumentTypeLookup)
    }
    if (this is KSTypeParameter) {
        return this.typeName(resolver, typeArgumentTypeLookup)
    }
    // if there is no qualified name, it is a resolution error so just return shared instance
    // KSP may improve that later and if not, we can improve it in Room
    // TODO: https://issuetracker.google.com/issues/168639183
    val qualified = qualifiedName?.asString() ?: return ERROR_TYPE_NAME
    val jvmSignature = resolver.mapToJvmSignature(this)
    if (jvmSignature != null && jvmSignature.isNotBlank()) {
        return jvmSignature.typeNameFromJvmSignature()
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
): TypeName = typeName(
    param = param,
    resolver = resolver,
    typeArgumentTypeLookup = TypeArgumentTypeLookup()
)

private fun KSTypeParameter.typeName(
    resolver: Resolver,
    typeArgumentTypeLookup: TypeArgumentTypeLookup
): TypeName {
    // see https://github.com/square/javapoet/issues/842
    typeArgumentTypeLookup[name]?.let {
        return it
    }
    val mutableBounds = mutableListOf<TypeName>()
    val typeName = createModifiableTypeVariableName(name = name.asString(), bounds = mutableBounds)
    typeArgumentTypeLookup[name] = typeName
    val resolvedBounds = bounds.map {
        it.typeName(resolver, typeArgumentTypeLookup).tryBox()
    }.toList()
    if (resolvedBounds.isNotEmpty()) {
        mutableBounds.addAll(resolvedBounds)
        mutableBounds.remove(TypeName.OBJECT)
    }
    typeArgumentTypeLookup.remove(name)
    return typeName
}

private fun KSTypeArgument.typeName(
    param: KSTypeParameter,
    resolver: Resolver,
    typeArgumentTypeLookup: TypeArgumentTypeLookup
): TypeName {
    fun resolveTypeName() = type.typeName(resolver, typeArgumentTypeLookup).tryBox()

    return when (variance) {
        Variance.CONTRAVARIANT -> WildcardTypeName.supertypeOf(resolveTypeName())
        Variance.COVARIANT -> WildcardTypeName.subtypeOf(resolveTypeName())
        Variance.STAR -> {
            // for star projected types, JavaPoet uses the name from the declaration if
            // * is not given explicitly
            if (type == null) {
                // explicit *
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            } else {
                param.typeName(resolver, typeArgumentTypeLookup)
            }
        }
        else -> resolveTypeName()
    }
}

/**
 * Turns a KSType into a TypeName in java's type system.
 */
internal fun KSType.typeName(resolver: Resolver): TypeName =
    typeName(
        resolver = resolver,
        typeArgumentTypeLookup = TypeArgumentTypeLookup()
    )

private fun KSType.typeName(
    resolver: Resolver,
    typeArgumentTypeLookup: TypeArgumentTypeLookup
): TypeName {
    return if (this.arguments.isNotEmpty()) {
        val args: Array<TypeName> = this.arguments.mapIndexed { index, typeArg ->
            typeArg.typeName(
                param = this.declaration.typeParameters[index],
                resolver = resolver,
                typeArgumentTypeLookup = typeArgumentTypeLookup
            )
        }.map {
            it.tryBox()
        }.toTypedArray()
        when (
            val typeName = declaration
                .typeName(resolver, typeArgumentTypeLookup).tryBox()
        ) {
            is ArrayTypeName -> ArrayTypeName.of(args.single())
            is ClassName -> ParameterizedTypeName.get(
                typeName,
                *args
            )
            else -> error("Unexpected type name for KSType: $typeName")
        }
    } else {
        this.declaration.typeName(resolver, typeArgumentTypeLookup)
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

/**
 * The private constructor of [TypeVariableName] which receives a list.
 * We use this in [createModifiableTypeVariableName] to create a [TypeVariableName] whose bounds
 * can be modified afterwards.
 */
private val typeVarNameConstructor by lazy {
    try {
        TypeVariableName::class.java.getDeclaredConstructor(
            String::class.java,
            List::class.java
        ).also {
            it.trySetAccessible()
        }
    } catch (ex: NoSuchMethodException) {
        throw IllegalStateException(
            """
            Room couldn't find the constructor it is looking for in JavaPoet.
            Please file a bug at $ISSUE_TRACKER_LINK.
            """.trimIndent(),
            ex
        )
    }
}

/**
 * Creates a TypeVariableName where we can change the bounds after constructor.
 * This is used to workaround a case for self referencing type declarations.
 * see b/187572913 for more details
 */
private fun createModifiableTypeVariableName(
    name: String,
    bounds: List<TypeName>
): TypeVariableName = typeVarNameConstructor.newInstance(
    name,
    bounds
) as TypeVariableName