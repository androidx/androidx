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

package androidx.room.integration.testapp.vo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * An entity that was weird names
 */
@Entity(tableName = FunnyNamedEntity.TABLE_NAME)
public class FunnyNamedEntity {
    public static final String TABLE_NAME = "funny but not so funny";
    public static final String COLUMN_ID = "_this $is id$";
    public static final String COLUMN_VALUE = "unlikely-Ωşå¨ıünames";
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    private int mId;
    @ColumnInfo(name = COLUMN_VALUE)
    private String mValue;

    public FunnyNamedEntity(int id, String value) {
        mId = id;
        mValue = value;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunnyNamedEntity entity = (FunnyNamedEntity) o;

        if (mId != entity.mId) return false;
        return mValue != null ? mValue.equals(entity.mValue) : entity.mValue == null;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
        return result;
    }
}
