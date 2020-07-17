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

interface XElement {
    val name: String

    val packageName: String

    /**
     * TODO:
     *  Nullability is normally a property of Type not Element but currently Room relies on
     *  Annotations to resolve nullability which exists only on Elements, not Types.
     *  Once we implement KSP version, we might be able to move this to the type by making sure
     *  we carry over nullability when type is resolved from an Element. We also need nullability
     *  on Types to properly handle DAO return types (e.g. Flow<T> vs Flow<T?>)
     */
    val nullability: XNullability

    val enclosingElement: XElement?

    fun isPublic(): Boolean

    fun isProtected(): Boolean

    fun isAbstract(): Boolean

    fun isPrivate(): Boolean

    fun isStatic(): Boolean

    fun isTransient(): Boolean

    fun isFinal(): Boolean

    fun kindName(): String

    fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>?

    // a very sad method but helps avoid abstraction annotation
    fun hasAnnotationInPackage(pkg: String): Boolean

    fun hasAnnotation(annotation: KClass<out Annotation>): Boolean

    fun hasAnyOf(vararg annotations: KClass<out Annotation>) = annotations.any(this::hasAnnotation)

    fun isNonNull() = nullability == XNullability.NONNULL

    fun asTypeElement() = this as XTypeElement

    fun asVariableElement() = this as XVariableElement

    fun asExecutableElement() = this as XExecutableElement

    fun asDeclaredType(): XDeclaredType {
        return asTypeElement().type
    }
}

// we keep these as extension methods to be able to use contracts
fun XElement.isType(): Boolean {
    contract {
        returns(true) implies (this@isType is XTypeElement)
    }
    return this is XTypeElement
}

fun XElement.isField(): Boolean {
    contract {
        returns(true) implies (this@isField is XVariableElement)
    }
    return this is XVariableElement
}

fun XElement.isMethod(): Boolean {
    contract {
        returns(true) implies (this@isMethod is XExecutableElement)
    }
    return this is XExecutableElement
}
