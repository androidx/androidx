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

package com.android.support.room;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.Cursor;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteStatement;
import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.executors.TaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class InvalidationTrackerTest {
    private InvalidationTracker mTracker;
    private TaskExecutor mTaskExecutor;
    private RoomDatabase mRoomDatabase;

    @Before
    public void setup() {
        mRoomDatabase = mock(RoomDatabase.class);
        SupportSQLiteDatabase sqliteDb = mock(SupportSQLiteDatabase.class);
        when(sqliteDb.compileStatement(eq(InvalidationTracker.CLEANUP_SQL))).thenReturn(
                mock(SupportSQLiteStatement.class));
        SupportSQLiteOpenHelper openHelper = mock(SupportSQLiteOpenHelper.class);
        when(openHelper.getWritableDatabase()).thenReturn(sqliteDb);
        when(mRoomDatabase.getOpenHelper()).thenReturn(openHelper);
        mTracker = new InvalidationTracker(mRoomDatabase, "a", "B");
        mTracker.internalInit(sqliteDb);
    }

    @Before
    public void swapExecutorDelegate() {
        mTaskExecutor = spy(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                runnable.run();
            }

            @Override
            public void executeOnMainThread(Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
        AppToolkitTaskExecutor.getInstance().setDelegate(mTaskExecutor);
    }

    @After
    public void removeExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void tableIds() {
        assertThat(mTracker.mTableIdLookup.get("a"), is(0));
        assertThat(mTracker.mTableIdLookup.get("b"), is(1));
    }

    @Test
    public void addRemoveObserver() throws Exception {
        InvalidationTracker.Observer observer = new LatchObserver(1, "a");
        mTracker.addObserver(observer);
        assertThat(mTracker.mObserverSet.size(), is(1));
        mTracker.removeObserver(new LatchObserver(1, "a"));
        assertThat(mTracker.mObserverSet.size(), is(1));
        mTracker.removeObserver(observer);
        assertThat(mTracker.mObserverSet.size(), is(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badObserver() {
        InvalidationTracker.Observer observer = new LatchObserver(1, "x");
        mTracker.addObserver(observer);
    }

    @Test
    public void refreshReadValues() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return nullValue();
            }
        }).when(mTaskExecutor).executeOnDiskIO(any(Runnable.class));

        setVersions(1, 0, 2, 1);
        mTracker.refreshVersionsAsync();
        assertThat(mTracker.mTableVersions, is(new long[]{1, 2}));

        setVersions(3, 1);
        mTracker.refreshVersionsAsync();
        assertThat(mTracker.mTableVersions, is(new long[]{1, 3}));

        setVersions(7, 0);
        mTracker.refreshVersionsAsync();
        assertThat(mTracker.mTableVersions, is(new long[]{7, 3}));

        mTracker.refreshVersionsAsync();
        assertThat(mTracker.mTableVersions, is(new long[]{7, 3}));
    }

    @Test
    public void refreshCheckTasks() throws Exception {
        when(mRoomDatabase.query(anyString(), any(String[].class)))
                .thenReturn(mock(Cursor.class));
        doNothing().when(mTaskExecutor).executeOnDiskIO(any(Runnable.class));
        mTracker.refreshVersionsAsync();
        mTracker.refreshVersionsAsync();
        verify(mTaskExecutor).executeOnDiskIO(mTracker.mRefreshRunnable);
        mTracker.mRefreshRunnable.run();

        reset(mTaskExecutor);
        mTracker.refreshVersionsAsync();
        verify(mTaskExecutor).executeOnDiskIO(mTracker.mRefreshRunnable);
    }

    @Test
    public void observe1Table() throws Exception {
        LatchObserver observer = new LatchObserver(1, "a");
        mTracker.addObserver(observer);
        setVersions(1, 0, 2, 1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(true));

        setVersions(3, 1);
        observer.reset(1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(false));

        setVersions(4, 0);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(true));
    }

    @Test
    public void observe2Tables() throws Exception {
        LatchObserver observer = new LatchObserver(1, "A", "B");
        mTracker.addObserver(observer);
        setVersions(1, 0, 2, 1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(true));

        setVersions(3, 1);
        observer.reset(1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(true));

        setVersions(4, 0);
        observer.reset(1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(true));

        observer.reset(1);
        mTracker.refreshVersionsAsync();
        assertThat(observer.await(), is(false));
    }

    /**
     * Key value pairs of VERSION, TABLE_ID
     */
    private void setVersions(int... keyValuePairs) {
        Cursor cursor = createCursorWithValues(keyValuePairs);
        doReturn(cursor).when(mRoomDatabase).query(
                Mockito.eq(InvalidationTracker.SELECT_UPDATED_TABLES_SQL),
                any(String[].class)
        );
    }

    private Cursor createCursorWithValues(final int... keyValuePairs) {
        Cursor cursor = mock(Cursor.class);
        final AtomicInteger index = new AtomicInteger(-2);
        when(cursor.moveToNext()).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return index.addAndGet(2) < keyValuePairs.length;
            }
        });
        Answer<Integer> answer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return keyValuePairs[index.intValue() + (Integer) invocation.getArguments()[0]];
            }
        };
        when(cursor.getInt(anyInt())).thenAnswer(answer);
        when(cursor.getLong(anyInt())).thenAnswer(answer);
        return cursor;
    }

    static class LatchObserver extends InvalidationTracker.Observer {
        private CountDownLatch mLatch;

        LatchObserver(int count, String... tableNames) {
            super(tableNames);
            mLatch = new CountDownLatch(count);
        }

        boolean await() throws InterruptedException {
            return mLatch.await(3, TimeUnit.SECONDS);
        }

        @Override
        protected void onInvalidated() {
            mLatch.countDown();
        }

        void reset(@SuppressWarnings("SameParameterValue") int count) {
            mLatch = new CountDownLatch(count);
        }
    }
}
