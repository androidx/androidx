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

package androidx.room3.compiler.processing.ksp

import androidx.room3.compiler.processing.XConstructorType
import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XExecutableElementStore
import androidx.room3.compiler.processing.XExecutableParameterElement
import androidx.room3.compiler.processing.XExecutableType
import androidx.room3.compiler.processing.XFiler
import androidx.room3.compiler.processing.XMessager
import androidx.room3.compiler.processing.XMethodType
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XProcessingEnvConfig
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.javac.XTypeElementStore
import androidx.room3.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.NativePlatformInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
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
    override val messager: XMessager = KspMessager(logger)
    override val filer: XFiler = KspFiler(codeGenerator, messager)

    private val jvmPlatformInfo by lazy {
        delegate.platforms.filterIsInstance<JvmPlatformInfo>().firstOrNull()
    }

    override val targetPlatforms: Set<XProcessingEnv.Platform> =
        delegate.platforms
            .map { platform ->
                when (platform) {
                    is JvmPlatformInfo -> XProcessingEnv.Platform.JVM
                    is NativePlatformInfo -> XProcessingEnv.Platform.NATIVE
                    is JsPlatformInfo -> XProcessingEnv.Platform.JS
                    else -> XProcessingEnv.Platform.UNKNOWN
                }
            }
            .toSet()

    override val jvmVersion by lazy {
        when (val jvmTarget = jvmPlatformInfo?.jvmTarget) {
            // Special case "1.8" since it is the only valid value with the 1.x notation, it is
            // also the default value.
            // See https://kotlinlang.org/docs/compiler-reference.html#jvm-target-version
            "1.8",
            null -> 8
            else -> jvmTarget.toInt()
        }
    }

    internal val isKsp2 by lazy { delegate.kspVersion >= KotlinVersion(2, 0) }

    val jvmDefaultMode by lazy {
        jvmPlatformInfo?.let { JvmDefaultMode.fromStringOrNull(it.jvmDefaultMode) }
    }

    private var nullableKspResolver: KspResolver? = null

    private val kspResolver: KspResolver
        get() = nullableKspResolver!!

    fun setResolver(resolver: Resolver) {
        nullableKspResolver = KspResolver(this, resolver)
    }

    val resolver: Resolver
        get() = kspResolver.resolver

    val voidType: KspType
        get() = kspResolver.voidType

    override fun findTypeElement(qName: String): KspTypeElement? =
        kspResolver.findTypeElement(qName)

    override fun findType(qName: String) = kspResolver.findType(qName)

    override fun findGeneratedAnnotation() = kspResolver.findGeneratedAnnotation()

    override fun getDeclaredType(type: XTypeElement, vararg types: XType) =
        kspResolver.getDeclaredType(type, *types)

    override fun getWildcardType(consumerSuper: XType?, producerExtends: XType?) =
        kspResolver.getWildcardType(consumerSuper, producerExtends)

    override fun getArrayType(type: XType) = kspResolver.getArrayType(type)

    override fun getTypeElementsFromPackage(packageName: String) =
        kspResolver.getTypeElementsFromPackage(packageName)

    override fun getElementsFromPackage(packageName: String) =
        kspResolver.getElementsFromPackage(packageName)

    /**
     * Wraps the given `ksType`.
     *
     * The [originatingReference] is used to calculate whether the given [ksType] can be a primitive
     * or not.
     */
    fun wrap(originatingReference: KSTypeReference, ksType: KSType): KspType =
        kspResolver.wrap(originatingReference, ksType)

    /** Wraps the given [typeReference] in to a [KspType]. */
    fun wrap(typeReference: KSTypeReference): KspType = kspResolver.wrap(typeReference)

    /** Wraps the given [ksTypeArgument] in to a [KspType]. */
    fun wrap(ksTypeArgument: KSTypeArgument): KspType = kspResolver.wrap(ksTypeArgument)

    /**
     * Wraps the given KSType into a KspType.
     *
     * Certain Kotlin types might be primitives in Java but such information cannot be derived just
     * by looking at the type itself. Instead, it is passed in an argument to this function and
     * public wrap functions make that decision.
     */
    fun wrap(ksType: KSType, allowPrimitives: Boolean): KspType =
        kspResolver.wrap(ksType, allowPrimitives)

    fun wrap(
        originalAnnotations: Sequence<KSAnnotation>,
        ksType: KSType,
        allowPrimitives: Boolean,
    ): KspType = kspResolver.wrap(originalAnnotations, ksType, allowPrimitives)

    fun wrapDeclaration(declaration: KSDeclaration): KspElement =
        kspResolver.wrapDeclaration(declaration)

    fun wrapClassDeclaration(declaration: KSClassDeclaration): KspElement =
        kspResolver.wrapClassDeclaration(declaration)

    fun wrapClassDeclarationForNonEnumEntry(declaration: KSClassDeclaration): KspTypeElement =
        kspResolver.wrapClassDeclarationForNonEnumEntry(declaration)

    fun wrapFunctionDeclaration(declaration: KSFunctionDeclaration) =
        kspResolver.wrapFunctionDeclaration(declaration)

    fun wrapPropertyDeclaration(declaration: KSPropertyDeclaration) =
        kspResolver.wrapPropertyDeclaration(declaration)

    fun wrapValueParameter(declaration: KSValueParameter) =
        kspResolver.wrapValueParameter(declaration)

    fun wrapPropertyAccessor(accessor: KSPropertyAccessor) =
        kspResolver.wrapPropertyAccessor(accessor)

    fun wrapAnnotation(declaration: KSAnnotation) = kspResolver.wrapAnnotation(declaration)

    fun wrapAnnotationValue(
        // TODO: Remove this parameter once https://github.com/google/ksp/issues/2637 is fixed.
        //  We need to pass in the parent annotation because KSValueArgument#parent is broken.
        annotation: KSAnnotation,
        value: KSValueArgument,
    ) = kspResolver.wrapAnnotationValue(annotation, value)

    fun wrapKSFile(file: KSFile): KspMemberContainer = kspResolver.wrapKSFile(file)

    /** Resolves the wildcards for the given ksType. See [KSTypeVarianceResolver] for details. */
    fun resolveWildcards(ksType: KSType, scope: KSTypeVarianceResolverScope?): KSType =
        kspResolver.resolveWildcards(ksType, scope)

    fun clearCache() = kspResolver.clearCache()

    fun isSameType(type1: XExecutableType, type2: XExecutableType): Boolean =
        kspResolver.isSameType(type1, type2)

    enum class JvmDefaultMode(val option: String) {
        DISABLE("disable"),
        ENABLE("enable"),
        NO_COMPATIBILITY("no-compatibility");

        companion object {
            fun fromStringOrNull(string: String?): JvmDefaultMode? =
                when (string) {
                    DISABLE.option -> DISABLE
                    ENABLE.option -> ENABLE
                    NO_COMPATIBILITY.option -> NO_COMPATIBILITY
                    else -> null
                }
        }
    }
}

