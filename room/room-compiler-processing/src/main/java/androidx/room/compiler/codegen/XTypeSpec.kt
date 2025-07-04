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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import androidx.room.compiler.codegen.impl.XTypeSpecImpl
import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import androidx.room.compiler.processing.XElement
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.JTypeSpec
import com.squareup.kotlinpoet.javapoet.KTypeSpec

interface XTypeSpec {

    val name: XName?

    fun toBuilder(): Builder

    interface Builder {
        fun superclass(typeName: XTypeName): Builder

        fun addSuperinterface(typeName: XTypeName): Builder

        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun addProperty(propertySpec: XPropertySpec): Builder

        fun addFunction(functionSpec: XFunSpec): Builder

        fun addType(typeSpec: XTypeSpec): Builder

        fun addTypeVariable(typeVariable: XTypeName): Builder

        fun addTypeVariables(typeVariables: List<XTypeName>) = apply {
            typeVariables.forEach { addTypeVariable(it) }
        }

        fun setPrimaryConstructor(functionSpec: XFunSpec): Builder

        fun setVisibility(visibility: VisibilityModifier): Builder

        fun addAbstractModifier(): Builder

        fun addOriginatingElement(element: XElement): Builder

        fun build(): XTypeSpec

        fun addProperty(
            name: String,
            typeName: XTypeName,
            visibility: VisibilityModifier,
            isMutable: Boolean = false,
            initExpr: XCodeBlock? = null,
        ) = apply {
            val builder = XPropertySpec.builder(name, typeName, visibility, isMutable)
            if (initExpr != null) {
                builder.initializer(initExpr)
            }
            addProperty(builder.build())
        }

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XTypeSpecImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaTypeSpec.Builder -> block(CodeLanguage.JAVA)
                    is KotlinTypeSpec.Builder -> block(CodeLanguage.KOTLIN)
                }
            }

            fun Builder.applyTo(language: CodeLanguage, block: Builder.() -> Unit) =
                applyTo { codeLanguage ->
                    if (codeLanguage == language) {
                        block()
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        fun classBuilder(name: String, isOpen: Boolean = false) =
            classBuilder(XName.of(name), isOpen)

        @JvmStatic
        fun classBuilder(className: XClassName, isOpen: Boolean = false) =
            classBuilder(
                XName.of(java = className.java.simpleName(), kotlin = className.kotlin.simpleName),
                isOpen,
            )

        @JvmStatic
        fun classBuilder(name: XName, isOpen: Boolean = false): Builder =
            XTypeSpecImpl.Builder(
                JavaTypeSpec.Builder(
                    JTypeSpec.classBuilder(name.java).apply {
                        if (!isOpen) {
                            addModifiers(JModifier.FINAL)
                        }
                    }
                ),
                KotlinTypeSpec.Builder(
                    KTypeSpec.classBuilder(name.kotlin).apply {
                        if (isOpen) {
                            addModifiers(KModifier.OPEN)
                        }
                    }
                ),
            )

        @JvmStatic
        fun anonymousClassBuilder(argsFormat: String = "", vararg args: Any?): Builder {
            val codeBlock = XCodeBlock.of(argsFormat, *args)
            require(codeBlock is XCodeBlockImpl)
            return XTypeSpecImpl.Builder(
                JavaTypeSpec.Builder(JTypeSpec.anonymousClassBuilder(codeBlock.java.actual)),
                KotlinTypeSpec.Builder(
                    KTypeSpec.anonymousClassBuilder().apply {
                        if (codeBlock.kotlin.actual.isNotEmpty()) {
                            addSuperclassConstructorParameter(codeBlock.kotlin.actual)
                        }
                    }
                ),
            )
        }

        @JvmStatic
        fun companionObjectBuilder(): Builder =
            XTypeSpecImpl.Builder(
                JavaTypeSpec.Builder(
                    JTypeSpec.classBuilder("Companion")
                        .addModifiers(JModifier.PUBLIC, JModifier.STATIC)
                ),
                KotlinTypeSpec.Builder(KTypeSpec.companionObjectBuilder()),
            )

        @JvmStatic
        fun objectBuilder(name: String): Builder =
            XTypeSpecImpl.Builder(
                JavaTypeSpec.Builder(JTypeSpec.classBuilder(name)),
                KotlinTypeSpec.Builder(KTypeSpec.objectBuilder(name)),
            )

        @JvmStatic
        fun objectBuilder(className: XClassName): Builder =
            XTypeSpecImpl.Builder(
                JavaTypeSpec.Builder(JTypeSpec.classBuilder(className.java)),
                KotlinTypeSpec.Builder(KTypeSpec.objectBuilder(className.kotlin)),
            )
    }
}
