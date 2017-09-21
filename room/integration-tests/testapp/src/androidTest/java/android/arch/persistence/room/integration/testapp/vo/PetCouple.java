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

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.RoomWarnings;
import android.support.annotation.NonNull;

@Entity
@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
public class PetCouple {
    @PrimaryKey
    @NonNull
    public String id;
    @Embedded(prefix = "male_")
    public Pet male;
    @Embedded(prefix = "female_")
    private Pet mFemale;

    public Pet getFemale() {
        return mFemale;
    }

    public void setFemale(Pet female) {
        mFemale = female;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PetCouple petCouple = (PetCouple) o;

        if (male != null ? !male.equals(petCouple.male) : petCouple.male != null) return false;
        return mFemale != null ? mFemale.equals(petCouple.mFemale) : petCouple.mFemale == null;
    }

    @Override
    public int hashCode() {
        int result = male != null ? male.hashCode() : 0;
        result = 31 * result + (mFemale != null ? mFemale.hashCode() : 0);
        return result;
    }
}
