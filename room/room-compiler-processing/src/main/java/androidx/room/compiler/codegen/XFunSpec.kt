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

import androidx.room.compiler.codegen.java.JavaFunSpec
import androidx.room.compiler.codegen.java.toJavaVisibilityModifier
import androidx.room.compiler.codegen.kotlin.KotlinFunSpec
import androidx.room.compiler.codegen.kotlin.toKotlinVisibilityModifier
import com.squareup.javapoet.MethodSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier

interface XFunSpec : TargetLanguage {

    val name: String

    interface Builder : TargetLanguage {

        fun addAnnotation(annotation: XAnnotationSpec)

        // TODO(b/247247442): Maybe make a XParameterSpec ?
        fun addParameter(
            typeName: XTypeName,
            name: String,
            annotations: List<XAnnotationSpec> = emptyList()
        ): Builder

        fun addCode(
            code: XCodeBlock
        ): Builder

        fun callSuperConstructor(vararg args: XCodeBlock): Builder

        fun returns(typeName: XTypeName): Builder

        fun build(): XFunSpec

        companion object {
            fun Builder.addStatement(format: String, vararg args: Any?) = addCode(
                XCodeBlock.builder(language).addStatement(format, *args).build()
            )

            fun Builder.apply(
                javaMethodBuilder: MethodSpec.Builder.() -> Unit,
                kotlinFunBuilder: FunSpec.Builder.() -> Unit,
            ): Builder = apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        check(this is JavaFunSpec.Builder)
                        this.actual.javaMethodBuilder()
                    }
                    CodeLanguage.KOTLIN -> {
                        check(this is KotlinFunSpec.Builder)
                        this.actual.kotlinFunBuilder()
                    }
                }
            }
        }
    }

    companion object {
        fun builder(
            language: CodeLanguage,
            name: String,
            visibility: VisibilityModifier,
            isOpen: Boolean = false,
            isOverridden: Boolean = false
        ): Builder {
            return when (language) {
                CodeLanguage.JAVA -> {
                    JavaFunSpec.Builder(
                        name,
                        MethodSpec.methodBuilder(name).apply {
                            addModifiers(visibility.toJavaVisibilityModifier())
                            // TODO(b/247242374) Add nullability annotations for non-private params
                            // if (!isOpen) {
                            //    addModifiers(Modifier.FINAL)
                            // }
                            if (isOverridden) {
                                addAnnotation(Override::class.java)
                            }
                        }
                    )
                }
                CodeLanguage.KOTLIN -> {
                    KotlinFunSpec.Builder(
                        name,
                        FunSpec.builder(name).apply {
                            addModifiers(visibility.toKotlinVisibilityModifier())
                            if (isOpen) {
                                addModifiers(KModifier.OPEN)
                            }
                            if (isOverridden) {
                                addModifiers(KModifier.OVERRIDE)
                            }
                        }
                    )
                }
            }
        }

        fun constructorBuilder(
            language: CodeLanguage,
            visibility: VisibilityModifier
        ): Builder {
            val name = "<init>"
            return when (language) {
                CodeLanguage.JAVA -> {
                    JavaFunSpec.Builder(
                        name,
                        MethodSpec.constructorBuilder().apply {
                            addModifiers(visibility.toJavaVisibilityModifier())
                        }
                    )
                }
                CodeLanguage.KOTLIN -> {
                    KotlinFunSpec.Builder(
                        name,
                        FunSpec.constructorBuilder().apply {
                            addModifiers(visibility.toKotlinVisibilityModifier())
                        }
                    )
                }
            }
        }
    }
}