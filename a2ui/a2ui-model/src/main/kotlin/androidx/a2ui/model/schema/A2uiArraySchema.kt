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

package androidx.a2ui.model.schema

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Schema representing an array where all items conform to a specific schema.
 *
 * @property items schema that every element in this array must conform to, or null if any schema is
 *   allowed.
 * @property description semantic description of the schema
 */
public class A2uiArraySchema(
    public val items: A2uiSchema? = null,
    public override val description: String? = null,
) : A2uiSchema() {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put(KEY_TYPE, TYPE_ARRAY)
        if (items != null) {
            put(KEY_ITEMS, items.toJsonElement())
        }
        if (description != null) {
            put(KEY_DESCRIPTION, description)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        other as A2uiArraySchema
        return items == other.items
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (items?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Array(items=$items, description=$description)"
    }

    public companion object {
        @JvmField public val INSTANCE: A2uiArraySchema = A2uiArraySchema()

        internal const val KEY_ITEMS = "items"
        internal const val TYPE_ARRAY = "array"
    }
}
