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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance

internal class KspProcessingEnv(
    override val options: Map<String, String>,
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    val resolver: Resolver
) : XProcessingEnv {
    override val backend: XProcessingEnv.Backend = XProcessingEnv.Backend.KSP

    private val typeElementStore =
        XTypeElementStore { qName ->
            resolver.getClassDeclarationByName(
                KspTypeMapper.swapWithKotlinType(qName)
            )?.let {
                KspTypeElement(
                    env = this,
                    declaration = it
                )
            }
        }

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
        return findTypeElement("javax.annotation.processing.Generated")
            ?: findTypeElement("javax.annotation.Generated")
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): KspDeclaredType {
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
        val result = wrap(
            ksType = type.declaration.asType(typeArguments),
            allowPrimitives = false
        )
        check(result is KspDeclaredType) {
            "Expected $type to be a declared type but a non-declared type ($result) is received"
        }
        return result
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

    fun wrap(ksTypeParam: KSTypeParameter, ksTypeArgument: KSTypeArgument): KspTypeArgumentType {
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
        return arrayTypeFactory.createIfArray(ksType) ?: KspDeclaredType(this, ksType)
    }

    fun wrapClassDeclaration(declaration: KSClassDeclaration): KspTypeElement {
        return KspTypeElement(
            env = this,
            declaration = declaration
        )
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
