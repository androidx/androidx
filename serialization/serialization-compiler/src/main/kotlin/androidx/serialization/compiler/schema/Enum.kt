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

package androidx.serialization.compiler.schema

import androidx.serialization.schema.Reserved
import androidx.serialization.schema.TypeName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

internal class Enum(
    override val name: TypeName,
    override val values: List<Value>,
    override val reserved: Reserved,
    val element: TypeElement
) : androidx.serialization.schema.Enum {
    class Value(
        override val id: Int,
        override val name: String,
        val element: VariableElement,
        val annotation: AnnotationMirror
    ) : androidx.serialization.schema.Enum.Value
}
