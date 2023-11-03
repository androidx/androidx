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

import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.tryBox
import androidx.room.compiler.processing.tryUnbox
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName
import kotlin.reflect.KClass

/**
 * XType implementation for KSP type.
 *
 * It might be initialized with a [KSTypeReference] or [KSType] depending on the call point.
 *
 * We don't necessarily have a [KSTypeReference] (e.g. if we are getting it from an element).
 * Similarly, we may not be able to get a [KSType] (e.g. if it resolves to error).
 */
internal abstract class KspType(
    env: KspProcessingEnv,
    val ksType: KSType,
    // This is needed as a workaround until https://github.com/google/ksp/issues/1376 is fixed.
    val originalKSAnnotations: Sequence<KSAnnotation>,
    /** Type resolver to convert KSType into its JVM representation. */
    val scope: KSTypeVarianceResolverScope?,
    /** The `typealias` that was resolved to get the [ksType], or null if none exists. */
    val typeAlias: KSType?,
) : KspAnnotated(env), XType, XEquality {
    override val rawType by lazy {
        KspRawType(this)
    }

    final override val typeName: TypeName by lazy {
        xTypeName.java
    }

    override fun asTypeName() = xTypeName

    /**
     * A Kotlin type might have a slightly different type in JVM vs Kotlin due to wildcards.
     * The [XTypeName] represents those differences as [JTypeName] and [KTypeName], respectively.
     */
    private val xTypeName: XTypeName by lazy {
        val jvmWildcardType = env.resolveWildcards(typeAlias ?: ksType, scope).let {
            if (it == ksType) {
                this
            } else {
                env.wrap(
                    ksType = it,
                    allowPrimitives = this is KspPrimitiveType
                )
            }
        }
        XTypeName(
            jvmWildcardType.resolveJTypeName(),
            resolveKTypeName(),
            nullability
        )
    }

    protected abstract fun resolveJTypeName(): JTypeName

    protected abstract fun resolveKTypeName(): KTypeName

    override val nullability by lazy {
        when (ksType.nullability) {
            Nullability.NULLABLE -> XNullability.NULLABLE
            Nullability.NOT_NULL -> XNullability.NONNULL
            else -> XNullability.UNKNOWN
        }
    }

    override val superTypes: List<XType> by lazy {
        if (xTypeName == XTypeName.ANY_OBJECT) {
            // The object class doesn't have any supertypes.
            return@lazy emptyList<XType>()
        }
        val resolvedTypeArguments: Map<String, KSTypeArgument> =
            ksType.declaration.typeParameters.mapIndexed { i, parameter ->
                parameter.name.asString() to ksType.arguments[i]
            }.toMap()
        val superTypes = (ksType.declaration as? KSClassDeclaration)?.superTypes?.toList()?.map {
            env.wrap(
                ksType = resolveTypeArguments(it.resolve(), resolvedTypeArguments),
                allowPrimitives = false
            )
        } ?: emptyList()
        val (superClasses, superInterfaces) = superTypes.partition {
            it.typeElement?.isClass() == true
        }
        // Per documentation, always return the class before the interfaces.
        if (superClasses.isEmpty()) {
            // Return Object when there's no explicit super class specified on the class/interface.
            // This matches javac's Types#directSupertypes().
            listOf(env.requireType(TypeName.OBJECT)) + superInterfaces
        } else {
            check(superClasses.size == 1) {
                "Class ${this.typeName} should have only one super class. Found" +
                    " ${superClasses.size}" +
                    " (${superClasses.joinToString { it.typeName.toString() }})."
            }
            superClasses + superInterfaces
        }
    }

    private fun resolveTypeArguments(
        type: KSType,
        resolvedTypeArguments: Map<String, KSTypeArgument>,
        stack: List<KSType> = emptyList()
    ): KSType {
        return type.replace(
            type.arguments.map { argument ->
                val argType = argument.type?.resolve() ?: return@map argument
                val argDeclaration = argType.declaration
                if (argDeclaration is KSTypeParameter) {
                    // If this is a type parameter, replace it with the resolved type argument.
                    resolvedTypeArguments[argDeclaration.name.asString()] ?: argument
                } else if (argType.arguments.isNotEmpty() && !stack.contains(argType)) {
                    // If this is a type with arguments, the arguments may contain a type parameter,
                    // e.g. Foo<T>, so try to resolve the type and then convert to a type argument.
                    env.resolver.getTypeArgument(
                        typeRef = resolveTypeArguments(
                            type = argType,
                            resolvedTypeArguments = resolvedTypeArguments,
                            stack = stack + argType
                        ).createTypeReference(),
                        variance = Variance.INVARIANT
                    )
                } else {
                    argument
                }
            }.toList()
        )
    }

    override val typeElement by lazy {
        // Array types don't have an associated type element (only the componentType does), so
        // return null.
        if (isArray()) {
            return@lazy null
        }

        // If this is a primitive, return null for consistency since primitives normally imply
        // that there isn't an associated type element.
        if (this is KspPrimitiveType) {
            return@lazy null
        }

        val declaration = ksType.declaration as? KSClassDeclaration
        declaration?.let {
            env.wrapClassDeclaration(it)
        }
    }

    @OptIn(KspExperimental::class)
    override val typeArguments: List<XType> by lazy {
        if (env.resolver.isJavaRawType(ksType)) {
            emptyList()
        } else {
            ksType.arguments.map { env.wrap(it) }
        }
    }

    override fun isAssignableFrom(other: XType): Boolean {
        check(other is KspType)
        return ksType.isAssignableFrom(other.ksType)
    }

    override fun isError(): Boolean {
        // Avoid returning true if this type represents a java wildcard type, e.g. "? extends Foo"
        // since in that case the wildcard type is not the error type itself. Instead, the error
        // type should be on the XType#extendsBound() type, "Foo", instead.
        return ksType.isError && !isJavaWildcardType()
    }

    private fun isJavaWildcardType(): Boolean {
        return asTypeName().java is WildcardTypeName
    }

    override fun defaultValue(): String {
        // NOTE: this does not match the java implementation though it is probably more correct for
        // kotlin.
        if (ksType.nullability == Nullability.NULLABLE) {
            return "null"
        }
        val builtIns = env.resolver.builtIns
        return when (ksType) {
            builtIns.booleanType -> "false"
            builtIns.byteType, builtIns.shortType, builtIns.intType, builtIns
                .charType -> "0"
            builtIns.longType -> "0L"
            builtIns.floatType -> "0f"
            builtIns.doubleType -> "0.0"
            else -> "null"
        }
    }

    override fun annotations(): Sequence<KSAnnotation> = originalKSAnnotations

    override fun isNone(): Boolean {
        // even void is converted to Unit so we don't have none type in KSP
        // see: KspTypeTest.noneType
        return false
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        // closest to what MoreTypes#isTypeOf does.
        // accept both boxed and unboxed because KClass.java for primitives wrappers will always
        // give the primitive (e.g. kotlin.Int::class.java is int)
        return rawType.typeName.tryBox().toString() == other.java.canonicalName ||
            rawType.typeName.tryUnbox().toString() == other.java.canonicalName
    }

    override fun isSameType(other: XType): Boolean {
        check(other is KspType)
        if (nullability == XNullability.UNKNOWN || other.nullability == XNullability.UNKNOWN) {
            // if one the nullabilities is unknown, it is coming from java source code or .class.
            // for those cases, use java platform type equality (via typename)
            return asTypeName().java == other.asTypeName().java
        }
        // NOTE: this is inconsistent with java where nullability is ignored.
        // it is intentional but might be reversed if it happens to break use cases.
        return ksType == other.ksType
    }

    override fun extendsBound(): XType? {
        // when we detect that there should be an extends bounds, KspProcessingEnv creates
        // [KspTypeArgumentType].
        return null
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(ksType)
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return ksType.toString()
    }

    abstract override fun boxed(): KspType

    abstract fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?,
    ): KspType

    fun copyWithScope(scope: KSTypeVarianceResolverScope) =
        copy(env, ksType, originalKSAnnotations, scope, typeAlias)

    fun copyWithTypeAlias(typeAlias: KSType) =
        copy(env, ksType, originalKSAnnotations, scope, typeAlias)

    private fun copyWithNullability(nullability: XNullability): KspType = boxed().copy(
        env = env,
        ksType = ksType.withNullability(nullability),
        originalKSAnnotations = originalKSAnnotations,
        scope = scope,
        typeAlias = typeAlias,
    )

    final override fun makeNullable(): KspType {
        if (nullability == XNullability.NULLABLE) {
            return this
        }
        return copyWithNullability(XNullability.NULLABLE)
    }

    final override fun makeNonNullable(): KspType {
        if (nullability == XNullability.NONNULL) {
            return this
        }
        return copyWithNullability(XNullability.NONNULL)
    }
}
