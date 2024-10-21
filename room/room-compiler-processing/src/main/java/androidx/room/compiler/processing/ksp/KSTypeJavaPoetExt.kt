/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room.compiler.codegen.JArrayTypeName
import androidx.room.compiler.processing.javac.kotlin.typeNameFromJvmSignature
import androidx.room.compiler.processing.tryBox
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.outerType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName

// Catch-all type name when we cannot resolve to anything. This is what KAPT uses as error type
// and we use the same type in KSP for consistency.
// https://kotlinlang.org/docs/reference/kapt.html#non-existent-type-correction
internal val ERROR_JTYPE_NAME = JClassName.get("error", "NonExistentClass")

/**
 * To handle self referencing types and avoid infinite recursion, we keep a lookup map for
 * TypeVariables.
 */
private class TypeResolutionContext(
    val originalType: KSType? = null,
    val typeArgumentTypeLookup: MutableMap<KSName, JTypeName> = LinkedHashMap(),
)

/** Turns a KSTypeReference into a TypeName in java's type system. */
internal fun KSTypeReference?.asJTypeName(resolver: Resolver): JTypeName =
    asJTypeName(resolver = resolver, typeResolutionContext = TypeResolutionContext())

private fun KSTypeReference?.asJTypeName(
    resolver: Resolver,
    typeResolutionContext: TypeResolutionContext
): JTypeName {
    return if (this == null) {
        ERROR_JTYPE_NAME
    } else {
        resolve().asJTypeName(resolver, typeResolutionContext)
    }
}

/** Turns a KSDeclaration into a TypeName in java's type system. */
internal fun KSDeclaration.asJTypeName(resolver: Resolver): JTypeName =
    asJTypeName(resolver = resolver, typeResolutionContext = TypeResolutionContext())

@OptIn(KspExperimental::class)
private fun KSDeclaration.asJTypeName(
    resolver: Resolver,
    typeResolutionContext: TypeResolutionContext
): JTypeName {
    if (this is KSTypeAlias) {
        return this.type.asJTypeName(resolver, typeResolutionContext)
    }
    if (this is KSTypeParameter) {
        return this.asJTypeName(resolver, typeResolutionContext)
    }
    // if there is no qualified name, it is a resolution error so just return shared instance
    // KSP may improve that later and if not, we can improve it in Room
    // TODO: https://issuetracker.google.com/issues/168639183
    val qualified = qualifiedName?.asString() ?: return ERROR_JTYPE_NAME
    val pkg = getNormalizedPackageName()

    // We want to map Kotlin types to equivalent Java types if there is one (e.g.
    // kotlin.String to java.lang.String or kotlin.collections.List to java.util.List).
    // Note: To match KAPT behavior, a type annotated with @JvmInline is only replaced with the
    // underlying type if the inline type is used directly (e.g. MyInlineType) rather than in the
    // type args of another type, (e.g. List<MyInlineType>).
    val isInline = isValueClass()
    val isKotlinType = pkg == "kotlin" || pkg.startsWith("kotlin.")
    val isKotlinUnitType = qualified == "kotlin.Unit"
    if (
        !isKotlinUnitType &&
            ((isInline && isUsedDirectly(typeResolutionContext)) || (!isInline && isKotlinType))
    ) {
        val jvmSignature = resolver.mapToJvmSignature(this)
        if (!jvmSignature.isNullOrBlank()) {
            return jvmSignature.typeNameFromJvmSignature()
        }
    }

    // using qualified name and pkg, figure out the short names.
    val shortNames =
        if (pkg == "") {
                qualified
            } else {
                qualified.substring(pkg.length + 1)
            }
            .split('.')
    return JClassName.get(pkg, shortNames.first(), *(shortNames.drop(1).toTypedArray()))
}

/** Turns a KSTypeArgument into a TypeName in java's type system. */
internal fun KSTypeArgument.asJTypeName(resolver: Resolver): JTypeName =
    asJTypeName(resolver = resolver, typeResolutionContext = TypeResolutionContext())

