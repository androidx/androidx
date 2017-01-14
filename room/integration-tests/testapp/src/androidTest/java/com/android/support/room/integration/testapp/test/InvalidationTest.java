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

package com.android.support.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.executors.TaskExecutor;
import com.android.support.room.InvalidationTracker;
import com.android.support.room.Room;
import com.android.support.room.integration.testapp.TestDatabase;
import com.android.support.room.integration.testapp.dao.UserDao;
import com.android.support.room.integration.testapp.vo.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Tests invalidation tracking.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class InvalidationTest {
    private UserDao mUserDao;
    private TestDatabase mDb;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        mDb = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = mDb.getUserDao();
    }

    @Before
    public void swapExecutorDelegate() {
        final ExecutorService ioService = Executors.newSingleThreadExecutor();
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        AppToolkitTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                ioService.execute(runnable);
            }

            @Override
            public void executeOnMainThread(Runnable runnable) {
                mainHandler.post(runnable);
            }

            @Override
            public boolean isMainThread() {
                return Looper.getMainLooper().getThread() == Thread.currentThread();
            }
        });
    }

    @After
    public void removeExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(null);
    }

    private void waitUntilIOThreadIsIdle() {
        FutureTask<Void> future = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return null;
            }
        });
        AppToolkitTaskExecutor.getInstance().executeOnDiskIO(future);
        //noinspection TryWithIdenticalCatches
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInvalidationOnUpdate() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        LatchObserver observer = new LatchObserver(1, "User");
        mDb.getInvalidationTracker().addObserver(observer);
        mUserDao.updateById(3, "foo2");
        waitUntilIOThreadIsIdle();
        assertThat(observer.await(), is(true));
    }

    @Test
    public void testInvalidationOnDelete() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        LatchObserver observer = new LatchObserver(1, "User");
        mDb.getInvalidationTracker().addObserver(observer);
        mUserDao.delete(user);
        waitUntilIOThreadIsIdle();
        assertThat(observer.await(), is(true));
    }

    @Test
    public void testInvalidationOnInsert() throws InterruptedException {
        LatchObserver observer = new LatchObserver(1, "User");
        mDb.getInvalidationTracker().addObserver(observer);
        mUserDao.insert(TestUtil.createUser(3));
        waitUntilIOThreadIsIdle();
        assertThat(observer.await(), is(true));
    }

    @Test
    public void testDontInvalidateOnLateInsert() throws InterruptedException {
        LatchObserver observer = new LatchObserver(1, "User");
        mUserDao.insert(TestUtil.createUser(3));
        waitUntilIOThreadIsIdle();
        mDb.getInvalidationTracker().addObserver(observer);
        waitUntilIOThreadIsIdle();
        assertThat(observer.await(), is(false));
    }

    private static class LatchObserver extends InvalidationTracker.Observer {
        CountDownLatch mLatch;

        LatchObserver(int permits, String... tables) {
            super(tables);
            mLatch = new CountDownLatch(permits);
        }

        boolean await() throws InterruptedException {
            return mLatch.await(5, TimeUnit.SECONDS);
        }

        @Override
        protected void onInvalidated() {
            mLatch.countDown();
        }
    }
}
