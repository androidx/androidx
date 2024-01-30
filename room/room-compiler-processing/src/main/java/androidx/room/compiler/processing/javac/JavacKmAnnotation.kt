/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.room.compiler.processing.InternalXAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.javac.kotlin.KmAnnotationContainer

internal class JavacKmAnnotation(
    private val env: JavacProcessingEnv,
    private val kmAnnotation: KmAnnotationContainer
) : InternalXAnnotation() {
    override fun <T : Annotation> asAnnotationBox(annotationClass: Class<T>): XAnnotationBox<T> {
        throw UnsupportedOperationException("No plan to support XAnnotationBox.")
    }

    override val name: String
        get() = typeElement.name

    override val qualifiedName: String
        get() = typeElement.qualifiedName

    override val typeElement: JavacTypeElement by lazy {
        requireNotNull(env.findTypeElement(kmAnnotation.className))
    }

    override val type: XType
        get() = typeElement.type

    override val declaredAnnotationValues: List<XAnnotationValue> by lazy {
        methodToDeclaredAnnotationValues.values.filterNotNull()
    }

    override val annotationValues: List<XAnnotationValue> by lazy {
        methodToDeclaredAnnotationValues.mapNotNull { (method, annotationValue) ->
            annotationValue ?: method.defaultValue
        }
    }

    private val methodToDeclaredAnnotationValues:
            Map<JavacMethodElement, XAnnotationValue?> by lazy {
        val methods = typeElement.getDeclaredMethods()
        val kmAnnotationArguments = kmAnnotation.getArguments(env)
        methods.associateWith { method ->
            // KmAnnotation doesn't include arguments with default values
            kmAnnotationArguments[method.jvmName]?.let {
                JavacKmAnnotationValue(
                    method = method,
                    kmAnnotationArgumentContainer = it
                )
            }
        }
    }

    override val defaultValues: List<XAnnotationValue> by lazy {
        typeElement.getDeclaredMethods().mapNotNull { it.defaultValue }
    }
}
