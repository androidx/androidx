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

package androidx.room.compiler.codegen.java

import androidx.room.compiler.codegen.L
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.kotlinpoet.javapoet.JTypeName
import javax.lang.model.element.Modifier

internal class JavaFunSpec(override val name: String, internal val actual: MethodSpec) :
    JavaLang(), XFunSpec {
    override fun toString() = actual.toString()

    internal class Builder(override val name: String, internal val actual: MethodSpec.Builder) :
        JavaLang(), XFunSpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is JavaAnnotationSpec)
            actual.addAnnotation(annotation.actual)
        }

        override fun addAbstractModifier() = apply { actual.addModifiers(Modifier.ABSTRACT) }

        override fun addCode(code: XCodeBlock) = apply {
            require(code is JavaCodeBlock)
            actual.addCode(code.actual)
        }

        override fun addParameter(
            typeName: XTypeName,
            name: String,
            annotations: List<XAnnotationSpec>
        ) = apply {
            val paramSpec = ParameterSpec.builder(typeName.java, name, Modifier.FINAL)
            actual.addParameter(
                // Adding nullability annotation to primitive parameters is redundant as
                // primitives can never be null.
                if (typeName.isPrimitive) {
                    paramSpec.build()
                } else {
                    when (typeName.nullability) {
                        XNullability.NULLABLE -> paramSpec.addAnnotation(NULLABLE_ANNOTATION)
                        XNullability.NONNULL -> paramSpec.addAnnotation(NONNULL_ANNOTATION)
                        else -> paramSpec
                    }.build()
                }
            )
            // TODO(b/247247439): Add other annotations
        }

        override fun callSuperConstructor(vararg args: XCodeBlock) = apply {
            actual.addStatement(
                "super($L)",
                CodeBlock.join(
                    args.map {
                        check(it is JavaCodeBlock)
                        it.actual
                    },
                    ", "
                )
            )
        }

        override fun returns(typeName: XTypeName) = apply {
            if (typeName.java == JTypeName.VOID) {
                return@apply
            }
            // TODO(b/247242374) Add nullability annotations for non-private methods
            if (!actual.modifiers.contains(Modifier.PRIVATE)) {
                if (typeName.nullability == XNullability.NULLABLE) {
                    actual.addAnnotation(NULLABLE_ANNOTATION)
                } else if (typeName.nullability == XNullability.NONNULL) {
                    actual.addAnnotation(NONNULL_ANNOTATION)
                }
            }
            actual.returns(typeName.java)
        }

        override fun build() = JavaFunSpec(name, actual.build())
    }
}

internal fun VisibilityModifier.toJavaVisibilityModifier() =
    when (this) {
        VisibilityModifier.PUBLIC -> Modifier.PUBLIC
        VisibilityModifier.PROTECTED -> Modifier.PROTECTED
        VisibilityModifier.INTERNAL -> Modifier.PUBLIC
        VisibilityModifier.PRIVATE -> Modifier.PRIVATE
    }
