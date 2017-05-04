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

package android.arch.persistence.room.integration.testapp.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.arch.persistence.room.integration.testapp.vo.AvgWeightByAge;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.database.Cursor;

import org.reactivestreams.Publisher;

import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;

@SuppressWarnings("SameParameterValue")
@Dao
public interface UserDao {
    @Query("select * from user where mName like :name")
    List<User> findUsersByName(String name);

    @Query("select * from user where mId = :id")
    User load(int id);

    @Query("select * from user where mId IN(:ids)")
    User[] loadByIds(int... ids);

    @Insert
    void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(User user);

    @Delete
    int delete(User user);

    @Delete
    int deleteAll(User[] users);

    @Update
    int update(User user);

    @Update
    int updateAll(List<User> users);

    @Insert
    void insertAll(User[] users);

    @Query("select * from user where mAdmin = :isAdmin")
    List<User> findByAdmin(boolean isAdmin);

    @Query("delete from user where mAge > :age")
    int deleteAgeGreaterThan(int age);

    @Query("delete from user where mId IN(:uids)")
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

    @Query("select * from user where length(mName) = :length")
    List<User> findByNameLenght(int length);

    @Query("select * from user where mAge = :age")
    List<User> findByAge(int age);

    @Query("select mAge, AVG(mWeight) from user GROUP BY mAge ORDER BY 2 DESC")
    List<AvgWeightByAge> weightByAge();

    @Query("select mAge, AVG(mWeight) from user GROUP BY mAge ORDER BY 2 DESC LIMIT 1")
    LiveData<AvgWeightByAge> maxWeightByAgeGroup();

    @Query("select * from user where mBirthday > :from AND mBirthday < :to")
    List<User> findByBirthdayRange(Date from, Date to);

    @Query("select mId from user where mId IN (:ids)")
    Cursor findUsersAsCursor(int... ids);

    @Query("select * from user where mId = :id")
    Flowable<User> flowableUserById(int id);

    @Query("select COUNT(*) from user")
    Flowable<Integer> flowableCountUsers();

    @Query("select COUNT(*) from user")
    Publisher<Integer> publisherCountUsers();
}
