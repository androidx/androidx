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

package androidx.room;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.JunitTaskExecutorRule;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteStatement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@RunWith(JUnit4.class)
public class InvalidationTrackerTest {
    private InvalidationTracker mTracker;
    @Mock
    private RoomDatabase mRoomDatabase;
    @Mock
    private SupportSQLiteDatabase mSqliteDb;
    @Mock
    private SupportSQLiteOpenHelper mOpenHelper;
    @Rule
    public JunitTaskExecutorRule mTaskExecutorRule = new JunitTaskExecutorRule(1, true);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final SupportSQLiteStatement statement = mock(SupportSQLiteStatement.class);
        doReturn(statement).when(mSqliteDb)
                .compileStatement(eq(InvalidationTracker.RESET_UPDATED_TABLES_SQL));
        doReturn(mSqliteDb).when(mOpenHelper).getWritableDatabase();
        doReturn(true).when(mRoomDatabase).isOpen();
        doReturn(ArchTaskExecutor.getIOThreadExecutor()).when(mRoomDatabase).getQueryExecutor();
        ReentrantLock closeLock = new ReentrantLock();
        doReturn(closeLock).when(mRoomDatabase).getCloseLock();
        //noinspection ResultOfMethodCallIgnored
        doReturn(mOpenHelper).when(mRoomDatabase).getOpenHelper();
        HashMap<String, String> shadowTables = new HashMap<>();
        shadowTables.put("C", "D");
        HashMap<String, Set<String>> viewTables = new HashMap<>();
        HashSet<String> tableSet = new HashSet<>();
        tableSet.add("a");
        viewTables.put("e", tableSet);
        mTracker = new InvalidationTracker(mRoomDatabase, shadowTables, viewTables,
                "a", "B", "i", "C");
        mTracker.internalInit(mSqliteDb);
        reset(mSqliteDb);
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
        assertThat(mTracker.mTableIdLookup.get("i"), is(2));
        assertThat(mTracker.mTableIdLookup.get("c"), is(3));
    }

    @Test
    public void testWeak() throws InterruptedException {
        final AtomicInteger data = new AtomicInteger(0);
        InvalidationTracker.Observer observer = new InvalidationTracker.Observer("a") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                data.incrementAndGet();
            }
        };
        mTracker.addWeakObserver(observer);
        setInvalidatedTables(0);
        refreshSync();
        assertThat(data.get(), is(1));
        observer = null;
        forceGc();
        setInvalidatedTables(0);
        refreshSync();
        assertThat(data.get(), is(1));
    }

    @Test
    public void addRemoveObserver() throws Exception {
        InvalidationTracker.Observer observer = new LatchObserver(1, "a");
        mTracker.addObserver(observer);
        assertThat(mTracker.mObserverMap.size(), is(1));
        mTracker.removeObserver(new LatchObserver(1, "a"));
        assertThat(mTracker.mObserverMap.size(), is(1));
        mTracker.removeObserver(observer);
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

    private void refreshSync() throws InterruptedException {
        mTracker.refreshVersionsAsync();
        drainTasks();
    }

    @Test
    public void refreshCheckTasks() throws Exception {
        when(mRoomDatabase.query(any(SimpleSQLiteQuery.class))).thenReturn(mock(Cursor.class));
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
        setInvalidatedTables(0);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("a"));

        setInvalidatedTables(1);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(false));

        setInvalidatedTables(0);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("a"));
    }

    @Test
    public void observe2Tables() throws Exception {
        LatchObserver observer = new LatchObserver(1, "A", "B");
        mTracker.addObserver(observer);
        setInvalidatedTables(0, 1);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(2));
        assertThat(observer.getInvalidatedTables(), hasItems("A", "B"));

        setInvalidatedTables(1, 2);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("B"));

        setInvalidatedTables(0, 3);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("A"));

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
        doReturn(false).when(mRoomDatabase).isOpen();
        doThrow(new IllegalStateException("foo")).when(mOpenHelper).getWritableDatabase();
        mTracker.addObserver(new LatchObserver(1, "a", "b"));
        mTracker.mRefreshRunnable.run();
    }

    @Test
    public void createTriggerOnShadowTable() {
        LatchObserver observer = new LatchObserver(1, "C");
        String[] triggers = new String[]{"UPDATE", "DELETE", "INSERT"};
        ArgumentCaptor<String> sqlArgCaptor;
        List<String> sqlCaptorValues;

        mTracker.addObserver(observer);
        sqlArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mSqliteDb, times(4)).execSQL(sqlArgCaptor.capture());
        sqlCaptorValues = sqlArgCaptor.getAllValues();
        assertThat(sqlCaptorValues.get(0),
                is("INSERT OR IGNORE INTO room_table_modification_log VALUES(3, 0)"));
        for (int i = 0; i < triggers.length; i++) {
            assertThat(sqlCaptorValues.get(i + 1),
                    is("CREATE TEMP TRIGGER IF NOT EXISTS "
                            + "`room_table_modification_trigger_d_" + triggers[i] + "` AFTER "
                            + triggers[i] + " ON `d` BEGIN UPDATE room_table_modification_log "
                            + "SET invalidated = 1 WHERE table_id = 3 AND invalidated = 0; END"
                    ));
        }

        reset(mSqliteDb);

        mTracker.removeObserver(observer);
        sqlArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mSqliteDb, times(3)).execSQL(sqlArgCaptor.capture());
        sqlCaptorValues = sqlArgCaptor.getAllValues();
        for (int i = 0; i < triggers.length; i++) {
            assertThat(sqlCaptorValues.get(i),
                    is("DROP TRIGGER IF EXISTS `room_table_modification_trigger_d_"
                            + triggers[i] + "`"));
        }
    }

    @Test
    public void observeView() throws InterruptedException {
        LatchObserver observer = new LatchObserver(1, "E");
        mTracker.addObserver(observer);
        setInvalidatedTables(0, 1);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("a"));

        setInvalidatedTables(2, 3);
        observer.reset(1);
        refreshSync();
        assertThat(observer.await(), is(false));

        setInvalidatedTables(0, 1);
        refreshSync();
        assertThat(observer.await(), is(true));
        assertThat(observer.getInvalidatedTables().size(), is(1));
        assertThat(observer.getInvalidatedTables(), hasItem("a"));
    }

    @Test
    public void failFastCreateLiveData() {
        // assert that sending a bad createLiveData table name fails instantly
        try {
            mTracker.createLiveData(new String[]{"invalid table name"}, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    return null;
                }
            });
            Assert.fail("should've throw an exception for invalid table name");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // @Test - disabled due to flakiness b/65257997
    public void closedDbAfterOpen() throws InterruptedException {
        setInvalidatedTables(3, 1);
        mTracker.addObserver(new LatchObserver(1, "a", "b"));
        mTracker.syncTriggers();
        mTracker.mRefreshRunnable.run();
        doThrow(new SQLiteException("foo")).when(mRoomDatabase).query(
                Mockito.eq(InvalidationTracker.SELECT_UPDATED_TABLES_SQL),
                any(Object[].class));
        mTracker.mPendingRefresh.set(true);
        mTracker.mRefreshRunnable.run();
    }

    /**
     * Key value pairs of VERSION, TABLE_ID
     */
    private void setInvalidatedTables(int... tableIds) throws InterruptedException {
        // mockito does not like multi-threaded access so before setting versions, make sure we
        // sync background tasks.
        drainTasks();
        Cursor cursor = createCursorWithValues(tableIds);
        doReturn(cursor).when(mRoomDatabase).query(
                argThat(new ArgumentMatcher<SimpleSQLiteQuery>() {
                    @Override
                    public boolean matches(SimpleSQLiteQuery argument) {
                        return argument.getSql().equals(
                                InvalidationTracker.SELECT_UPDATED_TABLES_SQL);
                    }
                })
        );
    }

    private Cursor createCursorWithValues(final int... tableIds) {
        Cursor cursor = mock(Cursor.class);
        final AtomicInteger index = new AtomicInteger(-1);
        when(cursor.moveToNext()).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return index.addAndGet(1) < tableIds.length;
            }
        });
        Answer<Integer> intAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return tableIds[index.intValue()
                        + (Integer) invocation.getArguments()[0]];
            }
        };
        when(cursor.getInt(anyInt())).thenAnswer(intAnswer);
        return cursor;
    }

    static class LatchObserver extends InvalidationTracker.Observer {
        private CountDownLatch mLatch;
        private Set<String> mInvalidatedTables;

        LatchObserver(int count, String... tableNames) {
            super(tableNames);
            mLatch = new CountDownLatch(count);
        }

        boolean await() throws InterruptedException {
            return mLatch.await(3, TimeUnit.SECONDS);
        }

        @Override
        public void onInvalidated(@NonNull Set<String> tables) {
            mInvalidatedTables = tables;
            mLatch.countDown();
        }

        void reset(@SuppressWarnings("SameParameterValue") int count) {
            mInvalidatedTables = null;
            mLatch = new CountDownLatch(count);
        }

        Set<String> getInvalidatedTables() {
            return mInvalidatedTables;
        }
    }

    private static void forceGc() {
        // Use a random index in the list to detect the garbage collection each time because
        // .get() may accidentally trigger a strong reference during collection.
        ArrayList<WeakReference<byte[]>> leak = new ArrayList<>();
        do {
            WeakReference<byte[]> arr = new WeakReference<>(new byte[100]);
            leak.add(arr);
        } while (leak.get((int) (Math.random() * leak.size())).get() != null);
    }
}
