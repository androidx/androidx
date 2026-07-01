/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.model.catalog

import androidx.a2ui.model.schema.A2uiSchema

/** The JSON-compatible return types a function can advertise to the AI agent. */
public enum class A2uiFunctionReturnType(public val value: String) {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    OBJECT("object"),
    ANY("any"),
    VOID("void"),
}

/** Represents the definition and schema of a function supported by A2UI. */
public interface A2uiFunctionDefinition {
    /** Function name that would be provided to the AI agent. */
    public val name: String

    /** Describes the function behavior so the AI agent knows when to use it. */
    public val description: String

    /** The schema of the arguments expected by the component. */
    public val argumentSchema: A2uiSchema

    /** The type of the value returned by the function. */
    public val returnType: A2uiFunctionReturnType
}
