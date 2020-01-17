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

import androidx.serialization.compiler.schema.Enum
import androidx.serialization.schema.Reserved
import javax.lang.model.element.ElementKind.ENUM_CONSTANT

/** Generates a customizable [Enum] for testing code generation. */
internal fun testEnum(
    packageName: String = "com.example",
    vararg simpleNames: String = Array(1) { "TestEnum" },
    values: Map<Int, String> = mapOf(
        0 to "DEFAULT",
        1 to "ONE",
        2 to "TWO"
    ),
    reserved: Reserved = Reserved.empty()
): Enum {
    val typeElement = testTypeElement(packageName, *simpleNames)
    return Enum(
        typeElement,
        values.mapTo(mutableSetOf()) { (id, name) ->
            Enum.Value(testVariableElement(name, ENUM_CONSTANT, typeElement), id)
        },
        reserved
    )
}
