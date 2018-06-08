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

import java.util.Date;

@Entity
public class Pet {
    @PrimaryKey
    private int mPetId;
    private int mUserId;
    @ColumnInfo(name = "mPetName")
    private String mName;

    private Date mAdoptionDate;

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

    public Date getAdoptionDate() {
        return mAdoptionDate;
    }

    public void setAdoptionDate(Date adoptionDate) {
        mAdoptionDate = adoptionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pet pet = (Pet) o;

        if (mPetId != pet.mPetId) return false;
        if (mUserId != pet.mUserId) return false;
        if (mName != null ? !mName.equals(pet.mName) : pet.mName != null) return false;
        return mAdoptionDate != null ? mAdoptionDate.equals(pet.mAdoptionDate)
                : pet.mAdoptionDate == null;
    }

    @Override
    public int hashCode() {
        int result = mPetId;
        result = 31 * result + mUserId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mAdoptionDate != null ? mAdoptionDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Pet{"
                + "mPetId=" + mPetId
                + ", mUserId=" + mUserId
                + ", mName='" + mName + '\''
                + ", mAdoptionDate=" + mAdoptionDate
                + '}';
    }
}
