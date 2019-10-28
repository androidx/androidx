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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetAndOwner;
import androidx.room.integration.testapp.vo.PetWithToyIds;
import androidx.room.integration.testapp.vo.PetWithUser;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Pet... pets);

    @Insert
    void insertAll(Pet[] pets);

    @Query("SELECT COUNT(*) FROM Pet")
    int count();

    @Transaction
    @Query("SELECT * FROM Pet ORDER BY Pet.mPetId ASC")
    List<PetWithToyIds> allPetsWithToyIds();

    @Transaction
    @Query("SELECT * FROM Pet")
    List<PetAndOwner> allPetsWithOwners();

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    ListenableFuture<Optional<Pet>> petWithIdFuture(int id);

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    Flowable<Pet> petWithIdFlowable(int id);

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    LiveData<Pet> petWithIdLiveData(int id);

    @Query("SELECT * FROM PetWithUser WHERE mPetId = :id")
    LiveData<PetWithUser> petWithUserLiveData(int id);

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
