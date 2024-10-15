/*
 * Copyright 2020 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.vo.User;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.testutils.AssertionsKt;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Consolidate with AutoClosingDatabaseTest that has access to internal APIs.
public class AutoClosingRoomOpenHelperTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();
    private UserDao mUserDao;
    private TestDatabase mDb;
    private final DatabaseCallbackTest.TestDatabaseCallback mCallback =
            new DatabaseCallbackTest.TestDatabaseCallback();

    @Before
    public void createDb() throws TimeoutException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("testDb");
        mDb = Room.databaseBuilder(context, TestDatabase.class, "testDb")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .addCallback(mCallback).build();
        mUserDao = mDb.getUserDao();
    }

    @After
    public void cleanUp() throws Exception {
        drain();
        mDb.close();
    }

    @Test
    @MediumTest
    public void inactiveConnection_shouldAutoClose() throws Exception {
        assertFalse(mCallback.mOpened);
        User user = TestUtil.createUser(1);
        user.setName("bob");
        mUserDao.insert(user);
        assertTrue(mCallback.mOpened);

        Thread.sleep(100);
        // Connection should be auto closed here

        User readUser = mUserDao.load(1);
        assertEquals(readUser.getName(), user.getName());
    }

    @Test
    @MediumTest
    public void slowTransaction_keepsDbAlive() throws Exception {
        assertFalse(mCallback.mOpened);

        User user = TestUtil.createUser(1);
        user.setName("bob");
        mUserDao.insert(user);
        assertTrue(mCallback.mOpened);
        Thread.sleep(30);
        mUserDao.load(1);
        // Connection should be auto closed here
        mDb.runInTransaction(
                () -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) { }
                    // Connection would've been auto closed here
                }
        );

        Thread.sleep(100);
        // Connection should be auto closed here
    }

    @Test
    @MediumTest
    public void slowCursorClosing_keepsDbAlive() throws Exception {
        User user = TestUtil.createUser(1);
        user.setName("bob");
        mUserDao.insert(user);
        mUserDao.load(1);

        Cursor cursor = mDb.query("select * from user", null);

        Thread.sleep(100);

        cursor.close();

        Thread.sleep(100);
        // Connection should be auto closed here
    }

    @Test
    @MediumTest
    public void autoClosedConnection_canReopen() throws Exception {
        User user1 = TestUtil.createUser(1);
        user1.setName("bob");
        mUserDao.insert(user1);

        Thread.sleep(100);
        // Connection should be auto closed here

        User user2 = TestUtil.createUser(2);
        user2.setName("bob2");
        mUserDao.insert(user2);
        Thread.sleep(100);
        // Connection should be auto closed here
    }

    @Test
    @MediumTest
    public void liveDataTriggers_shouldApplyOnReopen() throws Exception {
        LiveData<Boolean> adminLiveData = mUserDao.isAdminLiveData(1);

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<Boolean> observer = new AutoClosingRoomOpenHelperTest
                .MyTestObserver<>();
        TestUtil.observeOnMainThread(adminLiveData, lifecycleOwner, observer);
        assertNull(observer.get());

        User user = TestUtil.createUser(1);
        user.setAdmin(true);
        mUserDao.insert(user);

        assertNotNull(observer.get());
        assertTrue(observer.get());

        user.setAdmin(false);
        mUserDao.insertOrReplace(user);
        assertNotNull(observer.get());
        assertFalse(observer.get());

        Thread.sleep(100);
        // Connection should be auto closed here

        user.setAdmin(true);
        mUserDao.insertOrReplace(user);
        assertNotNull(observer.get());
        assertTrue(observer.get());
    }

    @Test
    @MediumTest
    public void testCanExecSqlInCallback() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("testDb2");
        TestDatabase db = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                        .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                        .addCallback(new ExecSqlInCallback())
                        .build();

        db.getUserDao().insert(TestUtil.createUser(1));

        db.close();
    }

    @Test
    public void testManuallyRoomDatabaseClose() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // Create a new db since the other one is cleared in the @After
        TestDatabase testDatabase = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .addCallback(new ExecSqlInCallback())
                .build();

        testDatabase.close();

        // We shouldn't be able to do anything with the database now...
        AssertionsKt.assertThrows(IllegalStateException.class, () -> {
            testDatabase.getUserDao().count();
        }).hasMessageThat().contains("closed");

        assertFalse(testDatabase.isOpen());

        assertFalse(testDatabase.isOpen());
        TestDatabase testDatabase2 = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .addCallback(new ExecSqlInCallback())
                .build();
        testDatabase2.getUserDao().count(); // db should open now
        testDatabase2.close();
        assertFalse(testDatabase.isOpen());
    }

    @Test
    public void testManuallyOpenHelperClose() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // Create a new db since the other one is cleared in the @After
        TestDatabase testDatabase = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .addCallback(new ExecSqlInCallback())
                .build();

        testDatabase.getOpenHelper().close();

        // We shouldn't be able to do anything with the database now...
        AssertionsKt.assertThrows(IllegalStateException.class, () -> {
            testDatabase.getUserDao().count();
        }).hasMessageThat().contains("closed");

        assertFalse(testDatabase.isOpen());
        TestDatabase testDatabase2 = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .addCallback(new ExecSqlInCallback())
                .build();
        testDatabase2.getUserDao().count(); // db should open now
        testDatabase2.getOpenHelper().close();
        assertFalse(testDatabase.isOpen());
    }

    // TODO(336671494): broken test
    @Ignore
    @Test
    @MediumTest
    public void invalidationObserver_isCalledOnEachInvalidation()
            throws TimeoutException, InterruptedException {
        AtomicInteger invalidationCount = new AtomicInteger(0);

        UserTableObserver userTableObserver =
                new UserTableObserver(invalidationCount::getAndIncrement);

        mDb.getInvalidationTracker().addObserver(userTableObserver);

        mUserDao.insert(TestUtil.createUser(1));

        drain();
        assertEquals(1, invalidationCount.get());

        User user1 = TestUtil.createUser(1);
        user1.setAge(123);
        mUserDao.insertOrReplace(user1);

        drain();
        assertEquals(2, invalidationCount.get());

        Thread.sleep(15);
        // Connection should be closed now

        mUserDao.insert(TestUtil.createUser(2));

        drain();
        assertEquals(3, invalidationCount.get());
    }

    // TODO(372946311): Broken test
    @Ignore
    @Test
    @MediumTest
    public void invalidationObserver_canRequeryDb() throws TimeoutException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();

        context.deleteDatabase("testDb2");
        TestDatabase db = Room.databaseBuilder(context, TestDatabase.class, "testDb2")
                // create contention for callback
                .setAutoCloseTimeout(0, TimeUnit.MILLISECONDS)
                .addCallback(mCallback).build();

        AtomicInteger userCount = new AtomicInteger(0);

        UserTableObserver userTableObserver = new UserTableObserver(
                () -> userCount.set(db.getUserDao().count()));

        db.getInvalidationTracker().addObserver(userTableObserver);

        db.getUserDao().insert(TestUtil.createUser(1));
        db.getUserDao().insert(TestUtil.createUser(2));
        db.getUserDao().insert(TestUtil.createUser(3));
        db.getUserDao().insert(TestUtil.createUser(4));
        db.getUserDao().insert(TestUtil.createUser(5));
        db.getUserDao().insert(TestUtil.createUser(6));
        db.getUserDao().insert(TestUtil.createUser(7));

        drain();
        assertEquals(7, userCount.get());
        db.close();
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    private class MyTestObserver<T> extends TestObserver<T> {
        @Override
        protected void drain() throws TimeoutException, InterruptedException {
            AutoClosingRoomOpenHelperTest.this.drain();
        }
    }

    private static class ExecSqlInCallback extends RoomDatabase.Callback {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            db.query("select * from user").close();
        }
    }

    private static class UserTableObserver extends InvalidationTracker.Observer {

        private final Runnable mInvalidationCallback;

        UserTableObserver(Runnable invalidationCallback) {
            super("user");
            mInvalidationCallback = invalidationCallback;
        }

        @Override
        public void onInvalidated(@NonNull @NotNull Set<String> tables) {
            mInvalidationCallback.run();
        }
    }
}
