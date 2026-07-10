/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.integration.kotlintestapp.vo.User
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.reactivestreams.Publisher

@Dao
interface UsersDao {
    @Insert fun insertUser(vararg user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertOrReplaceUser(vararg user: User)

    @Query("delete from user where uId IN(:uids)") fun deleteByUids(vararg uids: Int): Int

    @Query("SELECT * FROM user") fun getUsers(): List<User>

    @Query("select * from user where uId = :id") fun maybeUserId(id: Int): Maybe<User>

    @Query("select * from user where uId IN (:ids)")
    fun maybeUsersByIds(vararg ids: Int): Maybe<List<User>>

    @Query("select * from user where uId = :id") fun singleUserId(id: Int): Single<User>

    @Query("select * from user where uId IN (:ids)")
    fun singleUsersByIds(vararg ids: Int): Single<List<User>>

    @Query("select * from user where uId = :id") fun flowableUserById(id: Int): Flowable<User>

    @Query("select * from user where uId = :id") fun observableUserById(id: Int): Observable<User>

    @Query("select COUNT(*) from user") fun flowableCountUsers(): Flowable<Int>

    @Query("select COUNT(*) from user") fun publisherCountUsers(): Publisher<Int>
}
