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

import androidx.room.compiler.codegen.impl.XPropertySpecImpl
import androidx.room.compiler.codegen.java.JavaPropertySpec
import androidx.room.compiler.codegen.java.NONNULL_ANNOTATION
import androidx.room.compiler.codegen.java.NULLABLE_ANNOTATION
import androidx.room.compiler.codegen.java.toJavaVisibilityModifier
import androidx.room.compiler.codegen.kotlin.KotlinPropertySpec
import androidx.room.compiler.codegen.kotlin.toKotlinVisibilityModifier
import androidx.room.compiler.processing.XNullability

interface XPropertySpec {

    val name: String

    val type: XTypeName

    fun toBuilder(): Builder

    interface Builder {
        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun initializer(initExpr: XCodeBlock): Builder

        fun build(): XPropertySpec

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XPropertySpecImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaPropertySpec.Builder -> block(CodeLanguage.JAVA)
                    is KotlinPropertySpec.Builder -> block(CodeLanguage.KOTLIN)
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
            typeName: XTypeName,
            visibility: VisibilityModifier,
            isMutable: Boolean = false,
            addJavaNullabilityAnnotation: Boolean = true,
        ): Builder =
            XPropertySpecImpl.Builder(
                name,
                typeName,
                JavaPropertySpec.Builder(
                    name,
                    typeName,
                    JPropertySpec.builder(typeName.java, name).apply {
                        val visibilityModifier = visibility.toJavaVisibilityModifier()
                        addModifiers(visibilityModifier)
                        if (addJavaNullabilityAnnotation) {
                            // TODO(b/247242374) Add nullability annotations for private fields
                            if (visibilityModifier != JModifier.PRIVATE) {
                                if (typeName.nullability == XNullability.NULLABLE) {
                                    addAnnotation(NULLABLE_ANNOTATION)
                                } else if (typeName.nullability == XNullability.NONNULL) {
                                    addAnnotation(NONNULL_ANNOTATION)
                                }
                            }
                        }
                        if (!isMutable) {
                            addModifiers(JModifier.FINAL)
                        }
                    },
                ),
                KotlinPropertySpec.Builder(
                    name,
                    typeName,
                    KPropertySpec.builder(name, typeName.kotlin).apply {
                        mutable(isMutable)
                        addModifiers(visibility.toKotlinVisibilityModifier())
                    },
                ),
            )
    }
}
