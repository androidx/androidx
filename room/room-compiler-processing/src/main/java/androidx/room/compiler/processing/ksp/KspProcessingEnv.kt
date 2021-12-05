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

import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.XTypeElementStore
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
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
    override val options: Map<String, String>,
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
) : XProcessingEnv {
    override val backend: XProcessingEnv.Backend = XProcessingEnv.Backend.KSP
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
            jvmTypeResolver = null
        )

    override fun findTypeElement(qName: String): KspTypeElement? {
        return typeElementStore[qName]
    }

    @OptIn(KspExperimental::class)
    override fun getTypeElementsFromPackage(packageName: String): List<XTypeElement> {
        return resolver.getDeclarationsFromPackage(packageName)
            .filterIsInstance<KSClassDeclaration>()
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
                variance = Variance.INVARIANT
            )
        }
        return wrap(
            ksType = type.declaration.asType(typeArguments),
            allowPrimitives = false
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

    fun wrap(ksTypeParam: KSTypeParameter, ksTypeArgument: KSTypeArgument): KspType {
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
            typeParam = ksTypeParam,
            jvmTypeResolver = null
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
                ksType = declaration.type.resolve(),
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
            return KspTypeArgumentType(
                env = this,
                typeArg = resolver.getTypeArgument(
                    ksType.createTypeReference(),
                    declaration.variance
                ),
                typeParam = declaration,
                jvmTypeResolver = null
            )
        }
        if (allowPrimitives && qName != null && ksType.nullability == Nullability.NOT_NULL) {
            // check for primitives
            val javaPrimitive = KspTypeMapper.getPrimitiveJavaTypeName(qName)
            if (javaPrimitive != null) {
                return KspPrimitiveType(this, ksType, jvmTypeResolver = null)
            }
            // special case for void
            if (qName == "kotlin.Unit") {
                return voidType
            }
        }
        return arrayTypeFactory.createIfArray(ksType) ?: DefaultKspType(
            this,
            ksType,
            jvmTypeResolver = null
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
    fun resolveWildcards(
        /**
         * The KSType whose wildcards variance will be resolved
         */
        ksType: KSType,
        /**
         * Default wildcard resolution strategy
         */
        wildcardMode: KSTypeVarianceResolver.WildcardMode,
        /**
         * The original declaration type if [ksType] is obtained via inheritance.
         */
        declarationType: KSType?
    ): KSType {
        return ksTypeVarianceResolver.applyTypeVariance(
            ksType = ksType,
            wildcardMode = wildcardMode,
            declarationType = declarationType
        )
    }

    internal fun clearCache() {
        typeElementStore.clear()
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
