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

import android.arch.persistence.room.Relation;

import java.util.ArrayList;
import java.util.List;

public class PetsToys {
    public int petId;
    @Relation(parentColumn = "petId", entityColumn = "mPetId")
    public List<Toy> toys;

    public PetsToys() {
        toys = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PetsToys petsToys = (PetsToys) o;

        if (petId != petsToys.petId) {
            return false;
        }
        return toys != null ? toys.equals(petsToys.toys) : petsToys.toys == null;
    }

    @Override
    public int hashCode() {
        int result = petId;
        result = 31 * result + (toys != null ? toys.hashCode() : 0);
        return result;
    }
}
