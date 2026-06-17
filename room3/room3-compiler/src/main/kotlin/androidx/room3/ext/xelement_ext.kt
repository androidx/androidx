/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room3.ext

import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.processing.XConstructorElement
import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XExecutableElement
import androidx.room3.compiler.processing.XExecutableParameterElement
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XPropertyElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import kotlin.contracts.contract
import kotlin.reflect.KClass

fun XPropertyElement.hasAnnotationOnPropertyOrField(annotation: KClass<out Annotation>) =
    hasAnnotation(annotation) || (backingField?.hasAnnotation(annotation) ?: false)

fun <T : Annotation> XPropertyElement.getAnnotationOnPropertyOrField(annotation: KClass<T>) =
    getAnnotation(annotation) ?: backingField?.getAnnotation(annotation)

fun XPropertyElement.hasAnyAnnotationOnPropertyOrField(vararg annotations: KClass<out Annotation>) =
    annotations.any { hasAnnotationOnPropertyOrField(it) }

fun <T : Annotation> XPropertyElement.requireAnnotationOnPropertyOrField(annotation: KClass<T>) =
    requireNotNull(getAnnotationOnPropertyOrField(annotation)) {
        "Missing annotation $annotation on $this"
    }

fun XPropertyElement.requireAnnotationOnPropertyOrField(annotationName: XClassName) =
    requireNotNull(getAnnotationOnPropertyOrField(annotationName)) {
        "Missing annotation $annotationName on $this"
    }

fun XPropertyElement.getAnnotationOnPropertyOrField(annotationName: XClassName) =
    getAnnotation(annotationName) ?: backingField?.getAnnotation(annotationName)

fun XElement.isEntityElement(): Boolean {
    contract { returns(true) implies (this@isEntityElement is XTypeElement) }
    return this.hasAnnotation(androidx.room3.Entity::class)
}

fun XTypeElement.getValueClassUnderlyingInfo(): ValueClassInfo {
    check(this.isValueClass()) {
        "Can't get value class property, type element '$this' is not a value class"
    }
    // Kotlin states:
    // * Primary constructor is required for value class
    // * Value class must have exactly one primary constructor parameter
    // * Value class primary constructor must only have final read-only (val) property parameter
    val constructor =
        checkNotNull(this.findPrimaryConstructor()) {
            "Couldn't find primary constructor for value class."
        }
    val param = constructor.parameters.first()
    val getter =
        getDeclaredMethods().firstOrNull {
            it.isKotlinPropertyGetter() && it.propertyName == param.name
        }
    return ValueClassInfo(constructor, param, getter)
}

/** Store information about the underlying value property of a Kotlin value class */
class ValueClassInfo(
    val constructor: XConstructorElement,
    val parameter: XExecutableParameterElement,
    val getter: XMethodElement?,
)

/**
 * Utility for getting the parameters needed to call a function from generated Kotlin code,
 * excluding the Coroutine parameter if it is a suspend function.
 */
fun XExecutableElement.getRequiredFunctionParamTypes(): List<XType> {
    val adjustedSize =
        if (parameters.last().isContinuationParam()) {
            parameters.size - 1
        } else {
            parameters.size
        }
    return parameters.subList(0, adjustedSize).map { it.type }
}
