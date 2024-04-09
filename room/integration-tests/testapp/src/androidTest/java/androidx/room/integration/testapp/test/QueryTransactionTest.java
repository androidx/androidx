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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.CancellationSignal;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;
import androidx.room.paging.LimitOffsetDataSource;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

@SmallTest
@RunWith(Parameterized.class)
@SuppressWarnings("CheckReturnValue")
public class QueryTransactionTest {
    @Rule
    public CountingTaskExecutorRule countingTaskExecutorRule = new CountingTaskExecutorRule();
    private static final AtomicInteger sStartedTransactionCount = new AtomicInteger(0);
    private TransactionDb mDb;
    private final boolean mUseTransactionDao;
    private Entity1Dao mDao;
    private final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @NonNull
    @Parameterized.Parameters(name = "useTransaction_{0}")
    public static Boolean[] getParams() {
        return new Boolean[]{false, true};
    }

    public QueryTransactionTest(boolean useTransactionDao) {
        mUseTransactionDao = useTransactionDao;
    }

    @Before
    public void initDb() {
        resetTransactionCount();
        mDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                TransactionDb.class)
                .openHelperFactory(new TransactionOpenHelperFactory())
                .build();
        mDao = mUseTransactionDao ? mDb.transactionDao() : mDb.dao();
        drain();
    }

    @After
    public void closeDb() {
        mLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        drain();
        mDb.close();
    }

    @Test
    public void readList() {
        mDao.insert(new Entity1(1, "foo"));
        resetTransactionCount();

        int expectedTransactionCount = mUseTransactionDao ? 1 : 0;
        List<Entity1> allEntities = mDao.allEntities();
        assertTransactionCount(allEntities, expectedTransactionCount);
    }

    @Test
    public void liveData() {
        LiveData<List<Entity1>> listLiveData = mDao.liveData();
        observeForever(listLiveData);
        drain();
        assertThat(listLiveData.getValue(), is(Collections.<Entity1>emptyList()));

        resetTransactionCount();
        mDao.insert(new Entity1(1, "foo"));
        drain();

        //noinspection ConstantConditions
        assertThat(listLiveData.getValue().size(), is(1));
        int expectedTransactionCount = mUseTransactionDao ? 2 : 1;
        assertTransactionCount(listLiveData.getValue(), expectedTransactionCount);
    }

    @Test
    public void flowable() {
        Flowable<List<Entity1>> flowable = mDao.flowable();
        TestSubscriber<List<Entity1>> subscriber = observe(flowable);
        drain();
        assertThat(subscriber.values().size(), is(1));

        resetTransactionCount();
        mDao.insert(new Entity1(1, "foo"));
        drain();

        List<Entity1> allEntities = subscriber.values().get(1);
        assertThat(allEntities.size(), is(1));
        int expectedTransactionCount = mUseTransactionDao ? 2 : 1;
        assertTransactionCount(allEntities, expectedTransactionCount);
    }

    @Test
    public void maybe() {
        mDao.insert(new Entity1(1, "foo"));
        resetTransactionCount();

        int expectedTransactionCount = mUseTransactionDao ? 1 : 0;
        Maybe<List<Entity1>> listMaybe = mDao.maybe();
        TestObserver<List<Entity1>> observer = observe(listMaybe);
        drain();
        List<Entity1> allEntities = observer.values().get(0);
        assertTransactionCount(allEntities, expectedTransactionCount);
    }

    @Test
    public void single() {
        mDao.insert(new Entity1(1, "foo"));
        resetTransactionCount();

        int expectedTransactionCount = mUseTransactionDao ? 1 : 0;
        Single<List<Entity1>> listMaybe = mDao.single();
        TestObserver<List<Entity1>> observer = observe(listMaybe);
        drain();
        List<Entity1> allEntities = observer.values().get(0);
        assertTransactionCount(allEntities, expectedTransactionCount);
    }

    @Test
    public void relation() {
        mDao.insert(new Entity1(1, "foo"));
        mDao.insert(new Child(1, 1));
        mDao.insert(new Child(2, 1));
        resetTransactionCount();

        List<Entity1WithChildren> result = mDao.withRelation();
        int expectedTransactionCount = mUseTransactionDao ? 1 : 0;
        assertTransactionCountWithChildren(result, expectedTransactionCount);
    }

    @Test
    public void pagedList() {
        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(1)
                .setInitialLoadSizeHint(3)
                .setPrefetchDistance(2)
                .setEnablePlaceholders(false)
                .build();
        LiveData<PagedList<Entity1>> pagedList =
                new LivePagedListBuilder<>(mDao.pagedList(), config).build();
        observeForever(pagedList);
        drain();
        assertThat(sStartedTransactionCount.get(), is(1));

        mDao.insert(new Entity1(1, "foo"));
        drain();
        //noinspection ConstantConditions
        assertThat(pagedList.getValue().size(), is(1));
        assertTransactionCount(pagedList.getValue(), 3);

        mDao.insert(
                new Entity1(2, "bar"),
                new Entity1(3, "baz"),
                new Entity1(4, "bup"));
        drain();
        assertThat(pagedList.getValue().size(), is(3));
        assertTransactionCount(pagedList.getValue(), 5);

        // only loadRange is affected by transaction dao, since initial load requires transaction
        pagedList.getValue().loadAround(2);
        drain();
        assertThat(pagedList.getValue().size(), is(4));
        // note: we don't use assertTransactionCount here, since last item loaded separately
        assertThat(sStartedTransactionCount.get(), is(mUseTransactionDao ? 7 : 5));
    }

    @Test
    public void dataSourceRange() {
        mDao.insert(new Entity1(2, "bar"));
        drain();
        resetTransactionCount();
        LimitOffsetDataSource<Entity1> dataSource =
                (LimitOffsetDataSource<Entity1>) mDao.dataSource();
        dataSource.loadRange(0, 10);
        assertThat(sStartedTransactionCount.get(), is(mUseTransactionDao ? 1 : 0));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void dataSourceInitial() {
        mDao.insert(new Entity1(2, "bar"));
        drain();
        resetTransactionCount();
        LimitOffsetDataSource<Entity1> dataSource =
                (LimitOffsetDataSource<Entity1>) mDao.dataSource();
        dataSource.loadInitial(
                new PositionalDataSource.LoadInitialParams(0, 30, 10, true),
                new PositionalDataSource.LoadInitialCallback<Entity1>() {
                    @Override
                    public void onResult(@NonNull List<? extends Entity1> data, int position,
                            int totalCount) {}

                    @Override
                    public void onResult(@NonNull List<? extends Entity1> data, int position) {}
                });
        // always use a transaction, since we're loading count + initial data
        assertThat(sStartedTransactionCount.get(), is(1));
    }

    private void assertTransactionCount(List<Entity1> allEntities, int expectedTransactionCount) {
        assertThat(sStartedTransactionCount.get(), is(expectedTransactionCount));
        assertThat(allEntities.isEmpty(), is(false));
        for (Entity1 entity1 : allEntities) {
            assertThat(entity1.transactionId, is(expectedTransactionCount));
        }
    }

    private void assertTransactionCountWithChildren(List<Entity1WithChildren> allEntities,
            int expectedTransactionCount) {
        assertThat(sStartedTransactionCount.get(), is(expectedTransactionCount));
        assertThat(allEntities.isEmpty(), is(false));
        for (Entity1WithChildren entity1 : allEntities) {
            assertThat(entity1.transactionId, is(expectedTransactionCount));
            assertThat(entity1.children, notNullValue());
            assertThat(entity1.children.isEmpty(), is(false));
            for (Child child : entity1.children) {
                assertThat(child.transactionId, is(expectedTransactionCount));
            }
        }
    }

    private static void incrementTransactionCount() {
        // When incrementing the transaction count, ignore those coming from the refresh
        // in the invalidation tracker.
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String fileName = element.getFileName();
            if (fileName != null && fileName.equals("InvalidationTracker.kt")) {
                return;
            }
        }
        sStartedTransactionCount.incrementAndGet();
    }

    private void resetTransactionCount() {
        sStartedTransactionCount.set(0);
    }

    private void drain() {
        try {
            countingTaskExecutorRule.drainTasks(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new AssertionError("interrupted", e);
        } catch (TimeoutException e) {
            throw new AssertionError("drain timed out", e);
        }
    }

    private <T> TestSubscriber<T> observe(final Flowable<T> flowable) {
        TestSubscriber<T> subscriber = new TestSubscriber<>();
        flowable.observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
                .subscribeWith(subscriber);
        return subscriber;
    }

    private <T> TestObserver<T> observe(final Maybe<T> maybe) {
        TestObserver<T> observer = new TestObserver<>();
        maybe.observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
                .subscribeWith(observer);
        return observer;
    }

    private <T> TestObserver<T> observe(final Single<T> single) {
        TestObserver<T> observer = new TestObserver<>();
        single.observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
                .subscribeWith(observer);
        return observer;
    }

    private <T> void observeForever(final LiveData<T> liveData) {
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            liveData.observe(mLifecycleOwner, t -> {
            });
            return null;
        });
        ArchTaskExecutor.getMainThreadExecutor().execute(futureTask);
        try {
            futureTask.get();
        } catch (InterruptedException e) {
            throw new AssertionError("interrupted", e);
        } catch (ExecutionException e) {
            throw new AssertionError("execution error", e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class Entity1WithChildren extends Entity1 {
        @Relation(entity = Child.class, parentColumn = "id",
                entityColumn = "entity1Id")
        public List<Child> children;

        Entity1WithChildren(int id, String value) {
            super(id, value);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    static class Child {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public int entity1Id;
        @Ignore
        public final int transactionId = sStartedTransactionCount.get();

        Child(int id, int entity1Id) {
            this.id = id;
            this.entity1Id = entity1Id;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    static class Entity1 {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String value;
        @Ignore
        public final int transactionId = sStartedTransactionCount.get();

        Entity1(int id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    // we don't support dao inheritance for queries so for now, go with this
    interface Entity1Dao {
        String SELECT_ALL = "select * from Entity1";

        List<Entity1> allEntities();

        Flowable<List<Entity1>> flowable();

        Maybe<List<Entity1>> maybe();

        Single<List<Entity1>> single();

        LiveData<List<Entity1>> liveData();

        List<Entity1WithChildren> withRelation();

        DataSource.Factory<Integer, Entity1> pagedList();

        PositionalDataSource<Entity1> dataSource();

        @Insert
        void insert(Entity1 entity1);

        @Insert
        void insert(Entity1... entities);

        @Insert
        void insert(Child entity1);
    }

    @Dao
    interface EntityDao extends Entity1Dao {
        @Override
        @Query(SELECT_ALL)
        List<Entity1> allEntities();

        @Override
        @Query(SELECT_ALL)
        Flowable<List<Entity1>> flowable();

        @Override
        @Query(SELECT_ALL)
        LiveData<List<Entity1>> liveData();

        @Override
        @Query(SELECT_ALL)
        Maybe<List<Entity1>> maybe();

        @Override
        @Query(SELECT_ALL)
        Single<List<Entity1>> single();

        @Override
        @Query(SELECT_ALL)
        @SuppressWarnings(RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION)
        List<Entity1WithChildren> withRelation();

        @Override
        @Query(SELECT_ALL)
        DataSource.Factory<Integer, Entity1> pagedList();

        @Override
        @Query(SELECT_ALL)
        PositionalDataSource<Entity1> dataSource();
    }

    @Dao
    interface TransactionDao extends Entity1Dao {
        @Override
        @Transaction
        @Query(SELECT_ALL)
        List<Entity1> allEntities();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        Flowable<List<Entity1>> flowable();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        LiveData<List<Entity1>> liveData();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        Maybe<List<Entity1>> maybe();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        Single<List<Entity1>> single();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        List<Entity1WithChildren> withRelation();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        DataSource.Factory<Integer, Entity1> pagedList();

        @Override
        @Transaction
        @Query(SELECT_ALL)
        PositionalDataSource<Entity1> dataSource();
    }

    @Database(version = 1, entities = {Entity1.class, Child.class}, exportSchema = false)
    @SuppressWarnings("deprecation")
    abstract static class TransactionDb extends RoomDatabase {
        abstract EntityDao dao();

        abstract TransactionDao transactionDao();
    }

    static class TransactionOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

        private final SupportSQLiteOpenHelper.Factory mDelegate =
                new FrameworkSQLiteOpenHelperFactory();

        @NonNull
        @Override
        public SupportSQLiteOpenHelper create(
                @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
            return new TransactionSupportSQLiteOpenHelper(mDelegate.create(configuration));
        }
    }

    static class TransactionSupportSQLiteOpenHelper implements SupportSQLiteOpenHelper {
        private final SupportSQLiteOpenHelper mDelegate;

        TransactionSupportSQLiteOpenHelper(SupportSQLiteOpenHelper delegate) {
            this.mDelegate = delegate;
        }

        @Nullable
        @Override
        public String getDatabaseName() {
            return mDelegate.getDatabaseName();
        }

        @Override
        public void setWriteAheadLoggingEnabled(boolean enabled) {
            mDelegate.setWriteAheadLoggingEnabled(enabled);
        }

        @NonNull
        @Override
        public SupportSQLiteDatabase getWritableDatabase() {
            return new TransactionSupportSQLiteDatabase(mDelegate.getWritableDatabase());
        }

        @NonNull
        @Override
        public SupportSQLiteDatabase getReadableDatabase() {
            return new TransactionSupportSQLiteDatabase(mDelegate.getReadableDatabase());
        }

        @Override
        public void close() {
            mDelegate.close();
        }
    }

    static class TransactionSupportSQLiteDatabase implements SupportSQLiteDatabase {
        private final SupportSQLiteDatabase mDelegate;

        TransactionSupportSQLiteDatabase(SupportSQLiteDatabase delegate) {
            this.mDelegate = delegate;
        }

        @NonNull
        @Override
        public SupportSQLiteStatement compileStatement(@NonNull String sql) {
            return mDelegate.compileStatement(sql);
        }

        @Override
        public void beginTransaction() {
            mDelegate.beginTransaction();
            incrementTransactionCount();
        }

        @Override
        public void beginTransactionNonExclusive() {
            mDelegate.beginTransactionNonExclusive();
            incrementTransactionCount();
        }

        @Override
        public void beginTransactionReadOnly() {
            mDelegate.beginTransactionReadOnly();
            incrementTransactionCount();
        }

        @Override
        public void beginTransactionWithListener(
                @NonNull SQLiteTransactionListener transactionListener) {
            mDelegate.beginTransactionWithListener(transactionListener);
            incrementTransactionCount();
        }

        @Override
        public void beginTransactionWithListenerNonExclusive(
                @NonNull SQLiteTransactionListener transactionListener) {
            mDelegate.beginTransactionWithListenerNonExclusive(transactionListener);
            incrementTransactionCount();
        }

        @Override
        public void beginTransactionWithListenerReadOnly(
                @NonNull SQLiteTransactionListener transactionListener) {
            mDelegate.beginTransactionWithListenerReadOnly(transactionListener);
            incrementTransactionCount();
        }

        @Override
        public void endTransaction() {
            mDelegate.endTransaction();
        }

        @Override
        public void setTransactionSuccessful() {
            mDelegate.setTransactionSuccessful();
        }

        @Override
        public boolean inTransaction() {
            return mDelegate.inTransaction();
        }

        @Override
        public boolean isDbLockedByCurrentThread() {
            return mDelegate.isDbLockedByCurrentThread();
        }

        @Override
        public boolean yieldIfContendedSafely() {
            return mDelegate.yieldIfContendedSafely();
        }

        @Override
        public boolean yieldIfContendedSafely(long sleepAfterYieldDelayMillis) {
            return mDelegate.yieldIfContendedSafely(sleepAfterYieldDelayMillis);
        }

        @Override
        public boolean isExecPerConnectionSQLSupported() {
            return mDelegate.isExecPerConnectionSQLSupported();
        }

        @Override
        public void execPerConnectionSQL(@NonNull String sql, @Nullable Object[] bindArgs) {
            mDelegate.execPerConnectionSQL(sql, bindArgs);
        }

        @Override
        public int getVersion() {
            return mDelegate.getVersion();
        }

        @Override
        public void setVersion(int i) {
            mDelegate.setVersion(i);
        }

        @Override
        public long getMaximumSize() {
            return mDelegate.getMaximumSize();
        }

        @Override
        public long setMaximumSize(long numBytes) {
            return mDelegate.setMaximumSize(numBytes);
        }

        @Override
        public long getPageSize() {
            return mDelegate.getPageSize();
        }

        @Override
        public void setPageSize(long l) {
            mDelegate.setPageSize(l);
        }

        @NonNull
        @Override
        public Cursor query(@NonNull String query) {
            return mDelegate.query(query);
        }

        @NonNull
        @Override
        public Cursor query(@NonNull String query, @NonNull Object[] bindArgs) {
            return mDelegate.query(query, bindArgs);
        }

        @NonNull
        @Override
        public Cursor query(@NonNull SupportSQLiteQuery query) {
            return mDelegate.query(query);
        }

        @NonNull
        @Override
        public Cursor query(@NonNull SupportSQLiteQuery query,
                @Nullable CancellationSignal cancellationSignal) {
            return mDelegate.query(query, cancellationSignal);
        }

        @Override
        public long insert(@NonNull String table, int conflictAlgorithm,
                @NonNull ContentValues values) throws SQLException {
            return mDelegate.insert(table, conflictAlgorithm, values);
        }

        @Override
        public int delete(@NonNull String table, @Nullable String whereClause,
                @Nullable Object[] whereArgs) {
            return mDelegate.delete(table, whereClause, whereArgs);
        }

        @Override
        public int update(@NonNull String table, int conflictAlgorithm,
                @NonNull ContentValues values, @Nullable String whereClause,
                @Nullable Object[] whereArgs) {
            return mDelegate.update(table, conflictAlgorithm, values, whereClause, whereArgs);
        }

        @Override
        public void execSQL(@NonNull String sql) throws SQLException {
            mDelegate.execSQL(sql);
        }

        @Override
        public void execSQL(@NonNull String sql, @NonNull Object[] bindArgs) throws SQLException {
            mDelegate.execSQL(sql, bindArgs);
        }

        @Override
        public boolean isReadOnly() {
            return mDelegate.isReadOnly();
        }

        @Override
        public boolean isOpen() {
            return mDelegate.isOpen();
        }

        @Override
        public boolean needUpgrade(int newVersion) {
            return mDelegate.needUpgrade(newVersion);
        }

        @Nullable
        @Override
        public String getPath() {
            return mDelegate.getPath();
        }

        @Override
        public void setLocale(@NonNull Locale locale) {
            mDelegate.setLocale(locale);
        }

        @Override
        public void setMaxSqlCacheSize(int cacheSize) {
            mDelegate.setMaxSqlCacheSize(cacheSize);
        }

        @Override
        public void setForeignKeyConstraintsEnabled(boolean enabled) {
            mDelegate.setForeignKeyConstraintsEnabled(enabled);
        }

        @Override
        public boolean enableWriteAheadLogging() {
            return mDelegate.enableWriteAheadLogging();
        }

        @Override
        public void disableWriteAheadLogging() {
            mDelegate.disableWriteAheadLogging();
        }

        @Override
        public boolean isWriteAheadLoggingEnabled() {
            return mDelegate.isWriteAheadLoggingEnabled();
        }

        @Nullable
        @Override
        public List<Pair<String, String>> getAttachedDbs() {
            return mDelegate.getAttachedDbs();
        }

        @Override
        public boolean isDatabaseIntegrityOk() {
            return mDelegate.isDatabaseIntegrityOk();
        }

        @Override
        public void close() throws IOException {
            mDelegate.close();
        }
    }
}