private class KspResolver(val env: KspProcessingEnv, val resolver: Resolver) {
    private val ksFileMemberContainers = mutableMapOf<KSFile, KspFileMemberContainer>()

    /**
     * Variance resolver to find JVM types of KSType. See [KSTypeVarianceResolver] docs for details.
     */
    private val ksTypeVarianceResolver by lazy { KSTypeVarianceResolver(resolver) }

    private val typeElementStore =
        XTypeElementStore(
            findElement = { resolver.getClassDeclarationByName(it) },
            getQName = {
                // for error types or local types, qualified name is null.
                // it is best to just not cache them
                it.qualifiedName?.asString()
            },
            wrap = { classDeclaration -> KspTypeElement.create(env, classDeclaration) },
        )

    private val executableElementStore =
        XExecutableElementStore(
            wrap = { functionDeclaration: KSFunctionDeclaration ->
                KspExecutableElement.create(env, functionDeclaration)
            }
        )

    private val arrayTypeFactory by lazy { KspArrayType.Factory(env) }

    val voidType: KspType by lazy {
        KspVoidType(env = env, ksType = resolver.builtIns.unitType, boxed = false)
    }

    fun findTypeElement(qName: String): KspTypeElement? {
        return typeElementStore[KspTypeMapper.swapWithKotlinType(qName)]
    }