private fun KSTypeParameter.asJTypeName(
    resolver: Resolver,
    typeResolutionContext: TypeResolutionContext
): JTypeName {
    // see https://github.com/square/javapoet/issues/842
    typeResolutionContext.typeArgumentTypeLookup[name]?.let {
        return it
    }
    val mutableBounds = mutableListOf<JTypeName>()
    val typeName = createModifiableTypeVariableName(name = name.asString(), bounds = mutableBounds)
    typeResolutionContext.typeArgumentTypeLookup[name] = typeName
    val resolvedBounds =
        bounds.map { it.asJTypeName(resolver, typeResolutionContext).tryBox() }.toList()
    if (resolvedBounds.isNotEmpty()) {
        mutableBounds.addAll(resolvedBounds)
        mutableBounds.remove(JTypeName.OBJECT)
    }
    typeResolutionContext.typeArgumentTypeLookup.remove(name)
    return typeName
}

private fun KSTypeArgument.asJTypeName(
    resolver: Resolver,
    typeResolutionContext: TypeResolutionContext
): JTypeName {
    fun resolveTypeName() = type.asJTypeName(resolver, typeResolutionContext).tryBox()
    return when (variance) {
        Variance.CONTRAVARIANT -> JWildcardTypeName.supertypeOf(resolveTypeName())
        Variance.COVARIANT -> JWildcardTypeName.subtypeOf(resolveTypeName())
        Variance.STAR -> JWildcardTypeName.subtypeOf(JTypeName.OBJECT)
        else -> resolveTypeName()
    }
}

/** Turns a KSType into a TypeName in java's type system. */
internal fun KSType.asJTypeName(resolver: Resolver): JTypeName =
    asJTypeName(resolver = resolver, typeResolutionContext = TypeResolutionContext(this))

@OptIn(KspExperimental::class)
private fun KSType.asJTypeName(
    resolver: Resolver,
    typeResolutionContext: TypeResolutionContext,
): JTypeName {
    if (declaration is KSTypeAlias) {
        return replaceTypeAliases(resolver).asJTypeName(resolver, typeResolutionContext)
    }
    val typeName = declaration.asJTypeName(resolver, typeResolutionContext)
    val isJavaPrimitiveOrVoid = typeName.tryBox() !== typeName
    if (
        !isTypeParameter() &&
            !resolver.isJavaRawType(this) &&
            // Excluding generic value classes used directly otherwise we may generate something
            // like `Object<String>`.
            !(declaration.isValueClass() && declaration.isUsedDirectly(typeResolutionContext)) &&
            !isJavaPrimitiveOrVoid
    ) {
        val args: Array<JTypeName> =
            this.innerArguments
                .map { typeArg -> typeArg.asJTypeName(resolver, typeResolutionContext) }
                .map { it.tryBox() }
                .toTypedArray()

        when (typeName) {
            is JArrayTypeName -> {
                return if (args.isEmpty()) {
                    // e.g. IntArray
                    typeName
                } else {
                    JArrayTypeName.of(args.single())
                }
            }
            is JClassName -> {
                val outerType = this.outerType
                if (outerType != null) {
                    val outerTypeName = outerType.asJTypeName(resolver, typeResolutionContext)
                    if (outerTypeName is JParameterizedTypeName) {
                        return outerTypeName.nestedClass(typeName.simpleName(), args.toList())
                    }
                }
                return if (args.isEmpty()) {
                    typeName
                } else {
                    JParameterizedTypeName.get(typeName, *args)
                }
            }
            else -> error("Unexpected type name for KSType: $typeName")
        }
    } else {
        return typeName
    }
}

/**
 * The private constructor of [JTypeVariableName] which receives a list. We use this in
 * [createModifiableTypeVariableName] to create a [JTypeVariableName] whose bounds can be modified
 * afterwards.
 */
private val typeVarNameConstructor by lazy {
    try {
        JTypeVariableName::class
            .java
            .getDeclaredConstructor(String::class.java, List::class.java)
            .also { it.trySetAccessible() }
    } catch (ex: NoSuchMethodException) {
        throw IllegalStateException(
            """
            Room couldn't find the constructor it is looking for in JavaPoet.
            Please file a bug at $ISSUE_TRACKER_LINK.
            """
                .trimIndent(),
            ex
        )
    }
}

/**
 * Creates a TypeVariableName where we can change the bounds after constructor. This is used to
 * workaround a case for self referencing type declarations. see b/187572913 for more details
 */
private fun createModifiableTypeVariableName(
    name: String,
    bounds: List<JTypeName>
): JTypeVariableName = typeVarNameConstructor.newInstance(name, bounds) as JTypeVariableName

private fun KSDeclaration.isUsedDirectly(typeResolutionContext: TypeResolutionContext): Boolean {
    val qualified = qualifiedName?.asString()
    return typeResolutionContext.originalType?.declaration?.qualifiedName?.asString() == qualified
}
