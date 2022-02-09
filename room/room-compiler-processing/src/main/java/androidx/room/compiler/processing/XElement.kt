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
import androidx.room.compiler.processing.ksp.wrapAsOriginatingElement
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

    /**
     * Returns the immediate enclosing element. This uses Element.getEnclosingElement() on the
     * Java side, and KSNode.parent on the KSP side. For non-nested classes we return null as we
     * don't model packages yet. For fields declared in primary constructors in Kotlin we return
     * the enclosing type, not the constructor. For top-level properties or functions in Kotlin
     * we return JavacTypeElement on the Java side and KspFileMemberContainer or
     * KspSyntheticFileMemberContainer on the KSP side.
     */
    val enclosingElement: XElement?

    /**
     * Returns the closest member container. Could be the element if it's itself a member container.
     */
    val closestMemberContainer: XMemberContainer
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
 * Checks whether this element represents an [XEnumTypeElement].
 */
fun XElement.isEnum(): Boolean {
    contract {
        returns(true) implies (this@isEnum is XEnumTypeElement)
    }
    return this is XEnumTypeElement
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
 * Checks whether this element represents an [XFieldElement].
 */
fun XElement.isField(): Boolean {
    contract {
        returns(true) implies (this@isField is XFieldElement)
    }
    return this is XFieldElement
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

/**
 * Checks whether this element represents an [XExecutableParameterElement].
 */
fun XElement.isMethodParameter(): Boolean {
    contract {
        returns(true) implies (this@isMethodParameter is XExecutableParameterElement)
    }
    return this is XExecutableParameterElement
}

/**
 * Checks whether this element represents an [XConstructorElement].
 */
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
            declaration.wrapAsOriginatingElement()
        }
        is KspSyntheticPropertyMethodElement -> {
            field.declaration.wrapAsOriginatingElement()
        }
        is KspMemberContainer -> {
            declaration?.wrapAsOriginatingElement()
        }
        else -> error("Originating element is not implemented for ${this.javaClass}")
    }
}
