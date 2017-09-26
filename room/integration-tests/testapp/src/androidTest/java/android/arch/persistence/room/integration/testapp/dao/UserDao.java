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
import android.arch.paging.LivePagedListProvider;
import android.arch.paging.TiledDataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;
import android.arch.persistence.room.integration.testapp.TestDatabase;
import android.arch.persistence.room.integration.testapp.vo.AvgWeightByAge;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.database.Cursor;

import org.reactivestreams.Publisher;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@SuppressWarnings("SameParameterValue")
@Dao
public abstract class UserDao {

    private final TestDatabase mDatabase;

    public UserDao(TestDatabase database) {
        mDatabase = database;
    }

    @Query("select * from user where mName like :name")
    public abstract List<User> findUsersByName(String name);

    @Query("select * from user where mId = :id")
    public abstract User load(int id);

    @Query("select * from user where mId IN(:ids)")
    public abstract User[] loadByIds(int... ids);

    @Query("select * from user where custommm = :customField")
    public abstract List<User> findByCustomField(String customField);

    @Insert
    public abstract void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertOrReplace(User user);

    @Delete
    public abstract int delete(User user);

    @Delete
    public abstract int deleteAll(User[] users);

    @Query("delete from user")
    public abstract int deleteEverything();

    @Update
    public abstract int update(User user);

    @Update
    public abstract int updateAll(List<User> users);

    @Insert
    public abstract void insertAll(User[] users);

    @Query("select * from user where mAdmin = :isAdmin")
    public abstract List<User> findByAdmin(boolean isAdmin);

    @Query("delete from user where mAge > :age")
    public abstract int deleteAgeGreaterThan(int age);

    @Query("delete from user where mId IN(:uids)")
    public abstract int deleteByUids(int... uids);

    @Query("delete from user where mAge >= :min AND mAge <= :max")
    public abstract int deleteByAgeRange(int min, int max);

    @Query("update user set mName = :name where mId = :id")
    public abstract int updateById(int id, String name);

    @Query("update user set mId = mId + :amount")
    public abstract void incrementIds(int amount);

    @Query("update user set mAge = mAge + 1")
    public abstract void incrementAgeOfAll();

    @Query("select mId from user order by mId ASC")
    public abstract List<Integer> loadIds();

    @Query("select * from user where mId = :id")
    public abstract LiveData<User> liveUserById(int id);

    @Query("select * from user where mName LIKE '%' || :name || '%' ORDER BY mId DESC")
    public abstract LiveData<List<User>> liveUsersListByName(String name);

    @Query("select * from user where length(mName) = :length")
    public abstract List<User> findByNameLength(int length);

    @Query("select * from user where mAge = :age")
    public abstract List<User> findByAge(int age);

    @Query("select mAge, AVG(mWeight) from user GROUP BY mAge ORDER BY 2 DESC")
    public abstract List<AvgWeightByAge> weightByAge();

    @Query("select mAge, AVG(mWeight) from user GROUP BY mAge ORDER BY 2 DESC LIMIT 1")
    public abstract LiveData<AvgWeightByAge> maxWeightByAgeGroup();

    @Query("select * from user where mBirthday > :from AND mBirthday < :to")
    public abstract List<User> findByBirthdayRange(Date from, Date to);

    @Query("select mId from user where mId IN (:ids)")
    public abstract Cursor findUsersAsCursor(int... ids);

    @Query("select * from user where mId = :id")
    public abstract Flowable<User> flowableUserById(int id);

    @Query("select * from user where mId = :id")
    public abstract Maybe<User> maybeUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract Maybe<List<User>> maybeUsersByIds(int... ids);

    @Query("select * from user where mId = :id")
    public abstract Single<User> singleUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract Single<List<User>> singleUsersByIds(int... ids);

    @Query("select COUNT(*) from user")
    public abstract Flowable<Integer> flowableCountUsers();

    @Query("select COUNT(*) from user")
    public abstract Publisher<Integer> publisherCountUsers();

    @Query("SELECT mBirthday from User where mId = :id")
    public abstract Date getBirthday(int id);

    @Query("SELECT COUNT(*) from user")
    public abstract int count();

