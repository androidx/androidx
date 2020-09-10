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

package androidx.room.compiler.processing

import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * Represents an element declared in code.
 *
 * @see [javax.lang.model.element.Element]
 * @see XExecutableElement
 * @see XVariableElement
 * @see XTypeElement
 */
interface XElement {
    /**
     * Returns the string representation of the Element's kind.
     */
    fun kindName(): String

    /**
     * If the current element has an annotation with the given [annotation] class, a boxed instance
     * of it will be returned where fields can be read. Otherwise, `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>?

    /**
     * Returns `true` if this element has an annotation that is declared in the given package.
     */
    // a very sad method but helps avoid abstraction annotation
    fun hasAnnotationWithPackage(pkg: String): Boolean

    /**
     * Returns `true` if this element is annotated with the given [annotation].
     *
     * @see [toAnnotationBox]
     * @see [hasAnyOf]
     */
    fun hasAnnotation(annotation: KClass<out Annotation>): Boolean

    /**
     * Returns `true` if this element has one of the [annotations].
     */
    fun hasAnyOf(vararg annotations: KClass<out Annotation>) = annotations.any(this::hasAnnotation)

    /**
     * Casts current element to [XTypeElement].
     */
    fun asTypeElement() = this as XTypeElement

    /**
     * Casts current element to [XVariableElement].
     */
    fun asVariableElement() = this as XVariableElement

    /**
     * Casts current element to [XMethodElement].
     */
    fun asMethodElement() = this as XMethodElement

    /**
     * Returns the [XDeclaredType] type of the current element, assuming it is an [XTypeElement].
     * It is a shortcut for `asTypeElement().type`.
     */
    fun asDeclaredType(): XDeclaredType {
        return asTypeElement().type
    }
}

/**
 * Checks whether this element represents an [XTypeElement].
 * @see [XElement.asTypeElement]
 */
// we keep these as extension methods to be able to use contracts
fun XElement.isType(): Boolean {
    contract {
        returns(true) implies (this@isType is XTypeElement)
    }
    return this is XTypeElement
}

/**
 * Checks whether this element represents an [XVariableElement].
 * @see [XElement.asVariableElement]
 */
fun XElement.isVariableElement(): Boolean {
    contract {
        returns(true) implies (this@isVariableElement is XVariableElement)
    }
    return this is XVariableElement
}

/**
 * Checks whether this element represents an [XMethodElement].
 * @see [XElement.asMethodElement]
 */
fun XElement.isMethod(): Boolean {
    contract {
        returns(true) implies (this@isMethod is XMethodElement)
    }
    return this is XMethodElement
}

fun XElement.isConstructor(): Boolean {
    contract {
        returns(true) implies (this@isConstructor is XConstructorElement)
    }
    return this is XConstructorElement
}
