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

/**
 * Represents a variable element, that is either a method parameter or a field.
 */
interface XVariableElement : XElement {
    /**
     * The name of the variable element.
     */
    val name: String

    /**
     * Returns the type of this field or parameter
     */
    val type: XType

    /**
     * Returns this type as a member of the [other] type.
     * It is useful when this [XVariableElement] has a generic type declaration and its type is
     * specified in [other]. (e.g. Bar<T> vs Foo : Bar<String>)
     */
    fun asMemberOf(other: XType): XType
}