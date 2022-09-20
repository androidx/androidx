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
import androidx.room.compiler.processing.XNullability
import com.squareup.javapoet.MethodSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.JTypeName

interface XFunSpec : TargetLanguage {

    interface Builder : TargetLanguage {
        // TODO(b/247247442): Maybe make a XParameterSpec ?
        fun addParameter(
            typeName: JTypeName,
            name: String,
            nullability: XNullability,
            annotations: List<XAnnotationSpec> = emptyList()
        ): Builder

        fun addCode(
            code: XCodeBlock
        ): Builder

        fun callSuperConstructor(vararg args: XCodeBlock): Builder

        fun returns(
            typeName: JTypeName,
            nullability: XNullability,
        ): Builder

        fun build(): XFunSpec

        companion object {
            fun Builder.addStatement(format: String, vararg args: Any?) = addCode(
                XCodeBlock.builder(language).addStatement(format, *args).build()
            )
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
            return when (language) {
                CodeLanguage.JAVA -> {
                    JavaFunSpec.Builder(
                        MethodSpec.constructorBuilder().apply {
                            addModifiers(visibility.toJavaVisibilityModifier())
                        }
                    )
                }
                CodeLanguage.KOTLIN -> {
                    KotlinFunSpec.Builder(
                        FunSpec.constructorBuilder().apply {
                            addModifiers(visibility.toKotlinVisibilityModifier())
                        }
                    )
                }
            }
        }
    }
}