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
 * Represents a type information for a method or constructor.
 *
 * It is not an XType as it does not represent a class or primitive.
 */
interface XExecutableType {
    /** Parameter types of the method or constructor. */
    val parameterTypes: List<XType>

    /**
     * The list of `Throwable`s that are declared in this executable's signature.
     */
    val thrownTypes: List<XType>

    /**
     * Returns `true` if this represents the same type as [other].
     * TODO: decide on how we want to handle nullability here.
     */
    fun isSameType(other: XExecutableType): Boolean
}
