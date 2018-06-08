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

package androidx.room.integration.testapp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetWithToyIds;

import java.util.List;

@Dao
public interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Pet... pets);

    @Insert
    void insertAll(Pet[] pets);

    @Query("SELECT COUNT(*) FROM Pet")
    int count();

    @Query("SELECT * FROM Pet ORDER BY Pet.mPetId ASC")
    List<PetWithToyIds> allPetsWithToyIds();

    @Delete
    void delete(Pet pet);

    @Query("SELECT mPetId FROM Pet")
    int[] allIds();

    @Transaction
    default void deleteAndInsert(Pet oldPet, Pet newPet, boolean shouldFail) {
        delete(oldPet);
        if (shouldFail) {
            throw new RuntimeException();
        }
        insertOrReplace(newPet);
    }
}
