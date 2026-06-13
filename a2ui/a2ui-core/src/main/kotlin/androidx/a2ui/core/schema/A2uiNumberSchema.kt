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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Schema representing a number literal.
 *
 * @property description semantic description of the schema
 */
public class A2uiNumberSchema(public override val description: String? = null) : A2uiSchema() {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put(KEY_TYPE, TYPE_NUMBER)
        if (description != null) {
            put(KEY_DESCRIPTION, description)
        }
    }

    override fun toString(): String {
        return "Number(description=$description)"
    }

    internal companion object {
        internal const val TYPE_NUMBER = "number"
    }
}
