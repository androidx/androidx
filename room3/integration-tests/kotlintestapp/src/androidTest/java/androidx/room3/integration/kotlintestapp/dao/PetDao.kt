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
package androidx.room3.integration.kotlintestapp.dao

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.integration.kotlintestapp.vo.Pet
import androidx.room3.integration.kotlintestapp.vo.PetAndOwner
import androidx.room3.integration.kotlintestapp.vo.PetWithToyIds
import androidx.room3.integration.kotlintestapp.vo.PetWithUser
import com.google.common.base.Optional
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertOrReplace(vararg pets: Pet)

    @Insert fun insertAll(pets: Array<Pet>)

    @Query("SELECT COUNT(*) FROM Pet") fun count(): Int

    @Transaction
    @Query("SELECT * FROM Pet ORDER BY Pet.petId ASC")
    fun allPetsWithToyIds(): List<PetWithToyIds>

    @Transaction @Query("SELECT * FROM Pet") fun allPetsWithOwners(): List<PetAndOwner>

    @Query("SELECT * FROM Pet WHERE Pet.petId = :id")
    fun petWithIdFuture(id: Int): ListenableFuture<Optional<Pet>>

    @Query("SELECT * FROM Pet WHERE Pet.petId = :id") fun petWithIdFlowable(id: Int): Flowable<Pet>

    @Query("SELECT * FROM Pet WHERE Pet.petId = :id") fun petWithId(id: Int): Pet

    @Query("SELECT * FROM Pet WHERE Pet.petId = :id") fun petWithIdLiveData(id: Int): LiveData<Pet>

    @Query("SELECT * FROM PetWithUser WHERE petId = :id")
    fun petWithUserLiveData(id: Int): LiveData<PetWithUser>

    @Delete fun delete(pet: Pet)

    @Query("SELECT petId FROM Pet") fun allIds(): IntArray

    @Transaction
    fun deleteAndInsert(oldPet: Pet, newPet: Pet, shouldFail: Boolean) {
        delete(oldPet)
        if (shouldFail) {
            throw RuntimeException()
        }
        insertOrReplace(newPet)
    }
}
