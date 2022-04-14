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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo

import com.google.gson.annotations.SerializedName

/**
 * Data class that holds the schema information for an
 * [androidx.room.Entity] field.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FieldBundle(
    @SerializedName("fieldPath")
    public open val fieldPath: String,
    @SerializedName("columnName")
    public open val columnName: String,
    @SerializedName("affinity")
    public open val affinity: String,
    @SerializedName("notNull")
    public open val isNonNull: Boolean,
    @SerializedName("defaultValue")
    public open val defaultValue: String?,
) : SchemaEquality<FieldBundle> {

    /**
     * @deprecated Use [FieldBundle(String, String, String, boolean, String)]
     */
    @Deprecated("Use [FieldBundle(String, String, String, boolean, String)")
    public constructor(fieldPath: String, columnName: String, affinity: String, nonNull: Boolean) :
        this(fieldPath, columnName, affinity, nonNull, null)

    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    private constructor() : this("", "", "", false, null)

    override fun isSchemaEqual(other: FieldBundle): Boolean {
        if (isNonNull != other.isNonNull) return false
        if (columnName != other.columnName) {
            return false
        }
        if (defaultValue?.let { it != other.defaultValue } ?: (other.defaultValue != null)) {
            return false
        }
        return affinity == other.affinity
    }
}
