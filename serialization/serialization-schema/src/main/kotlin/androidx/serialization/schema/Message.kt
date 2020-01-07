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

package androidx.serialization.schema

/** A message type, either well-known or client-declared as a Java or Kotlin class or interface. */
interface Message : ComplexType {
    /** The fields of the message. */
    val fields: Collection<Field>

    /** Alias for [fields]. */
    override val members: Collection<Field>
        get() = fields

    /** A field of a message, usually derived from [androidx.serialization.Field] annotations. */
    interface Field : ComplexType.Member {
        /** The type of the field. May be any type including scalars and well-known types. */
        val type: Type
    }
}
