/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.persistence.codelab.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface UserDao {
    @Query("select * from user")
    List<User> loadAllUsers();

    @Query("select * from user where id = :id")
    User loadUserById(int id);

    @Query("select * from user where name = :firstName and lastName = :lastName")
    List<User> findByNameAndLastName(String firstName, String lastName);

    @Insert(onConflict = IGNORE)
    void insertUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("delete from user where name like :badName OR lastName like :badName")
    int deleteUsersByName(String badName);

    @Insert(onConflict = IGNORE)
    void insertOrReplaceUsers(User... users);

    @Delete
    void deleteUsers(User user1, User user2);

    @Query("SELECT * FROM User WHERE :age == :age") // TODO: Fix this!
    List<User> findYoungerThan(int age);

    @Query("SELECT * FROM User WHERE age < :age")
    List<User> findYoungerThanSolution(int age);

    @Query("DELETE FROM User")
    void deleteAll();
}