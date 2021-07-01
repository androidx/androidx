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

import com.squareup.javapoet.ClassName

interface XTypeElement : XHasModifiers, XElement, XMemberContainer {
    /**
     * The qualified name of the Class/Interface.
     */
    val qualifiedName: String

    /**
     * The qualified name of the package that contains this element.
     */
    val packageName: String

    /**
     * SimpleName of the type converted to String.
     *
     * @see [javax.lang.model.element.Element.getSimpleName]
     */
    val name: String

    /**
     * The type represented by this [XTypeElement].
     */
    override val type: XType

    /**
     * The super type of this element if it represents a class.
     */
    val superType: XType?

    /**
     * Javapoet [ClassName] of the type.
     */
    override val className: ClassName

    /**
     * The [XTypeElement] that contains this [XTypeElement] if it is an inner class/interface.
     */
    val enclosingTypeElement: XTypeElement?

    override val fallbackLocationText: String
        get() = qualifiedName

    /**
     * Returns `true` if this [XTypeElement] represents an interface
     */
    fun isInterface(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents a Kotlin functional interface,
     * i.e. marked with the keyword `fun`
     */
    fun isFunctionalInterface(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents an ordinary class. ie not an enum, object,
     * annotation, interface, or other type of specialty class.
     */
    fun isClass(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents a Kotlin data class
     */
    fun isDataClass(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents a Kotlin value class
     */
    fun isValueClass(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents a class with the Kotlin `expect` keyword
     */
    fun isExpect(): Boolean

    /**
     * Returns `true` if this [XTypeElement] represents a Kotlin annotation class or a Java
     * annotation type.
     */
    fun isAnnotationClass(): Boolean

    /**
     * Returns `true` if this [XTypeElement] is a non-companion `object` in Kotlin
     */
    fun isKotlinObject(): Boolean

    /**
     * Returns `true` if this [XTypeElement] is declared as a Kotlin `companion object`
     */
    fun isCompanionObject(): Boolean

    /**
     * Fields declared in this type
     *  includes all instance/static fields in this
     */
    fun getDeclaredFields(): List<XFieldElement>

    /**
     * All fields, including private supers.
     * Room only ever reads fields this way.
     */
    fun getAllFieldsIncludingPrivateSupers(): List<XFieldElement>

    /**
     * Returns the primary constructor for the type, if it exists.
     *
     * Note that this only exists for classes declared in Kotlin.
     */
    fun findPrimaryConstructor(): XConstructorElement?

    /**
     * methods declared in this type
     *  includes all instance/static methods in this
     */
    fun getDeclaredMethods(): List<XMethodElement>

    /**
     * Methods declared in this type and its parents
     *  includes all instance/static methods in this
     *  includes all instance/static methods in parent CLASS if they are accessible from this (e.g.
     *  not private).
     *  does not include static methods in parent interfaces
     */
    fun getAllMethods(): List<XMethodElement> {
        return collectAllMethods(this).toList()
    }

    /**
     * Instance methods declared in this and supers
     *  include non private instance methods
     *  also includes non-private instance methods from supers
     */
    fun getAllNonPrivateInstanceMethods(): List<XMethodElement> {
        return getAllMethods().filter {
            !it.isPrivate() && !it.isStatic()
        }
    }

    /**
     * Returns the list of constructors in this type element
     */
    fun getConstructors(): List<XConstructorElement>

    /**
     * List of interfaces implemented by this class
     */
    fun getSuperInterfaceElements(): List<XTypeElement>

    /**
     * Returns the list of all classes nested directly within this class, including companion
     * objects.
     */
    fun getEnclosedTypeElements(): List<XTypeElement>
}
