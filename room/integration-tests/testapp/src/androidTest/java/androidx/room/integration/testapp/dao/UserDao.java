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

package androidx.room.integration.testapp.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.vo.AvgWeightByAge;
import androidx.room.integration.testapp.vo.Day;
import androidx.room.integration.testapp.vo.IdUsername;
import androidx.room.integration.testapp.vo.NameAndLastName;
import androidx.room.integration.testapp.vo.NameAndUsers;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndFriends;
import androidx.room.integration.testapp.vo.UserSummary;
import androidx.room.integration.testapp.vo.Username;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.util.concurrent.ListenableFuture;

import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Observable;
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

    @Query("select * from user where mId IN(:ids)")
    public abstract List<User> loadByIdCollection(Collection<Integer> ids);

    @Query("select * from user where mId IN(:ids)")
    public abstract List<User> loadByIdSet(Set<Integer> ids);

    @Query("select * from user where mId IN(:ids)")
    public abstract List<User> loadByIdQueue(Queue<Integer> ids);

    @Query("select * from user where custommm = :customField")
    public abstract List<User> findByCustomField(String customField);

    @Transaction
    @Query("select * from user")
    public abstract List<UserAndFriends> loadUserAndFriends();

    @Insert
    public abstract void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertOrReplace(User user);

    @Delete
    public abstract int delete(User user);

    @Delete(entity = User.class)
    public abstract int deleteViaUsername(Username username);

    @Delete
    public abstract int deleteAll(User[] users);

    @Delete
    public abstract void deleteUser(User[] users);

    @Query("delete from user")
    public abstract int deleteEverything();

    @Update
    public abstract int update(User user);

    @Update(entity = User.class)
    public abstract int updateUsername(IdUsername username);

    @Update
    public abstract Completable updateCompletable(User user);

    @Update
    public abstract Single<Integer> updateSingle(User user);

    @Update
    public abstract Single<Integer> updateSingleUsers(User user1, User user2);

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

    @Transaction
    @Query("select * from user where mId = :id")
    public abstract LiveData<User> liveUserByIdInTransaction(int id);

    @Query("select * from user where mName LIKE '%' || :name || '%' ORDER BY mId DESC")
    public abstract LiveData<List<User>> liveUsersListByName(String name);

    @Query("select * from user where mId IN(:ids)")
    public abstract LiveData<List<User>> liveUsersListByByIds(int... ids);

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
    public abstract io.reactivex.Flowable<User> rx2_flowableUserById(int id);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.rxjava3.core.Flowable<User> rx3_flowableUserById(int id);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.Observable<User> rx2_observableUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.Observable<List<User>> rx2_observableUsersByIds(int... ids);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.rxjava3.core.Observable<User> rx3_observableUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.rxjava3.core.Observable<List<User>> rx3_observableUsersByIds(
            int... ids);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.Maybe<User> rx2_maybeUserById(int id);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.rxjava3.core.Maybe<User> rx3_maybeUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.Maybe<List<User>> rx2_maybeUsersByIds(int... ids);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.rxjava3.core.Maybe<List<User>> rx3_maybeUsersByIds(int... ids);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.Single<User> rx2_singleUserById(int id);

    @Query("select * from user where mId = :id")
    public abstract io.reactivex.rxjava3.core.Single<User> rx3_singleUserById(int id);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.Single<List<User>> rx2_singleUsersByIds(int... ids);

    @Query("select * from user where mId IN (:ids)")
    public abstract io.reactivex.rxjava3.core.Single<List<User>> rx3_singleUsersByIds(int... ids);

    @Query("select COUNT(*) from user")
    public abstract io.reactivex.Flowable<Integer> rx2_flowableCountUsers();

    @Query("select COUNT(*) from user")
    public abstract io.reactivex.rxjava3.core.Flowable<Integer> rx3_flowableCountUsers();

    @Query("select COUNT(*) from user")
    public abstract Publisher<Integer> publisherCountUsers();

    @Query("select COUNT(*) from user")
    public abstract Observable<Integer> observableCountUsers();

    @Query("SELECT mBirthday from User where mId = :id")
    public abstract Date getBirthday(int id);

    @Query("SELECT mBirthday from User")
    public abstract List<Date> getAllBirthdays();

    @Query("SELECT COUNT(*) from user")
    public abstract int count();

    @Query("SELECT mAdmin from User where mId = :uid")
    public abstract boolean isAdmin(int uid);

    @Query("SELECT mAdmin from User where mId = :uid")
    public abstract LiveData<Boolean> isAdminLiveData(int uid);

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
    public abstract DataSource.Factory<Integer, User> loadPagedByAge(int age);

    @RawQuery(observedEntities = User.class)
    public abstract DataSource.Factory<Integer, User> loadPagedByAgeWithObserver(
            SupportSQLiteQuery query);

    // TODO: switch to PositionalDataSource once Room supports it
    @Query("SELECT * FROM user ORDER BY mAge DESC")
    public abstract DataSource.Factory<Integer, User> loadUsersByAgeDesc();

    @Query("DELETE FROM User WHERE mId IN (:ids) AND mAge == :age")
    public abstract int deleteByAgeAndIds(int age, List<Integer> ids);

    @Query("UPDATE User set mWeight = :weight WHERE mId IN (:ids) AND mAge == :age")
    public abstract int updateByAgeAndIds(float weight, int age, List<Integer> ids);

    @Query("SELECT * FROM user WHERE (mWorkDays & :days) != 0")
    public abstract List<User> findUsersByWorkDays(Set<Day> days);

    // QueryLoader

    @Query("SELECT COUNT(*) from user")
    public abstract Integer getUserCount();

    @Query("SELECT u.mName, u.mLastName from user u where mId = :id")
    public abstract NameAndLastName getNameAndLastName(int id);

    @Transaction
    public void insertBothByAnnotation(final User a, final User b) {
        insert(a);
        insert(b);
    }

    // b/117401230
    @Query("SELECT * FROM user WHERE "
            + "mName LIKE '%' || 'happy' || '%' "
            + "OR mName LIKE '%' || 'life' || '%' "
            + "OR mName LIKE '%' || 'while' || '%' "
            + "OR mName LIKE '%' || 'playing' || '%' "
            + "OR mName LIKE '%' || 'video' || '%' "
            + "OR mName LIKE '%' || 'games' || '%' ")
    public abstract List<User> getUserWithCoolNames();

    // The subquery is intentional (b/118398616)
    @Query("SELECT `mId`, `mName` FROM (SELECT * FROM User)")
    public abstract List<UserSummary> getNames();

    @Insert
    public abstract ListenableFuture<List<Long>> insertWithLongListFuture(List<User> users);

    @Insert
    public abstract ListenableFuture<Long[]> insertWithLongArrayFuture(User... users);

    @Insert
    public abstract ListenableFuture<Long> insertWithLongFuture(User user);

    @Insert
    public abstract ListenableFuture<Void> insertWithVoidFuture(User user);

    @Delete
    public abstract ListenableFuture<Integer> deleteWithIntFuture(User user);

    @Delete
    public abstract ListenableFuture<Void> deleteWithVoidFuture(User user);

    @Update
    public abstract ListenableFuture<Integer> updateWithIntFuture(User user);

    @Update
    public abstract ListenableFuture<Void> updateWithVoidFuture(User user);

    @Query("UPDATE user SET mName = :name, mLastName = :name WHERE mId = :userId")
    public abstract void setSameNames(String name, int userId);

    @Query("SELECT us.mName, us.mLastName  FROM User as us WHERE mName = :name UNION "
            + "SELECT us.mName, us.mLastName  FROM User as us WHERE mName = :name")
    public abstract List<NameAndLastName> selectByName_withTablePrefixAndUnion(String name);

    @Transaction
    @Query("SELECT mName FROM User")
    public abstract List<NameAndUsers> getNameAndUsers();
}
