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

import com.squareup.javapoet.ClassName

interface XTypeElement : XElement {
    /**
     * The qualified name of the Class/Interface.
     */
    val qualifiedName: String

    /**
     * The type represented by this [XTypeElement].
     */
    val type: XDeclaredType

    /**
     * The super type of this element if it represents a class.
     */
    val superType: XType?

    /**
     * Javapoet [ClassName] of the type.
     */
    val className: ClassName

    /**
     * Returns `true` if this [XTypeElement] represents an interface
     */
    fun isInterface(): Boolean

    /**
     * Returns `true` if this [XTypeElement] is declared as a Kotlin `object`
     */
    fun isKotlinObject(): Boolean

    /**
     * All fields, including private supers.
     * Room only ever reads fields this way.
     */
    fun getAllFieldsIncludingPrivateSupers(): List<XVariableElement>

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
    fun getAllMethods(): List<XMethodElement>

    /**
     * Instance methods declared in this and supers
     *  include non private instance methods
     *  also includes non-private instance methods from supers
     */
    fun getAllNonPrivateInstanceMethods(): List<XMethodElement>

    /**
     * Returns the list of constructors in this type element
     */
    fun getConstructors(): List<XConstructorElement>
}
