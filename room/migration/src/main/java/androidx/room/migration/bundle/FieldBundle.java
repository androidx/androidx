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

package androidx.room.migration.bundle;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

/**
 * Data class that holds the schema information for an
 * {@link androidx.room.Entity Entity} field.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FieldBundle implements SchemaEquality<FieldBundle> {
    @SerializedName("fieldPath")
    private String mFieldPath;
    @SerializedName("columnName")
    private String mColumnName;
    @SerializedName("affinity")
    private String mAffinity;
    @SerializedName("notNull")
    private boolean mNonNull;

    public FieldBundle(String fieldPath, String columnName, String affinity, boolean nonNull) {
        mFieldPath = fieldPath;
        mColumnName = columnName;
        mAffinity = affinity;
        mNonNull = nonNull;
    }

    public String getFieldPath() {
        return mFieldPath;
    }

    public String getColumnName() {
        return mColumnName;
    }

    public String getAffinity() {
        return mAffinity;
    }

    public boolean isNonNull() {
        return mNonNull;
    }

    @Override
    public boolean isSchemaEqual(FieldBundle other) {
        if (mNonNull != other.mNonNull) return false;
        if (mColumnName != null ? !mColumnName.equals(other.mColumnName)
                : other.mColumnName != null) {
            return false;
        }
        return mAffinity != null ? mAffinity.equals(other.mAffinity) : other.mAffinity == null;
    }
}
