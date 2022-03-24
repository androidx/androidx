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

package foo.bar;
import androidx.room.*;
import java.util.List;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
abstract interface UpdateDao {
    @Update
    void updateUser(User user);
    @Update
    void updateUsers(User user1, List<User> others);
    @Update
    void updateArrayOfUsers(User[] users);
    @Update(onConflict=OnConflictStrategy.NONE)
    void updateTwoUsers(User userOne, User userTwo);

    @Update
    int updateUserAndReturnCount(User user);
    @Update
    int updateUserAndReturnCount(User user1, List<User> others);
    @Update
    int updateUserAndReturnCount(User[] users);

    @Update
    Integer updateUserAndReturnCountObject(User user);

    @Update
    Completable updateUserAndReturnCountCompletable(User user);
    @Update
    Single<Integer> updateUserAndReturnCountSingle(User user);
    @Update
    Maybe<Integer> updateUserAndReturnCountMaybe(User user);

    @Update
    int multiPKey(MultiPKeyEntity entity);

    @Update
    void updateUserAndBook(User user, Book book);

    @Query("UPDATE User SET ageColumn = ageColumn + 1 WHERE uid = :uid")
    void ageUserByUid(String uid);

    @Query("UPDATE User SET ageColumn = ageColumn + 1")
    void ageUserAll();

    @Query("UPDATE User SET ageColumn = ageColumn + 1")
    Completable ageUserAllCompletable();

    @Query("UPDATE User SET ageColumn = ageColumn + 1")
    Single<Integer> ageUserAllSingle();

    @Query("UPDATE User SET ageColumn = ageColumn + 1")
    Maybe<Integer> ageUserAllMaybe();

    @Transaction
    default void updateAndAge(User user) {
        updateUser(user);
        ageUserByUid(String.valueOf(user.uid));
    }
}
