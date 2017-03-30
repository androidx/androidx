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

package com.example.android.persistence.codelab.orm_db;

import com.android.support.room.Dao;
import com.android.support.room.Delete;
import com.android.support.room.Insert;
import com.android.support.room.Query;

import java.util.List;

import static com.android.support.room.OnConflictStrategy.REPLACE;

@Dao
public interface UserDao {
    @Query("select * from user")
    List<User> loadAllUsers();

    @Query("select * from user where id = ?")
    User loadUserById(int id);

    @Query("select * from user where name = :firstName and lastName = :lastName")
    List<User> findByNameAndLastName(String firstName, String lastName);

    @Insert
    void insertUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("delete from user where name like :badName OR lastName like :badName")
    int deleteUsersByName(String badName);

    @Insert(onConflict = REPLACE)
    void insertOrReplaceUsers(User... users);

    @Delete
    void deleteBothUsers(User user1, User user2);

    @Query("SELECT * FROM User WHERE :age == :age") // Fix this!
    List<User> findYoungerThan(int age);

    @Query("SELECT * FROM User WHERE age < :age") // Fix this!
    List<User> findYoungerThanSolution(int age);
}