    public void insertBothByRunnable(final User a, final User b) {
        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                insert(a);
                insert(b);
            }
        });
    }

    public int insertBothByCallable(final User a, final User b) {
        return mDatabase.runInTransaction(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                insert(a);
                insert(b);
                return 2;
            }
        });
    }

    @Query("SELECT * FROM user where mAge > :age")
    public abstract LivePagedListProvider<Integer, User> loadPagedByAge(int age);

    @Query("SELECT * FROM user ORDER BY mAge DESC")
    public abstract TiledDataSource<User> loadUsersByAgeDesc();

    @Query("DELETE FROM User WHERE mId IN (:ids) AND mAge == :age")
    public abstract int deleteByAgeAndIds(int age, List<Integer> ids);

    @Query("UPDATE User set mWeight = :weight WHERE mId IN (:ids) AND mAge == :age")
    public abstract int updateByAgeAndIds(float weight, int age, List<Integer> ids);

    // QueryLoader

    @Query("SELECT COUNT(*) from user")
    public abstract Integer getUserCount();


    // QueryDataSourceTest - name desc

    //   limit-offset
    @Query("SELECT * from user ORDER BY mName DESC LIMIT :limit OFFSET :offset")
    public abstract List<User> userNameLimitOffset(int limit, int offset);

    //   keyed
    @Query("SELECT * from user ORDER BY mName DESC LIMIT :limit")
    public abstract List<User> userNameInitial(int limit);

    @Query("SELECT * from user WHERE mName < :key ORDER BY mName DESC LIMIT :limit")
    public abstract List<User> userNameLoadAfter(String key, int limit);

    @Query("SELECT COUNT(*) from user WHERE mName < :key ORDER BY mName DESC")
    public abstract int userNameCountAfter(String key);

    @Query("SELECT * from user WHERE mName > :key ORDER BY mName ASC LIMIT :limit")
    public abstract List<User> userNameLoadBefore(String key, int limit);

    @Query("SELECT COUNT(*) from user WHERE mName > :key ORDER BY mName ASC")
    public abstract int userNameCountBefore(String key);



    // ComplexQueryDataSourceTest - last desc, first asc, id desc

    //   limit-offset
    @Query("SELECT * from user"
            + " ORDER BY mLastName DESC, mName ASC, mId DESC"
            + " LIMIT :limit OFFSET :offset")
    public abstract List<User> userComplexLimitOffset(int limit, int offset);

    //   keyed
    @Query("SELECT * from user"
            + " ORDER BY mLastName DESC, mName ASC, mId DESC"
            + " LIMIT :limit")
    public abstract List<User> userComplexInitial(int limit);

    @Query("SELECT * from user"
            + " WHERE mLastName < :lastName or (mLastName = :lastName and (mName > :name or (mName = :name and mId < :id)))"
            + " ORDER BY mLastName DESC, mName ASC, mId DESC"
            + " LIMIT :limit")
    public abstract List<User> userComplexLoadAfter(String lastName, String name, int id, int limit);

    @Query("SELECT COUNT(*) from user"
            + " WHERE mLastName < :lastName or (mLastName = :lastName and (mName > :name or (mName = :name and mId < :id)))"
            + " ORDER BY mLastName DESC, mName ASC, mId DESC")
    public abstract int userComplexCountAfter(String lastName, String name, int id);

    @Query("SELECT * from user"
            + " WHERE mLastName > :lastName or (mLastName = :lastName and (mName < :name or (mName = :name and mId > :id)))"
            + " ORDER BY mLastName ASC, mName DESC, mId ASC"
            + " LIMIT :limit")
    public abstract List<User> userComplexLoadBefore(String lastName, String name, int id, int limit);

    @Query("SELECT COUNT(*) from user"
            + " WHERE mLastName > :lastName or (mLastName = :lastName and (mName < :name or (mName = :name and mId > :id)))"
            + " ORDER BY mLastName ASC, mName DESC, mId ASC")
    public abstract int userComplexCountBefore(String lastName, String name, int id);

    @Transaction
    public void insertBothByAnnotation(final User a, final User b) {
        insert(a);
        insert(b);
    }
}