    @OptIn(KspExperimental::class)
    fun getTypeElementsFromPackage(packageName: String): List<XTypeElement> {
        return resolver
            .getDeclarationsFromPackage(packageName)
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.classKind == ClassKind.ENUM_ENTRY }
            .map { this.wrapClassDeclarationForNonEnumEntry(it) }
            .toList()
    }

    fun findType(qName: String): XType? {
        val kotlinTypeName = KspTypeMapper.swapWithKotlinType(qName)
        return resolver.findClass(kotlinTypeName)?.let {
            wrap(
                allowPrimitives = KspTypeMapper.isJavaPrimitiveType(qName),
                ksType = it.asType(emptyList()),
            )
        }
    }

    fun findGeneratedAnnotation(): XTypeElement? {
        val jvmPlatform =
            env.delegate.platforms.filterIsInstance<JvmPlatformInfo>().singleOrNull() ?: return null
        val jvmTarget =
            try {
                jvmPlatform.jvmTarget.toInt()
            } catch (ex: NumberFormatException) {
                null
            }
        return if (jvmTarget != null && jvmTarget >= 9) {
            findTypeElement("javax.annotation.processing.Generated")
        } else {
            findTypeElement("javax.annotation.Generated")
        }
    }

    fun getDeclaredType(type: XTypeElement, vararg types: XType): KspType {
        check(type is KspTypeElement) { "Unexpected type element type: $type" }
        val typeArguments =
            types.map { argType ->
                check(argType is KspType) { "$argType is not an instance of KspType" }
                resolver.getTypeArgument(
                    argType.ksType.createTypeReference(),
                    variance =
                        if (argType is KspTypeArgumentType) {
                            argType.typeArg.variance
                        } else {
                            Variance.INVARIANT
                        },
                )
            }
        return wrap(ksType = type.declaration.asType(typeArguments), allowPrimitives = false)
    }

    fun getWildcardType(consumerSuper: XType?, producerExtends: XType?): XType {
        check(consumerSuper == null || producerExtends == null) {
            "Cannot supply both super and extends bounds."
        }
        return wrap(
            ksTypeArgument =
                if (consumerSuper != null) {
                    resolver.getTypeArgument(
                        typeRef = (consumerSuper as KspType).ksType.createTypeReference(),
                        variance = Variance.CONTRAVARIANT,
                    )
                } else if (producerExtends != null) {
                    resolver.getTypeArgument(
                        typeRef = (producerExtends as KspType).ksType.createTypeReference(),
                        variance = Variance.COVARIANT,
                    )
                } else {
                    // This returns the type "out Any?", which should be equivalent to "*"
                    resolver.getTypeArgument(
                        typeRef = resolver.builtIns.anyType.makeNullable().createTypeReference(),
                        variance = Variance.COVARIANT,
                    )
                }
        )
    }

    fun getArrayType(type: XType): KspArrayType {
        check(type is KspType)
        return arrayTypeFactory.createWithComponentType(type)
    }

    @OptIn(KspExperimental::class)
    fun getElementsFromPackage(packageName: String): List<XElement> {
        return resolver
            .getDeclarationsFromPackage(packageName)
            .map {
                when (it) {
                    is KSClassDeclaration -> wrapClassDeclaration(it)
                    is KSPropertyDeclaration -> KspFieldElement.create(env, it)
                    is KSFunctionDeclaration -> KspMethodElement.create(env, it)
                    else -> error("Unknown element type")
                }
            }
            .toList()
    }

    /**
     * Wraps the given `ksType`.
     *
     * The [originatingReference] is used to calculate whether the given [ksType] can be a primitive
     * or not.
     */
    fun wrap(originatingReference: KSTypeReference, ksType: KSType): KspType {
        return wrap(
            originalAnnotations = originatingReference.annotations,
            ksType = ksType,
            allowPrimitives = !originatingReference.isTypeParameterReference(),
        )
    }

    /** Wraps the given [typeReference] in to a [KspType]. */
    fun wrap(typeReference: KSTypeReference) =
        wrap(originatingReference = typeReference, ksType = typeReference.resolve())

    fun wrap(ksTypeArgument: KSTypeArgument): KspType {
        val typeRef = ksTypeArgument.type
        if (typeRef != null && ksTypeArgument.variance == Variance.INVARIANT) {
            val declaration = typeRef.resolve().declaration
            // inline classes can't be non-invariant.
            if (declaration.isValueClass()) {
                return KspValueClassArgumentType(
                    env = env,
                    typeArg = ksTypeArgument,
                    originalKSAnnotations = ksTypeArgument.annotations,
                )
            }

            // fully resolved type argument, return regular type.
            return wrap(
                ksTypeArgument.annotations,
                ksType = typeRef.resolve(),
                allowPrimitives = false,
            )
        }
        return KspTypeArgumentType.create(env, ksTypeArgument)
    }

    /**
     * Wraps the given KSType into a KspType.
     *
     * Certain Kotlin types might be primitives in Java but such information cannot be derived just
     * by looking at the type itself. Instead, it is passed in an argument to this function and
     * public wrap functions make that decision.
     */
    fun wrap(ksType: KSType, allowPrimitives: Boolean): KspType {
        return wrap(ksType.annotations, ksType, allowPrimitives)
    }

    fun wrap(
        originalAnnotations: Sequence<KSAnnotation>,
        ksType: KSType,
        allowPrimitives: Boolean,
    ): KspType {
        val declaration = ksType.declaration
        if (declaration is KSTypeAlias) {
            return wrap(
                    originalAnnotations = originalAnnotations,
                    ksType = ksType.replaceTypeAliases(resolver),
                    allowPrimitives = allowPrimitives && ksType.nullability == Nullability.NOT_NULL,
                )
                .copyWithTypeAlias(ksType)
        }
        val qName = ksType.declaration.qualifiedName?.asString()
        if (declaration is KSTypeParameter) {
            return KspTypeVariableType(env, declaration, ksType, originalAnnotations)
        }
        if (allowPrimitives && qName != null && ksType.nullability == Nullability.NOT_NULL) {
            // check for primitives
            val javaPrimitive = KspTypeMapper.getPrimitiveJavaTypeName(qName)
            if (javaPrimitive != null) {
                return KspPrimitiveType(env, ksType, originalAnnotations)
            }
        }
        return arrayTypeFactory.createIfArray(ksType)
            ?: DefaultKspType(env, ksType, originalAnnotations)
    }

    fun wrapKSFile(file: KSFile): KspMemberContainer {
        return ksFileMemberContainers.getOrPut(file) {
            KspFileMemberContainer(env = env, ksFile = file)
        }
    }

    /** Resolves the wildcards for the given ksType. See [KSTypeVarianceResolver] for details. */
    internal fun resolveWildcards(ksType: KSType, scope: KSTypeVarianceResolverScope?) =
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

    fun wrapDeclaration(declaration: KSDeclaration): KspElement {
        return when (declaration) {
            is KSClassDeclaration -> wrapClassDeclaration(declaration)
            is KSFunctionDeclaration -> wrapFunctionDeclaration(declaration)
            is KSPropertyDeclaration -> wrapPropertyDeclaration(declaration)
            else -> error("Unsupported $declaration")
        }
    }

    fun wrapClassDeclaration(declaration: KSClassDeclaration): KspElement {
        return when (declaration.classKind) {
            ClassKind.ENUM_ENTRY -> KspEnumEntry.create(env, declaration)
            else -> wrapClassDeclarationForNonEnumEntry(declaration)
        }
    }

    fun wrapClassDeclarationForNonEnumEntry(declaration: KSClassDeclaration): KspTypeElement {
        check(declaration.classKind != ClassKind.ENUM_ENTRY) { "$declaration is an enum entry" }
        return typeElementStore[declaration]
    }

    fun wrapFunctionDeclaration(declaration: KSFunctionDeclaration): KspExecutableElement {
        return executableElementStore[declaration]
    }

    fun wrapPropertyDeclaration(declaration: KSPropertyDeclaration): KspFieldElement {
        return KspFieldElement.create(env, declaration)
    }

    fun wrapValueParameter(declaration: KSValueParameter): XExecutableParameterElement {
        return KspExecutableParameterElement.create(env, declaration)
    }

    fun wrapPropertyAccessor(accessor: KSPropertyAccessor): KspSyntheticPropertyMethodElement? {
        return wrapPropertyDeclaration(accessor.receiver).let {
            when (accessor) {
                is KSPropertyGetter -> it.getter
                is KSPropertySetter -> it.setter
                else -> error("Unsupported $accessor class ${accessor::class}")
            }
        }
    }

    fun wrapAnnotation(declaration: KSAnnotation): KspAnnotation {
        return KspAnnotation(env, declaration)
    }

    fun wrapAnnotationValue(annotation: KSAnnotation, value: KSValueArgument): KspAnnotationValue {
        return KspAnnotationValue.create(env, annotation, value)
    }
}
