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
package androidx.room.integration.kotlintestapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.integration.kotlintestapp.vo.Pet
import androidx.room.integration.kotlintestapp.vo.PetAndOwner
import androidx.room.integration.kotlintestapp.vo.PetWithToyIds
import androidx.room.integration.kotlintestapp.vo.PetWithUser
import com.google.common.base.Optional
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Flowable

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg pets: Pet)

    @Insert
    fun insertAll(pets: Array<Pet>)

    @Query("SELECT COUNT(*) FROM Pet")
    fun count(): Int

    @Transaction
    @Query("SELECT * FROM Pet ORDER BY Pet.mPetId ASC")
    fun allPetsWithToyIds(): List<PetWithToyIds>

    @Transaction
    @Query("SELECT * FROM Pet")
    fun allPetsWithOwners(): List<PetAndOwner>

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    fun petWithIdFuture(id: Int): ListenableFuture<Optional<Pet>>

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    fun petWithIdFlowable(id: Int): Flowable<Pet>

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    fun petWithId(id: Int): Pet

    @Query("SELECT * FROM Pet WHERE Pet.mPetId = :id")
    fun petWithIdLiveData(id: Int): LiveData<Pet>

    @Query("SELECT * FROM PetWithUser WHERE mPetId = :id")
    fun petWithUserLiveData(id: Int): LiveData<PetWithUser>

    @Delete
    fun delete(pet: Pet)

    @Query("SELECT mPetId FROM Pet")
    fun allIds(): IntArray

    @Transaction
    fun deleteAndInsert(oldPet: Pet, newPet: Pet, shouldFail: Boolean) {
        delete(oldPet)
        if (shouldFail) {
            throw RuntimeException()
        }
        insertOrReplace(newPet)
    }
}
