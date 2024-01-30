/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator.poet

import androidx.privacysandbox.tools.core.model.Type

internal enum class AidlTypeKind {
    PRIMITIVE,
    PARCELABLE,
    INTERFACE,
}

internal data class AidlTypeSpec(
    val innerType: Type,
    val kind: AidlTypeKind,
    val isList: Boolean = false,
) {
    override fun toString() = buildString {
        append(innerType.simpleName)
        if (isList) append("[]")
    }

    /** Returns a new type spec representing a list of this type. */
    fun listSpec(): AidlTypeSpec {
        require(!isList) { "Nested lists are not supported." }
        return copy(isList = true)
    }

    val requiresImport = kind != AidlTypeKind.PRIMITIVE
    val isParcelable = kind == AidlTypeKind.PARCELABLE
}
