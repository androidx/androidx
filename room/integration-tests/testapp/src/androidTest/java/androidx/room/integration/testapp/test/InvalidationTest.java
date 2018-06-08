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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.vo.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests invalidation tracking.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class InvalidationTest {
    @Rule
    public CountingTaskExecutorRule executorRule = new CountingTaskExecutorRule();
    private UserDao mUserDao;
    private TestDatabase mDb;

    @Before
    public void createDb() throws TimeoutException, InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        mDb = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = mDb.getUserDao();
        drain();
    }

    @After
    public void closeDb() throws TimeoutException, InterruptedException {
        mDb.close();
        drain();
    }

    private void drain() throws TimeoutException, InterruptedException {
        executorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    @Test
    public void testInvalidationOnUpdate() throws InterruptedException, TimeoutException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        LoggingObserver observer = new LoggingObserver("User");
        mDb.getInvalidationTracker().addObserver(observer);
        drain();
        mUserDao.updateById(3, "foo2");
        drain();
        assertThat(observer.getInvalidatedTables(), hasSize(1));
        assertThat(observer.getInvalidatedTables(), hasItem("User"));
    }

    @Test
    public void testInvalidationOnDelete() throws InterruptedException, TimeoutException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        LoggingObserver observer = new LoggingObserver("User");
        mDb.getInvalidationTracker().addObserver(observer);
        drain();
        mUserDao.delete(user);
        drain();
        assertThat(observer.getInvalidatedTables(), hasSize(1));
        assertThat(observer.getInvalidatedTables(), hasItem("User"));
    }

    @Test
    public void testInvalidationOnInsert() throws InterruptedException, TimeoutException {
        LoggingObserver observer = new LoggingObserver("User");
        mDb.getInvalidationTracker().addObserver(observer);
        drain();
        mUserDao.insert(TestUtil.createUser(3));
        drain();
        assertThat(observer.getInvalidatedTables(), hasSize(1));
        assertThat(observer.getInvalidatedTables(), hasItem("User"));
    }

    @Test
    public void testDontInvalidateOnLateInsert() throws InterruptedException, TimeoutException {
        LoggingObserver observer = new LoggingObserver("User");
        mUserDao.insert(TestUtil.createUser(3));
        drain();
        mDb.getInvalidationTracker().addObserver(observer);
        drain();
        assertThat(observer.getInvalidatedTables(), nullValue());
    }

    @Test
    public void testMultipleTables() throws InterruptedException, TimeoutException {
        LoggingObserver observer = new LoggingObserver("User", "Pet");
        mDb.getInvalidationTracker().addObserver(observer);
        drain();
        mUserDao.insert(TestUtil.createUser(3));
        drain();
        assertThat(observer.getInvalidatedTables(), hasSize(1));
        assertThat(observer.getInvalidatedTables(), hasItem("User"));
    }

    private static class LoggingObserver extends InvalidationTracker.Observer {
        private Set<String> mInvalidatedTables;

        LoggingObserver(String... tables) {
            super(tables);
        }

        @Override
        public void onInvalidated(@NonNull Set<String> tables) {
            mInvalidatedTables = tables;
        }

        Set<String> getInvalidatedTables() {
            return mInvalidatedTables;
        }
    }
}
