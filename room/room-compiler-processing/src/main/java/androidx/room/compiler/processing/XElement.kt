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

import androidx.room.compiler.processing.javac.JavacElement
import androidx.room.compiler.processing.ksp.KSFileAsOriginatingElement
import androidx.room.compiler.processing.ksp.KspElement
import androidx.room.compiler.processing.ksp.KspMemberContainer
import androidx.room.compiler.processing.ksp.containingFileAsOriginatingElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import javax.lang.model.element.Element
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
     * When the location of an element is unknown, this String is appended to the diagnostic
     * message. Without this information, developer gets no clue on where the error is.
     */
    val fallbackLocationText: String

    /**
     * The documentation comment of the element, or null if there is none.
     */
    val docComment: String?

    /**
     * Returns true if all types referenced by this element are valid, i.e. resolvable.
     */
    fun validate(): Boolean
}

/**
 * Checks whether this element represents an [XTypeElement].
 */
// we keep these as extension methods to be able to use contracts
fun XElement.isTypeElement(): Boolean {
    contract {
        returns(true) implies (this@isTypeElement is XTypeElement)
    }
    return this is XTypeElement
}

/**
 * Checks whether this element represents an [XVariableElement].
 */
fun XElement.isVariableElement(): Boolean {
    contract {
        returns(true) implies (this@isVariableElement is XVariableElement)
    }
    return this is XVariableElement
}

/**
 * Checks whether this element represents an [XMethodElement].
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

/**
 * Attempts to get a Javac [Element] representing the originating element for attribution
 * when writing a file for incremental processing.
 *
 * In KSP a [KSFileAsOriginatingElement] will be returned, which is a synthetic javac element
 * that allows us to pass originating elements to JavaPoet and KotlinPoet, and later extract
 * the KSP file when writing with [XFiler].
 */
internal fun XElement.originatingElementForPoet(): Element? {
    return when (this) {
        is JavacElement -> element
        is KspElement -> {
            declaration.containingFileAsOriginatingElement()
        }
        is KspSyntheticPropertyMethodElement -> {
            field.declaration.containingFileAsOriginatingElement()
        }
        is KspMemberContainer -> {
            declaration?.containingFileAsOriginatingElement()
        }
        else -> error("Originating element is not implemented for ${this.javaClass}")
    }
}
