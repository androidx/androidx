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
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.integration.testapp.vo.EmbeddedUserAndAllPets;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.room.integration.testapp.vo.UserAndPet;
import androidx.room.integration.testapp.vo.UserAndPetAdoptionDates;
import androidx.room.integration.testapp.vo.UserAndGenericPet;
import androidx.room.integration.testapp.vo.UserAndPetNonNull;
import androidx.room.integration.testapp.vo.UserIdAndPetNames;
import androidx.room.integration.testapp.vo.UserWithPetsAndToys;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface UserPetDao {
    @Query("SELECT * FROM User u, Pet p WHERE u.mId = p.mUserId")
    List<UserAndPet> loadAll();

    @Query("SELECT * FROM User u, Pet p WHERE u.mId = p.mUserId")
    List<UserAndGenericPet> loadAllGeneric();

    @Query("SELECT * FROM User u LEFT OUTER JOIN Pet p ON u.mId = p.mUserId")
    List<UserAndPet> loadUsers();

    @Query("SELECT * FROM User u LEFT OUTER JOIN Pet p ON u.mId = p.mUserId")
    List<UserAndPetNonNull> loadUsersWithNonNullPet();

    @Query("SELECT * FROM Pet p LEFT OUTER JOIN User u ON u.mId = p.mUserId")
    List<UserAndPet> loadPets();

    @Query("SELECT * FROM User u LEFT OUTER JOIN Pet p ON u.mId = p.mUserId")
    DataSource.Factory<Integer, UserAndAllPets> dataSourceFactoryMultiTable();

    @Query("SELECT * FROM User u")
    List<UserAndAllPets> loadAllUsersWithTheirPets();

    @Query("SELECT * FROM User u")
    List<UserIdAndPetNames> loadUserAndPetNames();

    @Query("SELECT * FROM User u")
    List<UserWithPetsAndToys> loadUserWithPetsAndToys();

    @Query("SELECT * FROM User UNION ALL SELECT * FROM USER")
    List<UserAndAllPets> unionByItself();

    @Query("SELECT * FROM User")
    List<UserAndPetAdoptionDates> loadUserWithPetAdoptionDates();

    @Query("SELECT * FROM User u where u.mId = :userId")
    LiveData<UserAndAllPets> liveUserWithPets(int userId);

    @Query("SELECT * FROM User u where u.mId = :userId")
    Flowable<UserAndAllPets> flowableUserWithPets(int userId);

    @Query("SELECT * FROM User u where u.mId = :uid")
    EmbeddedUserAndAllPets loadUserAndPetsAsEmbedded(int uid);

    @Insert
    void insertUserAndPet(User user, Pet pet);

    @Update
    void updateUsersAndPets(User[] users, Pet[] pets);

    @Delete
    void delete2UsersAndPets(User user1, User user2, Pet[] pets);
}
