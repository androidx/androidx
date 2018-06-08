/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room.vo

import androidx.room.migration.bundle.PrimaryKeyBundle
import javax.lang.model.element.Element

/**
 * Represents a PrimaryKey for an Entity.
 */
data class PrimaryKey(val declaredIn: Element?, val fields: List<Field>,
                      val autoGenerateId: Boolean) : HasSchemaIdentity {
    companion object {
        val MISSING = PrimaryKey(null, emptyList(), false)
    }

    val columnNames by lazy { fields.map { it.columnName } }

    fun toHumanReadableString(): String {
        return "PrimaryKey[" +
                fields.joinToString(separator = ", ", transform = Field::getPath) + "]"
    }

    fun toBundle(): PrimaryKeyBundle = PrimaryKeyBundle(
            autoGenerateId, fields.map { it.columnName })

    override fun getIdKey(): String {
        return "$autoGenerateId-${fields.map { it.columnName }}"
    }
}
