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

import androidx.room.compiler.codegen.java.JavaCodeBlock
import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinCodeBlock
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.addOriginatingElement
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.JTypeSpec
import com.squareup.kotlinpoet.javapoet.KTypeSpec
import javax.lang.model.element.Modifier

interface XTypeSpec : TargetLanguage {

    val className: XClassName

    interface Builder : TargetLanguage {
        fun superclass(typeName: XTypeName): Builder
        fun addSuperinterface(typeName: XTypeName): Builder
        fun addAnnotation(annotation: XAnnotationSpec)
        fun addProperty(propertySpec: XPropertySpec): Builder
        fun addFunction(functionSpec: XFunSpec): Builder
        fun addType(typeSpec: XTypeSpec): Builder
        fun setPrimaryConstructor(functionSpec: XFunSpec): Builder
        fun setVisibility(visibility: VisibilityModifier)
        fun addAbstractModifier(): Builder
        fun build(): XTypeSpec

        companion object {

            fun Builder.addOriginatingElement(element: XElement) = apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        check(this is JavaTypeSpec.Builder)
                        actual.addOriginatingElement(element)
                    }
                    CodeLanguage.KOTLIN -> {
                        check(this is KotlinTypeSpec.Builder)
                        actual.addOriginatingElement(element)
                    }
                }
            }

            fun Builder.addProperty(
                name: String,
                typeName: XTypeName,
                visibility: VisibilityModifier,
                isMutable: Boolean = false,
                initExpr: XCodeBlock? = null,
            ) = apply {
                val builder = XPropertySpec.builder(language, name, typeName, visibility, isMutable)
                if (initExpr != null) {
                    builder.initializer(initExpr)
                }
                addProperty(builder.build())
            }

            fun Builder.apply(
                javaTypeBuilder: com.squareup.javapoet.TypeSpec.Builder.() -> Unit,
                kotlinTypeBuilder: com.squareup.kotlinpoet.TypeSpec.Builder.() -> Unit,
            ): Builder = apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        check(this is JavaTypeSpec.Builder)
                        this.actual.javaTypeBuilder()
                    }
                    CodeLanguage.KOTLIN -> {
                        check(this is KotlinTypeSpec.Builder)
                        this.actual.kotlinTypeBuilder()
                    }
                }
            }
        }
    }

    companion object {
        fun classBuilder(
            language: CodeLanguage,
            className: XClassName,
            isOpen: Boolean = false
        ): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaTypeSpec.Builder(
                    className = className,
                    actual = JTypeSpec.classBuilder(className.java).apply {
                        if (!isOpen) {
                            addModifiers(Modifier.FINAL)
                        }
                    }
                )

                CodeLanguage.KOTLIN -> KotlinTypeSpec.Builder(
                    className = className,
                    actual = KTypeSpec.classBuilder(className.kotlin).apply {
                        if (isOpen) {
                            addModifiers(KModifier.OPEN)
                        }
                    }
                )
            }
        }

        fun anonymousClassBuilder(
            language: CodeLanguage,
            argsFormat: String = "",
            vararg args: Any
        ): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaTypeSpec.Builder(
                    className = null,
                    actual = JTypeSpec.anonymousClassBuilder(
                        XCodeBlock.of(language, argsFormat, *args).let {
                            check(it is JavaCodeBlock)
                            it.actual
                        }
                    )
                )
                CodeLanguage.KOTLIN -> KotlinTypeSpec.Builder(
                    className = null,
                    actual = KTypeSpec.anonymousClassBuilder().apply {
                        if (args.isNotEmpty()) {
                            addSuperclassConstructorParameter(
                                XCodeBlock.of(language, argsFormat, *args).let {
                                    check(it is KotlinCodeBlock)
                                    it.actual
                                }
                            )
                        }
                    }
                )
            }
        }

        fun companionObjectBuilder(language: CodeLanguage): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaTypeSpec.Builder(
                    className = null,
                    actual = JTypeSpec.classBuilder("Companion")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                )
                CodeLanguage.KOTLIN -> KotlinTypeSpec.Builder(
                    className = null,
                    actual = KTypeSpec.companionObjectBuilder()
                )
            }
        }
    }
}
