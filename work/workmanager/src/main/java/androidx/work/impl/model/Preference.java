/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity
public class Preference {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "key")
    public String mKey;

    @Nullable
    @ColumnInfo(name = "long_value")
    public Long mValue;

    public Preference(@NonNull String key, boolean value) {
        this(key, value ? 1L : 0L);
    }

    public Preference(@NonNull String key, long value) {
        mKey = key;
        mValue = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Preference)) return false;

        Preference that = (Preference) o;

        if (!mKey.equals(that.mKey)) return false;
        return mValue != null ? mValue.equals(that.mValue) : that.mValue == null;
    }

    @Override
    public int hashCode() {
        int result = mKey.hashCode();
        result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
        return result;
    }
}
