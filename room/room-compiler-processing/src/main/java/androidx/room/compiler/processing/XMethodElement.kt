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
 * Represents a method in a class / interface.
 *
 * @see XConstructorElement
 * @see XMethodElement
 */
interface XMethodElement : XExecutableElement {
    /**
     * The name of the method.
     */
    val name: String

    /**
     * The return type for the method. Note that it might be [XType.isNone] if it does not return or
     * [XType.isError] if the return type cannot be resolved.
     */
    val returnType: XType

    /**
     * The type representation of the method where more type parameters might be resolved.
     */
    val executableType: XMethodType

    override val fallbackLocationText: String
        get() = buildString {
            append(enclosingElement.fallbackLocationText)
            append(".")
            append(name)
            append("(")
            // don't report last parameter if it is a suspend function
            append(
                parameters.dropLast(
                    if (isSuspendFunction()) 1 else 0
                ).joinToString(", ") {
                    it.type.typeName.toString()
                }
            )
            append(")")
        }

    /**
     * Returns true if this method has the default modifier.
     *
     * @see [hasKotlinDefaultImpl]
     */
    fun isJavaDefault(): Boolean

    /**
     * Returns the method as if it is declared in [other].
     *
     * This is specifically useful if you have a method that has type arguments and there is a
     * subclass ([other]) where type arguments are specified to actual types.
     */
    fun asMemberOf(other: XType): XMethodType

    /**
     * Returns true if this method has a default implementation in Kotlin.
     *
     * To support default methods in interfaces, Kotlin generates a delegate implementation. In
     * Java, we find the DefaultImpls class to delegate the call. In kotlin, we get this information
     * from KSP.
     */
    fun hasKotlinDefaultImpl(): Boolean

    /**
     * Returns true if this is a suspend function.
     *
     * @see XMethodType.getSuspendFunctionReturnType
     */
    fun isSuspendFunction(): Boolean

    /**
     * Returns true if this method can be overridden without checking its enclosing [XElement].
     */
    fun isOverrideableIgnoringContainer(): Boolean {
        return !isFinal() && !isPrivate() && !isStatic()
    }

    /**
     * Returns `true` if this method overrides the [other] method when this method is viewed as
     * member of the [owner].
     */
    fun overrides(other: XMethodElement, owner: XTypeElement): Boolean

    /**
     * Creates a new [XMethodElement] where containing element is replaced with [newContainer].
     */
    fun copyTo(newContainer: XTypeElement): XMethodElement
}