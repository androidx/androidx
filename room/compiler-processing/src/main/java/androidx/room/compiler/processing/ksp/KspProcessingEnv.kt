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
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import java.lang.reflect.Method

internal class KspProcessingEnv(
    override val options: Map<String, String>,
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    val resolver: Resolver
) : XProcessingEnv {
    override val backend: XProcessingEnv.Backend = XProcessingEnv.Backend.KSP

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

    private val arrayTypeFactory = KspArrayType.Factory(this)

    override val filer: XFiler = KspFiler(codeGenerator)

    val commonTypes = CommonTypes(resolver)

    val voidType by lazy {
        KspVoidType(
            env = this,
            ksType = resolver.builtIns.unitType,
            boxed = false
        )
    }

    override fun findTypeElement(qName: String): XTypeElement? {
        return typeElementStore[qName]
    }

    override fun findType(qName: String): XType? {
        val kotlinTypeName = KspTypeMapper.swapWithKotlinType(qName)
        return resolver.findClass(kotlinTypeName)?.let {
            wrap(
                allowPrimitives = KspTypeMapper.isJavaPrimitiveType(qName),
                ksType = it.asStarProjectedType()
            )
        }
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        // this almost replicates what GeneratedAnnotations does except it doesn't check source
        // version because we don't have that property here yet. Instead, it tries the new one
        // first and falls back to the old one.
        // implement when https://github.com/google/ksp/issues/198 is fixed
        return null
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
            typeParam = ksTypeParam
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
        val qName = ksType.declaration.qualifiedName?.asString()
        val declaration = ksType.declaration
        if (declaration is KSTypeParameter) {
            return KspTypeArgumentType(
                env = this,
                typeArg = resolver.getTypeArgument(
                    ksType.createTypeReference(),
                    declaration.variance
                ),
                typeParam = declaration
            )
        }
        if (allowPrimitives && qName != null && ksType.nullability == Nullability.NOT_NULL) {
            // check for primitives
            val javaPrimitive = KspTypeMapper.getPrimitiveJavaTypeName(qName)
            if (javaPrimitive != null) {
                return KspPrimitiveType(this, ksType)
            }
            // special case for void
            if (qName == "kotlin.Unit") {
                return voidType
            }
        }
        return arrayTypeFactory.createIfArray(ksType) ?: DefaultKspType(this, ksType)
    }

    fun wrapClassDeclaration(declaration: KSClassDeclaration): KspTypeElement {
        return typeElementStore[declaration]
    }

    // workaround until KSP stops returning fake overrides from methods.
    private val fakeChecker = FakeOverrideChecker()

    fun isFakeOverride(ksFunctionDeclaration: KSFunctionDeclaration): Boolean {
        return fakeChecker.isFakeOverride(ksFunctionDeclaration)
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

    /**
     * Current version of KSP returns fake methods which kotlin generates for overrides. This
     * create exceptions later on when we try to resolve them, and we don't want them.
     * This class is a temporary band aid to filter them out.
     * KSP already fixed this issue in 1.4.20-dev-experimental-20201222 but we cannot update to
     * that version yet due to https://github.com/google/ksp/issues/216
     * https://github.com/google/ksp/commit/93675bef1dc20be096ada7afa4baa46e04acdb12
     */
    @Suppress("BanUncheckedReflection")
    private class FakeOverrideChecker {
        private lateinit var kindMethod: Method
        private lateinit var fakeOverrideKind: Any

        private fun initializeIfNecessary(ref: Any) {
            if (this::kindMethod.isInitialized) {
                return
            }
            val callableDescriptorClass = ref.javaClass.classLoader
                .loadClass(CALLABLE_MEMBER_DESCRIPTOR_CLASS_NAME)
            kindMethod = callableDescriptorClass.getDeclaredMethod("getKind")
            check(kindMethod.returnType.isEnum) {
                "expected getKind method to return an enum"
            }
            fakeOverrideKind = kindMethod.returnType.enumConstants.firstOrNull {
                it.toString() == FAKE_OVERRIDE_ENUM_VALUE
            } ?: error("cannot find FAKE_OVERRIDE enum constant in ${kindMethod.returnType}")
        }

        /**
         * Return true if this is a FAKE_OVERRIDE which kotlin generates for overrides when there
         * is no real override and method is inherited.
         */
        fun isFakeOverride(declaration: KSFunctionDeclaration): Boolean {
            if (declaration.origin != Origin.CLASS) return false

            val descriptorField = try {
                declaration::class.java.getDeclaredField("descriptor").also {
                    it.trySetAccessible()
                }
            } catch (ignored: Throwable) {
                null
            }
            val descriptor = descriptorField?.get(declaration) ?: return false
            initializeIfNecessary(declaration)
            return fakeOverrideKind == kindMethod.invoke(descriptor)
        }

        companion object {
            private val CALLABLE_MEMBER_DESCRIPTOR_CLASS_NAME =
                "org.jetbrains.kotlin.descriptors.CallableMemberDescriptor"
            private val FAKE_OVERRIDE_ENUM_VALUE = "FAKE_OVERRIDE"
        }
    }
}
