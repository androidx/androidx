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

/**
 * Any non-scalar type, such as [Message], [Enum], or [Service].
 *
 * This super-interface allows validation of common elements such as duplicate or reserved member
 * IDs or names.
 */
interface ComplexType : Type {
    /** The name of the type, usually derived from a Java or Kotlin class name. */
    val name: TypeName

    /** Members of the type, such as fields for messages. */
    val members: Collection<Member>

    /** Reserved member names and IDs for this type. */
    val reserved: Reserved

    override val typeKind: Type.Kind
        get() = Type.Kind.DECLARED

    /** A member of a complex type, such a message field or enum value. */
    interface Member {
        /** The integer ID of the member; implementers types may restrict valid ID values. */
        val id: Int

        /** The name of the member, usually derived from its Java or Kotlin syntactic name. */
        val name: String
    }
}
