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

import androidx.room.compiler.codegen.JCodeBlock
import androidx.room.compiler.codegen.JFunSpec
import androidx.room.compiler.codegen.JFunSpecBuilder
import androidx.room.compiler.codegen.L
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XParameterSpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import androidx.room.compiler.codegen.impl.XParameterSpecImpl
import androidx.room.compiler.processing.XNullability
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import javax.lang.model.element.Modifier

internal class JavaFunSpec(
    private val addJavaNullabilityAnnotation: Boolean,
    override val actual: JFunSpec,
) : JavaSpec<JFunSpec>(), XFunSpec {

    override val name = XName.of(actual.name)

    override fun toBuilder() = Builder(addJavaNullabilityAnnotation, actual.toBuilder())

    override fun toString() = actual.toString()

    internal class Builder(
        private val addJavaNullabilityAnnotation: Boolean,
        internal val actual: JFunSpecBuilder,
    ) : XSpec.Builder(), XFunSpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.java.actual)
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            require(typeVariable.java is JTypeVariableName)
            actual.addTypeVariable(typeVariable.java as JTypeVariableName)
        }

        override fun addAbstractModifier() = apply { actual.addModifiers(Modifier.ABSTRACT) }

        override fun addParameter(parameter: XParameterSpec) = apply {
            require(parameter is XParameterSpecImpl)
            actual.addParameter(parameter.java.actual)
        }

        override fun addParameter(name: String, typeName: XTypeName) =
            addParameter(
                XParameterSpec.builder(name, typeName, addJavaNullabilityAnnotation).build()
            )

        override fun addCode(code: XCodeBlock) = apply {
            require(code is XCodeBlockImpl)
            actual.addCode(code.java.actual)
        }

        override fun callSuperConstructor(vararg args: XCodeBlock) = apply {
            actual.addStatement(
                "super($L)",
                JCodeBlock.join(
                    args.map {
                        require(it is XCodeBlockImpl)
                        it.java.actual
                    },
                    ", ",
                ),
            )
        }

        override fun returns(typeName: XTypeName) = apply {
            if (typeName.java == JTypeName.VOID) {
                return@apply
            }
            if (addJavaNullabilityAnnotation) {
                // TODO(b/247242374) Add nullability annotations for non-private methods
                if (!actual.modifiers.contains(Modifier.PRIVATE)) {
                    if (typeName.nullability == XNullability.NULLABLE) {
                        actual.addAnnotation(NULLABLE_ANNOTATION)
                    } else if (typeName.nullability == XNullability.NONNULL) {
                        actual.addAnnotation(NONNULL_ANNOTATION)
                    }
                }
            }
            actual.returns(typeName.java)
        }

        override fun build() = JavaFunSpec(addJavaNullabilityAnnotation, actual.build())
    }
}

internal fun VisibilityModifier.toJavaVisibilityModifier() =
    when (this) {
        VisibilityModifier.PUBLIC -> Modifier.PUBLIC
        VisibilityModifier.PROTECTED -> Modifier.PROTECTED
        VisibilityModifier.INTERNAL -> Modifier.PUBLIC
        VisibilityModifier.PRIVATE -> Modifier.PRIVATE
    }
