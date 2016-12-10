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

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.room.DatabaseConfiguration;
import com.android.support.room.integration.testapp.TestDatabase;
import com.android.support.room.integration.testapp.TestDatabase_Impl;
import com.android.support.room.integration.testapp.vo.User;

import junit.framework.AssertionFailedError;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SimpleEntityReadWriteTest {
    TestDatabase mDb;
    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        DatabaseConfiguration configuration = new DatabaseConfiguration.Builder(context).build();
        mDb = new TestDatabase_Impl(configuration);
    }
    @Test
    public void writeUserAndReadInList() throws Exception {
        User user = new User();
        user.setId(3);
        user.setAge(99);
        user.setName("george");
        user.setLastName("kloony");
        mDb.getUserDao().insert(user);
        List<User> byName = mDb.getUserDao().findUsersByName("george");
        Assert.assertEquals(user, byName.get(0));
    }

    @Test
    public void throwExceptionOnConflict() {
        User user = new User();
        user.setId(3);
        user.setAge(99);
        user.setName("george");
        user.setLastName("kloony");
        mDb.getUserDao().insert(user);

        User user2 = new User();
        user2.setId(3);
        user2.setAge(22);
        user2.setName("michael");
        user2.setLastName("jordo");
        try {
            mDb.getUserDao().insert(user2);
            throw new AssertionFailedError("didn't throw in conflicting insertion");
        } catch (SQLiteException ignored) {
        }
    }

    @Test
    public void replaceOnConflict() {
        User user = new User();
        user.setId(3);
        user.setAge(99);
        user.setName("george");
        user.setLastName("kloony");
        mDb.getUserDao().insert(user);

        User user2 = new User();
        user2.setId(3);
        user2.setAge(22);
        user2.setName("michael");
        user2.setLastName("jordo");
        mDb.getUserDao().insertOrReplace(user2);
        Assert.assertEquals(user2, mDb.getUserDao().load(3));
    }
}
