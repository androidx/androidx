/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XValueArgument
import com.google.auto.common.MoreTypes
import java.lang.annotation.Repeatable
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

internal class JavacValueArgument(
    val env: JavacProcessingEnv,
    val element: ExecutableElement,
    val annotationValue: AnnotationValue
) : XValueArgument {
    override val name: String
        get() = element.simpleName.toString()

    override val value: Any? by lazy { annotationValue.unwrap() }

    private fun AnnotationValue.unwrap(): Any? {
        val value = value
        fun Any?.unwrapIfNeeded(): Any? {
            return if (this is AnnotationValue) this.unwrap() else this
        }
        return when {
            // The List implementation further wraps each value as a AnnotationValue.
            // We don't use arrays because we don't have reified type to instantiate the array
            // with, and using "Any" prevents the array from being cast to the correct
            // type later on.
            value is List<*> -> value.map { it.unwrapIfNeeded() }
            // Class types are represented as DeclaredType
            value is TypeMirror -> env.wrap(value, kotlinType = null, XNullability.NONNULL)
            value is AnnotationMirror -> {
                JavacAnnotation(env, value)
            }
            // Enums are wrapped in a variable element with kind ENUM_CONSTANT
            value is VariableElement -> {
                when {
                    value.kind == ElementKind.ENUM_CONSTANT -> {
                        val enumTypeElement = MoreTypes.asTypeElement(value.asType())
                        JavacEnumEntry(
                            env,
                            value,
                            JavacTypeElement.create(env, enumTypeElement) as XEnumTypeElement
                        )
                    }
                    else -> error("Unexpected annotation value $value for argument $name")
                }
            }
            else -> value
        }
    }
}