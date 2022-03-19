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

package androidx.room.androidx.room.integration.kotlintestapp.test

import androidx.room.androidx.room.integration.kotlintestapp.vo.Email
import androidx.room.androidx.room.integration.kotlintestapp.vo.User
import androidx.room.integration.kotlintestapp.test.TestDatabaseTest
import androidx.test.filters.MediumTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

@MediumTest
class UsersDaoTest : TestDatabaseTest() {

    @Test
    fun insertAndGetUsers() {
        val USER_1 = User("u1", Email("e1", "email address 1"), Email("e2", "email address 2"))
        val USER_2 = User("u2", Email(null, null), Email("e3", "email address 3"))
        val USER_3 = User("u3", Email("e4", "email address 4"), Email(null, null))
        val USER_4 = User("u4", Email(null, null), Email(null, null))

        usersDao.insertUser(USER_1)
        usersDao.insertUser(USER_2)
        usersDao.insertUser(USER_3)
        usersDao.insertUser(USER_4)

        val expectedList = ArrayList<User>()
        expectedList.add(USER_1)
        expectedList.add(USER_2)
        expectedList.add(User(USER_3.userId, USER_3.email, null))
        expectedList.add(User(USER_4.userId, USER_4.email, null))

        MatcherAssert.assertThat(
            database.usersDao().getUsers(), CoreMatchers.`is`<List<User>>(expectedList)
        )
    }
}