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

/**
 * Represents an element declared in code.
 *
 * @see [javax.lang.model.element.Element]
 * @see XExecutableElement
 * @see XVariableElement
 * @see XTypeElement
 */
interface XElement : XAnnotated {
    /**
     * Returns the string representation of the Element's kind.
     */
    fun kindName(): String

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
