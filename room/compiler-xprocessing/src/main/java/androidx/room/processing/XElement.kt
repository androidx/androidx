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

package androidx.room.processing

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
     * SimpleName of the element converted to a String.
     *
     * @see [javax.lang.model.element.Element.getSimpleName]
     */
    val name: String

    /**
     * The qualified name of the package that contains this element.
     */
    val packageName: String

    /**
     * Nullability of an Element as declared in code via its type or annotations (e.g.
     * [androidx.annotation.Nullable].
     * This will be moved into [XType].
     *
     * TODO:
     *  Nullability is normally a property of Type not Element but currently Room relies on
     *  Annotations to resolve nullability which exists only on Elements, not Types.
     *  Once we implement KSP version, we might be able to move this to the type by making sure
     *  we carry over nullability when type is resolved from an Element. We also need nullability
     *  on Types to properly handle DAO return types (e.g. Flow<T> vs Flow<T?>)
     */
    val nullability: XNullability

    /**
     * The [XElement] that contains this element.
     *
     * For inner classes, this will be another [XTypeElement].
     * For top level classes, it will be null as x-processing does not model packages or modules.
     *
     * For [XExecutableElement], it will be the [XTypeElement] where the method is declared.
     */
    val enclosingElement: XElement?

    /**
     * Returns `true` if this element is public (has public modifier in Java or not marked as
     * private / internal in Kotlin).
     */
    fun isPublic(): Boolean

    /**
     * Returns `true` if this element has protected modifier.
     */
    fun isProtected(): Boolean

    /**
     * Returns `true` if this element is declared as abstract.
     */
    fun isAbstract(): Boolean

    /**
     * Returns `true` if this element has private modifier.
     */
    fun isPrivate(): Boolean

    /**
     * Returns `true` if this element has static modifier.
     */
    fun isStatic(): Boolean

    /**
     * Returns `true` if this element has transient modifier.
     */
    fun isTransient(): Boolean

    /**
     * Returns `true` if this element is final and cannot be overridden.
     */
    fun isFinal(): Boolean

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
     * Returns `true` if and only if this element can never be null.
     * For Java source code, this means the element is either primitive or annotated with one of
     * the non-nullability annotations.
     * For Kotlin source code, this means element's type is specified as non-null.
     */
    fun isNonNull() = nullability == XNullability.NONNULL

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
