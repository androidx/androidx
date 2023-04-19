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

import androidx.room.compiler.processing.XConstructorType
import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.XTypeElementStore
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance

internal class KspProcessingEnv(
    val delegate: SymbolProcessorEnvironment,
    override val config: XProcessingEnvConfig,
) : XProcessingEnv {
    override val backend: XProcessingEnv.Backend = XProcessingEnv.Backend.KSP
    override val options = delegate.options
    private val logger = delegate.logger
    private val codeGenerator = delegate.codeGenerator

    // No API to get this but Kotlin's default is 8, so go with it for now.
    // TODO: https://github.com/google/ksp/issues/810
    override val jvmVersion: Int
        get() = 8

    private val ksFileMemberContainers = mutableMapOf<KSFile, KspFileMemberContainer>()

    /**
     * Variance resolver to find JVM types of KSType. See [KSTypeVarianceResolver] docs for details.
     */
    private val ksTypeVarianceResolver by lazy {
        KSTypeVarianceResolver(resolver)
    }

    private var _resolver: Resolver? = null

    var resolver
        get() = _resolver!!
        internal set(value) {
            _resolver = value
        }

    private val typeElementStore =
        XTypeElementStore(
            findElement = {
                resolver.getClassDeclarationByName(
                    KspTypeMapper.swapWithKotlinType(it)
                )
            },
            getQName = {
                // for error types or local types, qualified name is null.
                // it is best to just not cache them
                it.qualifiedName?.asString()
            },
            wrap = { classDeclaration ->
                KspTypeElement.create(this, classDeclaration)
            }
        )

    override val messager: XMessager = KspMessager(logger)

    private val arrayTypeFactory by lazy {
        KspArrayType.Factory(this)
    }

    override val filer: XFiler = KspFiler(codeGenerator, messager)

    val commonTypes
        get() = CommonTypes(resolver)

    val voidType
        get() = KspVoidType(
            env = this,
            ksType = resolver.builtIns.unitType,
            boxed = false,
            scope = null
        )

    override fun findTypeElement(qName: String): KspTypeElement? {
        return typeElementStore[qName]
    }

    @OptIn(KspExperimental::class)
    override fun getTypeElementsFromPackage(packageName: String): List<XTypeElement> {
        return resolver.getDeclarationsFromPackage(packageName)
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.classKind == ClassKind.ENUM_ENTRY }
            .map { KspTypeElement.create(this, it) }
            .toList()
    }

    override fun findType(qName: String): XType? {
        val kotlinTypeName = KspTypeMapper.swapWithKotlinType(qName)
        return resolver.findClass(kotlinTypeName)?.let {
            wrap(
                allowPrimitives = KspTypeMapper.isJavaPrimitiveType(qName),
                ksType = it.asType(emptyList())
            )
        }
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        return findTypeElement("javax.annotation.processing.Generated")
            ?: findTypeElement("javax.annotation.Generated")
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): KspType {
        check(type is KspTypeElement) {
            "Unexpected type element type: $type"
        }
        val typeArguments = types.map { argType ->
            check(argType is KspType) {
                "$argType is not an instance of KspType"
            }
            resolver.getTypeArgument(
                argType.ksType.createTypeReference(),
                variance = if (argType is KspTypeArgumentType) {
                    argType.typeArg.variance
                } else {
                    Variance.INVARIANT
                }
            )
        }
        return wrap(
            ksType = type.declaration.asType(typeArguments),
            allowPrimitives = false
        )
    }

    override fun getWildcardType(consumerSuper: XType?, producerExtends: XType?): XType {
        check(consumerSuper == null || producerExtends == null) {
            "Cannot supply both super and extends bounds."
        }
        return wrap(
            ksTypeArgument = if (consumerSuper != null) {
                resolver.getTypeArgument(
                    typeRef = (consumerSuper as KspType).ksType.createTypeReference(),
                    variance = Variance.CONTRAVARIANT
                )
            } else if (producerExtends != null) {
                resolver.getTypeArgument(
                    typeRef = (producerExtends as KspType).ksType.createTypeReference(),
                    variance = Variance.COVARIANT
                )
            } else {
                // This returns the type "out Any?", which should be equivalent to "*"
                resolver.getTypeArgument(
                    typeRef = resolver.builtIns.anyType.makeNullable().createTypeReference(),
                    variance = Variance.COVARIANT
                )
            }
        )
    }

    override fun getArrayType(type: XType): KspArrayType {
        check(type is KspType)
        return arrayTypeFactory.createWithComponentType(type)
    }

    /**
     * Wraps the given `ksType`.
     *
     * The [originatingReference] is used to calculate whether the given [ksType] can be a
     * primitive or not.
     */
    fun wrap(
        originatingReference: KSTypeReference,
        ksType: KSType
    ): KspType {
        return wrap(
            ksType = ksType,
            allowPrimitives = !originatingReference.isTypeParameterReference()
        )
    }

    /**
     * Wraps the given [typeReference] in to a [KspType].
     */
    fun wrap(
        typeReference: KSTypeReference
    ) = wrap(
        originatingReference = typeReference,
        ksType = typeReference.resolve()
    )

    fun wrap(ksTypeArgument: KSTypeArgument): KspType {
        val typeRef = ksTypeArgument.type
        if (typeRef != null && ksTypeArgument.variance == Variance.INVARIANT) {
            // fully resolved type argument, return regular type.
            return wrap(
                ksType = typeRef.resolve(),
                allowPrimitives = false
            )
        }
        return KspTypeArgumentType(
            env = this,
            typeArg = ksTypeArgument,
            scope = null
        )
    }

    /**
     * Wraps the given KSType into a KspType.
     *
     * Certain Kotlin types might be primitives in Java but such information cannot be derived
     * just by looking at the type itself.
     * Instead, it is passed in an argument to this function and public wrap functions make that
     * decision.
     */
    fun wrap(ksType: KSType, allowPrimitives: Boolean): KspType {
        val declaration = ksType.declaration
        if (declaration is KSTypeAlias) {
            val actual = wrap(
                ksType = declaration.type.resolve().replace(ksType.arguments),
                allowPrimitives = allowPrimitives && ksType.nullability == Nullability.NOT_NULL
            )
            // if this type is nullable, carry it over
            return if (ksType.nullability == Nullability.NULLABLE) {
                actual.makeNullable()
            } else {
                actual
            }
        }
        val qName = ksType.declaration.qualifiedName?.asString()
        if (declaration is KSTypeParameter) {
            return KspTypeVariableType(
                env = this,
                ksType = ksType,
                scope = null
            )
        }
        if (allowPrimitives && qName != null && ksType.nullability == Nullability.NOT_NULL) {
            // check for primitives
            val javaPrimitive = KspTypeMapper.getPrimitiveJavaTypeName(qName)
            if (javaPrimitive != null) {
                return KspPrimitiveType(this, ksType, scope = null)
            }
            // special case for void
            if (qName == "kotlin.Unit") {
                return voidType
            }
        }
        return arrayTypeFactory.createIfArray(ksType) ?: DefaultKspType(
            this,
            ksType,
            scope = null
        )
    }

    fun wrapClassDeclaration(declaration: KSClassDeclaration): KspTypeElement {
        return typeElementStore[declaration]
    }

    fun wrapKSFile(file: KSFile): KspMemberContainer {
        return ksFileMemberContainers.getOrPut(file) {
            KspFileMemberContainer(
                env = this,
                ksFile = file
            )
        }
    }

    /**
     * Resolves the wildcards for the given ksType. See [KSTypeVarianceResolver] for details.
     */
    internal fun resolveWildcards(ksType: KSType, scope: KSTypeVarianceResolverScope) =
        ksTypeVarianceResolver.applyTypeVariance(ksType, scope)

    internal fun clearCache() {
        typeElementStore.clear()
    }

    internal fun isSameType(type1: XExecutableType, type2: XExecutableType): Boolean {
        if (type1 == type2) {
            return true
        }
        if (type1.parameterTypes.size != type2.parameterTypes.size) {
            return false
        }
        type1.parameterTypes.indices.forEach { i ->
            if (!type1.parameterTypes[i].isSameType(type2.parameterTypes[i])) {
                return false
            }
        }
        fun returnType(type: XExecutableType): XType {
            return when (type) {
                is XMethodType -> type.returnType
                is XConstructorType -> voidType
                else -> error("Unexpected XExecutableType: $type")
            }
        }
        return returnType(type1).isSameType(returnType(type2))
    }

    class CommonTypes(resolver: Resolver) {
        val nullableInt by lazy {
            resolver.builtIns.intType.makeNullable()
        }
        val nullableLong by lazy {
            resolver.builtIns.longType.makeNullable()
        }
        val nullableByte by lazy {
            resolver.builtIns.byteType.makeNullable()
        }
    }
}
