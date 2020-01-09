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

import androidx.serialization.schema.ComplexType
import androidx.serialization.schema.Enum as SchemaEnum
import androidx.serialization.schema.Message as SchemaMessage
import androidx.serialization.schema.Reserved
import androidx.serialization.schema.Service as SchemaService
import androidx.serialization.schema.Service.Action.Mode.BLOCKING
import androidx.serialization.schema.Type
import androidx.serialization.schema.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/** Base type for schema compiled from the processing environment. */
internal sealed class ProcessingType : ComplexType {
    /** The type element this Serialization type is derived from. */
    abstract val element: TypeElement
}

internal data class Enum(
    override val element: TypeElement,
    override val values: Set<Value> = emptySet(),
    override val reserved: Reserved = Reserved.empty(),
    override val name: TypeName = element.toTypeName()
) : ProcessingType(), SchemaEnum {
    data class Value(
        /** The element representing the annotated enum constant. */
        val element: VariableElement,
        override val id: Int,
        override val name: String = element.simpleName.toString()
    ) : SchemaEnum.Value
}

internal data class Message(
    override val element: TypeElement,
    override val fields: Set<Field> = emptySet(),
    override val reserved: Reserved = Reserved.empty(),
    override val name: TypeName = element.toTypeName()
) : ProcessingType(), SchemaMessage {
    data class Field(
        override val id: Int,
        override val name: String,
        override val type: Type
    ) : SchemaMessage.Field
}

internal data class Service(
    override val element: TypeElement,
    override val actions: Set<Action> = emptySet(),
    override val reserved: Reserved = Reserved.empty(),
    override val name: TypeName = element.toTypeName(),
    override val descriptor: String = name.canonicalName
) : ProcessingType(), SchemaService {
    data class Action(
        /** The element representing the annotated action method. */
        val element: ExecutableElement,
        override val id: Int,
        override val name: String = element.simpleName.toString(),
        override val mode: SchemaService.Action.Mode = BLOCKING,
        override val request: Message? = null,
        override val response: Message? = null
    ) : SchemaService.Action
}
