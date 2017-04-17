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

package android.arch.persistence.room;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import android.arch.core.executor.JunitTaskExecutorRule;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteStatement;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class InvalidationTrackerTest {
    private InvalidationTracker mTracker;
    private RoomDatabase mRoomDatabase;
    private SupportSQLiteOpenHelper mOpenHelper;
    @Rule
    public JunitTaskExecutorRule mTaskExecutorRule = new JunitTaskExecutorRule(1, true);

    @Before
    public void setup() {
        mRoomDatabase = mock(RoomDatabase.class);
        SupportSQLiteDatabase sqliteDb = mock(SupportSQLiteDatabase.class);
        final SupportSQLiteStatement statement = mock(SupportSQLiteStatement.class);
        mOpenHelper = mock(SupportSQLiteOpenHelper.class);

        doReturn(statement).when(sqliteDb).compileStatement(eq(InvalidationTracker.CLEANUP_SQL));
        doReturn(sqliteDb).when(mOpenHelper).getWritableDatabase();
        doReturn(true).when(mRoomDatabase).isOpen();
        //noinspection ResultOfMethodCallIgnored
        doReturn(mOpenHelper).when(mRoomDatabase).getOpenHelper();

        mTracker = new InvalidationTracker(mRoomDatabase, "a", "B", "i");
        mTracker.internalInit(sqliteDb);
    }

    @Before
    public void setLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @After
    public void unsetLocale() {
        Locale.setDefault(Locale.US);
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
        drainTasks();
        assertThat(mTracker.mObserverMap.size(), is(1));
        mTracker.removeObserver(new LatchObserver(1, "a"));
        drainTasks();
        assertThat(mTracker.mObserverMap.size(), is(1));
        mTracker.removeObserver(observer);
        drainTasks();
        assertThat(mTracker.mObserverMap.size(), is(0));
    }

    private void drainTasks() throws InterruptedException {
        mTaskExecutorRule.drainTasks(200);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badObserver() {
        InvalidationTracker.Observer observer = new LatchObserver(1, "x");
        mTracker.addObserver(observer);
    }

    @Test
    public void refreshReadValues() throws Exception {
        setVersions(1, 0, 2, 1);
        refreshSync();
        assertThat(mTracker.mTableVersions, is(new long[]{1, 2, 0}));

        setVersions(3, 1);
        refreshSync();
        assertThat(mTracker.mTableVersions, is(new long[]{1, 3, 0}));

        setVersions(7, 0);
        refreshSync();
        assertThat(mTracker.mTableVersions, is(new long[]{7, 3, 0}));

        refreshSync();
        assertThat(mTracker.mTableVersions, is(new long[]{7, 3, 0}));
    }

    private void refreshSync() throws InterruptedException {
        mTracker.refreshVersionsAsync();
        drainTasks();
    }

    @Test
    public void refreshCheckTasks() throws Exception {
        when(mRoomDatabase.query(anyString(), any(String[].class)))
                .thenReturn(mock(Cursor.class));
        mTracker.refreshVersionsAsync();
        mTracker.refreshVersionsAsync();
        verify(mTaskExecutorRule.getTaskExecutor()).executeOnDiskIO(mTracker.mRefreshRunnable);
        drainTasks();

        reset(mTaskExecutorRule.getTaskExecutor());
        mTracker.refreshVersionsAsync();
        verify(mTaskExecutorRule.getTaskExecutor()).executeOnDiskIO(mTracker.mRefreshRunnable);
    }

    @Test
    public void observe1Table() throws Exception {
        LatchObserver observer = new LatchObserver(1, "a");
        mTracker.addObserver(observer);
        setVersions(1, 0, 2, 1);
        refreshSync();
        assertThat(observer.await(), is(true));

        setVersions(3, 1);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(false));

        setVersions(4, 0);
        refreshSync();
        assertThat(observer.await(), is(true));
    }

    @Test
    public void observe2Tables() throws Exception {
        LatchObserver observer = new LatchObserver(1, "A", "B");
        mTracker.addObserver(observer);
        setVersions(1, 0, 2, 1);
        refreshSync();
        assertThat(observer.await(), is(true));

        setVersions(3, 1);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(true));

        setVersions(4, 0);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(true));

        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(false));
    }

    @Test
    public void locale() {
        LatchObserver observer = new LatchObserver(1, "I");
        mTracker.addObserver(observer);
    }

    @Test
    public void closedDb() {
        doThrow(new IllegalStateException("foo")).when(mOpenHelper).getWritableDatabase();
        mTracker.addObserver(new LatchObserver(1, "a", "b"));
        mTracker.syncTriggers();
        mTracker.mRefreshRunnable.run();
    }

    @Test
    public void closedDbAfterOpen() throws InterruptedException {
        setVersions(3, 1);
        mTracker.addObserver(new LatchObserver(1, "a", "b"));
        mTracker.syncTriggers();
        mTracker.mRefreshRunnable.run();
        doThrow(new SQLiteException("foo")).when(mRoomDatabase).query(
                Mockito.eq(InvalidationTracker.SELECT_UPDATED_TABLES_SQL),
                any(String[].class));
        mTracker.mPendingRefresh.set(true);
        mTracker.mRefreshRunnable.run();
    }

    /**
     * Key value pairs of VERSION, TABLE_ID
     */
    private void setVersions(int... keyValuePairs) throws InterruptedException {
        // mockito does not like multi-threaded access so before setting versions, make sure we
        // sync background tasks.
        drainTasks();
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
        public void onInvalidated() {
            mLatch.countDown();
        }

        void reset(@SuppressWarnings("SameParameterValue") int count) {
            mLatch = new CountDownLatch(count);
        }
    }
}
