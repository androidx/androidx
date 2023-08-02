/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * Represents a constructor of a class.
 *
 * @see XMethodElement
 * @see XExecutableElement
 */
interface XConstructorElement : XExecutableElement {
    override val enclosingElement: XTypeElement

    override val fallbackLocationText: String
        get() = buildString {
            append(enclosingElement.qualifiedName)
            append(".<init>")
            append("(")
            append(
                parameters.joinToString(", ") {
                    it.type.asTypeName().java.toString()
                }
            )
            append(")")
        }

    /** The type representation of the method where more type parameters might be resolved. */
    override val executableType: XConstructorType

    /**
     * Returns the constructor as if it is declared in [other].
     *
     * This is specifically useful if you have a constructor that has type arguments and there is a
     * subclass ([other]) where type arguments are specified to actual types.
     */
    override fun asMemberOf(other: XType): XConstructorType

    /**
     * Denotes if this is a synthetic constructor generated via the usage of @JvmOverloads
     */
    fun isSyntheticConstructorForJvmOverloads(): Boolean
}
