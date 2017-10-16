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

package android.arch.persistence.room.integration.testapp.vo;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Pet {
    @PrimaryKey
    private int mPetId;
    private int mUserId;
    @ColumnInfo(name = "mPetName")
    private String mName;

    public int getPetId() {
        return mPetId;
    }

    public void setPetId(int petId) {
        mPetId = petId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pet pet = (Pet) o;

        if (mPetId != pet.mPetId) return false;
        if (mUserId != pet.mUserId) return false;
        return mName != null ? mName.equals(pet.mName) : pet.mName == null;
    }

    @Override
    public int hashCode() {
        int result = mPetId;
        result = 31 * result + mUserId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        return result;
    }
}
