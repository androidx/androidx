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

package androidx.a2ui.core.schema

import androidx.a2ui.core.schema.internal.toJsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Schema representing a value that must exactly match one of the provided enum values.
 *
 * @property enumValues list of exact values allowed by this schema.
 * @property description semantic description of the schema
 */
public class A2uiEnumSchema(
    public val enumValues: List<Any>,
    public override val description: String? = null,
) : A2uiSchema() {
    override fun toJsonElement(): JsonElement {
        return buildJsonObject {
            // Note: type is intentionally left out to support any
            put(KEY_ENUM, JsonArray(enumValues.map { it.toJsonElement() }))
            if (description != null) {
                put(KEY_DESCRIPTION, description)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        other as A2uiEnumSchema
        return enumValues == other.enumValues
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + enumValues.hashCode()
        return result
    }

    override fun toString(): String {
        return "Enum(enumValues=$enumValues, description=$description)"
    }

    internal companion object {
        internal const val KEY_ENUM = "enum"
    }
}
