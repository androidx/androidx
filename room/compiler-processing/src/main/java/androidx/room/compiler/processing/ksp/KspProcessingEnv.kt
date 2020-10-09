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
import org.jetbrains.kotlin.ksp.processing.CodeGenerator
import org.jetbrains.kotlin.ksp.processing.KSPLogger
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSType
import org.jetbrains.kotlin.ksp.symbol.KSTypeReference
import org.jetbrains.kotlin.ksp.symbol.Variance

internal class KspProcessingEnv(
    override val options: Map<String, String>,
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
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

    override val messager: XMessager
        get() = TODO("Not yet implemented")

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
        TODO("Not yet implemented")
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): XDeclaredType {
        TODO("Not yet implemented")
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

    fun wrap(ksType: KSType): KspType {
        return if (ksType.declaration.qualifiedName?.asString() == KOTLIN_ARRAY_Q_NAME) {
            KspArrayType(
                env = this,
                ksType = ksType
            )
        } else {
            KspType(
                env = this,
                ksType = ksType
            )
        }
    }

    fun wrap(ksTypeReference: KSTypeReference): KspType {
        return wrap(ksTypeReference.requireType())
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
