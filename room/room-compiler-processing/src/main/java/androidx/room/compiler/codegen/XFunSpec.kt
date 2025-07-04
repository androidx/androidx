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

import androidx.room.compiler.codegen.impl.XFunSpecImpl
import androidx.room.compiler.codegen.java.JavaFunSpec
import androidx.room.compiler.codegen.java.toJavaVisibilityModifier
import androidx.room.compiler.codegen.kotlin.KotlinFunSpec
import androidx.room.compiler.codegen.kotlin.toKotlinVisibilityModifier
import androidx.room.compiler.processing.FunSpecHelper
import androidx.room.compiler.processing.MethodSpecHelper
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import com.squareup.kotlinpoet.KModifier

interface XFunSpec {

    val name: XName

    fun toBuilder(): Builder

    interface Builder {

        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun addTypeVariable(typeVariable: XTypeName): Builder

        fun addTypeVariables(typeVariables: List<XTypeName>) = apply {
            typeVariables.forEach { addTypeVariable(it) }
        }

        fun addAbstractModifier(): Builder

        fun addParameter(parameter: XParameterSpec): Builder

        fun addParameter(name: String, typeName: XTypeName): Builder

        fun addParameters(parameters: List<XParameterSpec>) = apply {
            parameters.forEach { addParameter(it) }
        }

        fun addCode(code: XCodeBlock): Builder

        fun addCode(format: String, vararg args: Any?) =
            addCode(XCodeBlock.builder().add(format, *args).build())

        fun addStatement(format: String, vararg args: Any?) =
            addCode(XCodeBlock.builder().addStatement(format, *args).build())

        fun callSuperConstructor(vararg args: XCodeBlock): Builder

        fun returns(typeName: XTypeName): Builder

        fun build(): XFunSpec

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XFunSpecImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaFunSpec.Builder -> block(CodeLanguage.JAVA)
                    is KotlinFunSpec.Builder -> block(CodeLanguage.KOTLIN)
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
        fun builder(
            name: String,
            visibility: VisibilityModifier,
            isOpen: Boolean = false,
            isOverride: Boolean = false,
            addJavaNullabilityAnnotation: Boolean = true,
        ) = builder(XName.of(name), visibility, isOpen, isOverride, addJavaNullabilityAnnotation)

        @JvmStatic
        fun builder(
            name: XName,
            visibility: VisibilityModifier,
            isOpen: Boolean = false,
            isOverride: Boolean = false,
            addJavaNullabilityAnnotation: Boolean = true,
        ): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(
                    addJavaNullabilityAnnotation,
                    JFunSpec.methodBuilder(name.java).apply {
                        addModifiers(visibility.toJavaVisibilityModifier())
                        // TODO(b/247242374) Add nullability annotations for non-private params
                        // if (!isOpen) {
                        //    addModifiers(Modifier.FINAL)
                        // }
                        if (isOverride) {
                            addAnnotation(Override::class.java)
                        }
                    },
                ),
                KotlinFunSpec.Builder(
                    KFunSpec.builder(name.kotlin).apply {
                        addModifiers(visibility.toKotlinVisibilityModifier())
                        if (isOpen) {
                            addModifiers(KModifier.OPEN)
                        }
                        if (isOverride) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                    }
                ),
            )

        @JvmStatic
        fun constructorBuilder(
            visibility: VisibilityModifier,
            addJavaNullabilityAnnotation: Boolean = true,
        ): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(
                    addJavaNullabilityAnnotation,
                    JFunSpec.constructorBuilder().apply {
                        addModifiers(visibility.toJavaVisibilityModifier())
                    },
                ),
                KotlinFunSpec.Builder(
                    KFunSpec.constructorBuilder().apply {
                        // Workaround for the unreleased fix in
                        // https://github.com/square/kotlinpoet/pull/1342
                        if (visibility != VisibilityModifier.PUBLIC) {
                            addModifiers(visibility.toKotlinVisibilityModifier())
                        }
                    }
                ),
            )

        @JvmStatic
        fun overridingBuilder(
            element: XMethodElement,
            owner: XType,
            addJavaNullabilityAnnotation: Boolean = true,
        ): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(
                    addJavaNullabilityAnnotation,
                    MethodSpecHelper.overridingWithFinalParams(element, owner),
                ),
                KotlinFunSpec.Builder(FunSpecHelper.overriding(element, owner)),
            )
    }
}
