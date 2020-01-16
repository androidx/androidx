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

package androidx.serialization.compiler

import androidx.serialization.schema.Enum
import androidx.serialization.schema.Reserved
import androidx.serialization.schema.TypeName

/** Customizable [Enum] implementation with default values scaffolding. */
internal data class TestEnum(
    override val name: TypeName = TypeName("com.example", "TestEnum"),
    override val values: Set<Value> = setOf(
        Value(0, "DEFAULT"),
        Value(1, "ONE"),
        Value(2, "TWO")
    ),
    override val reserved: Reserved = Reserved.empty()
) : Enum {
    data class Value(
        override val id: Int,
        override val name: String
    ) : Enum.Value
}
