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

import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.XTypeElementStore
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

internal class KspProcessingEnv(
    override val options: Map<String, String>,
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    val resolver: Resolver
) : XProcessingEnv {

    private val typeElementStore =
        XTypeElementStore { qName ->
            resolver.getClassDeclarationByName(
                resolver.getKSNameFromString(qName)
            )?.let {
                KspTypeElement(
                    env = this,
                    declaration = it
                )
            }
        }

    override val messager: XMessager = KspMessager(logger)

    override val filer: XFiler = KspFiler(codeGenerator)

    val commonTypes = CommonTypes(resolver)

    override fun findTypeElement(qName: String): XTypeElement? {
        return typeElementStore[qName]
    }

    override fun findType(qName: String): XType? {
        return resolver.findClass(qName)?.let {
            wrap(it.asStarProjectedType())
        }
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        // this almost replicates what GeneratedAnnotations does except it doesn't check source
        // version because we don't have that property here yet. Instead, it tries the new one
        // first and falls back to the old one.
        return findTypeElement("javax.annotation.processing.Generated")
            ?: findTypeElement("javax.annotation.Generated")
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): XDeclaredType {
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
            type.declaration.asType(typeArguments)
        )
    }

    override fun getArrayType(type: XType): XArrayType {
        check(type is KspType)
        val arrayType = resolver.requireClass(KOTLIN_ARRAY_Q_NAME)
        val ksType = arrayType.asType(
            listOf(
                resolver.getTypeArgument(
                    type.ksType.createTypeReference(),
                    Variance.INVARIANT
                )
            )
        )
        return KspArrayType(
            env = this,
            ksType = ksType
        )
    }

    fun wrap(ksType: KSType): KspDeclaredType {
        return if (ksType.declaration.qualifiedName?.asString() == KOTLIN_ARRAY_Q_NAME) {
            KspArrayType(
                env = this,
                ksType = ksType
            )
        } else {
            KspDeclaredType(this, ksType)
        }
    }

    fun wrap(ksTypeReference: KSTypeReference): KspDeclaredType {
        return wrap(ksTypeReference.resolve())
    }

    fun wrap(ksTypeParam: KSTypeParameter, ksTypeArgument: KSTypeArgument): KspTypeArgumentType {
        return KspTypeArgumentType(
            env = this,
            typeArg = ksTypeArgument,
            typeParam = ksTypeParam
        )
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

    companion object {
        private const val KOTLIN_ARRAY_Q_NAME = "kotlin.Array"
    }
}
