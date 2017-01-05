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

package com.android.support.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.room.Room;
import com.android.support.room.integration.testapp.TestDatabase;
import com.android.support.room.integration.testapp.dao.UserDao;
import com.android.support.room.integration.testapp.vo.User;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SimpleEntityReadWriteTest {
    private UserDao mUserDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = db.getUserDao();
    }

    @Test
    public void writeUserAndReadInList() throws Exception {
        User user = TestUtil.createUser(3);
        user.setName("george");
        mUserDao.insert(user);
        List<User> byName = mUserDao.findUsersByName("george");
        assertThat(byName.get(0), equalTo(user));
    }

    @Test
    public void throwExceptionOnConflict() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);

        User user2 = TestUtil.createUser(3);
        try {
            mUserDao.insert(user2);
            throw new AssertionFailedError("didn't throw in conflicting insertion");
        } catch (SQLiteException ignored) {
        }
    }

    @Test
    public void replaceOnConflict() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);

        User user2 = TestUtil.createUser(3);
        mUserDao.insertOrReplace(user2);

        assertThat(mUserDao.load(3), equalTo(user2));
        assertThat(mUserDao.load(3), not(equalTo(user)));
    }

    @Test
    public void delete() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        assertThat(mUserDao.delete(user), is(1));
        assertThat(mUserDao.delete(user), is(0));
        assertThat(mUserDao.load(3), is(nullValue()));
    }

    @Test
    public void deleteAll() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        mUserDao.insertAll(users);
        // there is actually no guarantee for this order by works fine since they are ordered for
        // the test and it is a new database (no pages to recycle etc)
        assertThat(mUserDao.loadByIds(3, 5, 7, 9), is(users));
        int deleteCount = mUserDao.deleteAll(new User[]{users[0], users[3],
                TestUtil.createUser(9)});
        assertThat(deleteCount, is(2));
        assertThat(mUserDao.loadByIds(3, 5, 7, 9), is(new User[]{users[1], users[2]}));
    }

    @Test
    public void findByBoolean() {
        User user1 = TestUtil.createUser(3);
        user1.setAdmin(true);
        User user2 = TestUtil.createUser(5);
        user2.setAdmin(false);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.findByAdmin(true), is(Arrays.asList(user1)));
        assertThat(mUserDao.findByAdmin(false), is(Arrays.asList(user2)));
    }

    @Test
    public void deleteByAge() {
        User user1 = TestUtil.createUser(3);
        user1.setAge(30);
        User user2 = TestUtil.createUser(5);
        user2.setAge(45);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.deleteAgeGreaterThan(60), is(0));
        assertThat(mUserDao.deleteAgeGreaterThan(45), is(0));
        assertThat(mUserDao.deleteAgeGreaterThan(35), is(1));
        assertThat(mUserDao.loadByIds(3, 5), is(new User[]{user1}));
    }

    @Test
    public void deleteByAgeRange() {
        User user1 = TestUtil.createUser(3);
        user1.setAge(30);
        User user2 = TestUtil.createUser(5);
        user2.setAge(45);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.deleteByAgeRange(35, 40), is(0));
        assertThat(mUserDao.deleteByAgeRange(25, 30), is(1));
        assertThat(mUserDao.loadByIds(3, 5), is(new User[]{user2}));
    }

    @Test
    public void deleteByUIds() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9, 11);
        mUserDao.insertAll(users);
        assertThat(mUserDao.deleteByUids(2, 4, 6), is(0));
        assertThat(mUserDao.deleteByUids(3, 11), is(2));
        assertThat(mUserDao.loadByIds(3, 5, 7, 9, 11), is(new User[]{
                users[1], users[2], users[3]
        }));
    }

    @Test
    public void updateNameById() {
        User[] usersArray = TestUtil.createUsersArray(3, 5, 7);
        mUserDao.insertAll(usersArray);
        assertThat("test sanity", usersArray[1].getName(), not(equalTo("updated name")));
        int changed = mUserDao.updateById(5, "updated name");
        assertThat(changed, is(1));
        assertThat(mUserDao.load(5).getName(), is("updated name"));
    }

    @Test
    public void incrementIds() {
        User[] usersArr = TestUtil.createUsersArray(2, 4, 6);
        mUserDao.insertAll(usersArr);
        mUserDao.incrementIds(1);
        assertThat(mUserDao.loadIds(), is(Arrays.asList(3, 5, 7)));
    }
}
