/*
 * Copyright 2017 The Android Open Source Project
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

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;

public class PetWithToyIds {
    @Embedded
    public final Pet pet;
    @Relation(parentColumn = "mPetId", entityColumn = "mPetId", projection = "mId",
            entity = Toy.class)
    public List<Integer> toyIds;

    // for the relation
    public PetWithToyIds(Pet pet) {
        this.pet = pet;
    }

    @Ignore
    public PetWithToyIds(Pet pet, List<Integer> toyIds) {
        this.pet = pet;
        this.toyIds = toyIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PetWithToyIds that = (PetWithToyIds) o;

        if (pet != null ? !pet.equals(that.pet) : that.pet != null) return false;
        return toyIds != null ? toyIds.equals(that.toyIds) : that.toyIds == null;
    }

    @Override
    public int hashCode() {
        int result = pet != null ? pet.hashCode() : 0;
        result = 31 * result + (toyIds != null ? toyIds.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PetWithToyIds{"
                + "pet=" + pet
                + ", toyIds=" + toyIds
                + '}';
    }
}
