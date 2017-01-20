/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.integration.testapp.dao;

import com.android.support.lifecycle.LiveData;
import com.android.support.room.Dao;
import com.android.support.room.Delete;
import com.android.support.room.Insert;
import com.android.support.room.Query;
import com.android.support.room.integration.testapp.vo.User;

import java.util.List;

@Dao
public interface UserDao {
    @Query("select * from user where mName like :name")
    List<User> findUsersByName(String name);

    @Query("select * from user where mId = ?")
    User load(int id);

    @Query("select * from user where mId IN(?)")
    User[] loadByIds(int... ids);

    @Insert
    void insert(User user);

    @Insert(onConflict = Insert.REPLACE)
    void insertOrReplace(User user);

    @Delete
    int delete(User user);

    @Delete
    int deleteAll(User[] users);

    @Insert
    void insertAll(User[] users);

    @Query("select * from user where mAdmin = ?")
    List<User> findByAdmin(boolean isAdmin);

    @Query("delete from user where mAge > ?")
    int deleteAgeGreaterThan(int age);

    @Query("delete from user where mId IN(?)")
    int deleteByUids(int... uids);

    @Query("delete from user where mAge >= :min AND mAge <= :max")
    int deleteByAgeRange(int min, int max);

    @Query("update user set mName = :name where mId = :id")
    int updateById(int id, String name);

    @Query("update user set mId = mId + :amount")
    void incrementIds(int amount);

    @Query("select mId from user order by mId ASC")
    List<Integer> loadIds();

    @Query("select * from user where mId = :id")
    LiveData<User> liveUserById(int id);

    @Query("select * from user where mName LIKE '%' || :name || '%' ORDER BY mId DESC")
    LiveData<List<User>> liveUsersListByName(String name);

    @Query("select * from user where length(mName) = ?")
    List<User> findByNameLenght(int length);

    @Query("select * from user where mAge = ?")
    List<User> findByAge(int age);
}
