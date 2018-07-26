/*
 * Copyright (C) 2018 The Android Open Source Project
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
import androidx.room.Relation;

import java.util.List;

public class UserAndPetsAndHouses {

    @Embedded
    private final User mUser;

    @Relation(parentColumn = "mId", entityColumn = "mUserId")
    private final List<Pet> mPets;

    @Relation(parentColumn = "mId", entityColumn = "mOwnerId")
    private final List<House> mHouses;

    public UserAndPetsAndHouses(User user,
            List<Pet> pets, List<House> houses) {
        this.mUser = user;
        this.mPets = pets;
        this.mHouses = houses;
    }

    public User getUser() {
        return mUser;
    }

    public List<Pet> getPets() {
        return mPets;
    }

    public List<House> getHouses() {
        return mHouses;
    }
}
