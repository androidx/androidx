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
    val qualifiedName: String

    val type: XDeclaredType

    val superType: XType?

    val className: ClassName

    fun isInterface(): Boolean

    fun isKotlinObject(): Boolean

    /**
     * All fields, including private supers.
     * Room only ever reads fields this way.
     */
    fun getAllFieldsIncludingPrivateSupers(): List<XVariableElement>

    // only in kotlin
    fun findPrimaryConstructor(): XExecutableElement?

    /**
     * methods declared in this type
     *  includes all instance/static methods in this
     */
    fun getDeclaredMethods(): List<XExecutableElement>

    /**
     * Methods declared in this type and its parents
     *  includes all instance/static methods in this
     *  includes all instance/static methods in parent CLASS if they are accessible from this (e.g. not
     *  private).
     *  does not include static methods in parent interfaces
     */
    fun getAllMethods(): List<XExecutableElement>

    /**
     * Instance methods declared in this and supers
     *  include non private instance methods
     *  also includes non-private instance methods from supers
     */
    fun getAllNonPrivateInstanceMethods(): List<XExecutableElement>

    fun getConstructors(): List<XExecutableElement>
}
