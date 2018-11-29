/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.room.integration.testapp.test.TestUtil.forceGc;

import static junit.framework.TestCase.assertFalse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import android.app.Instrumentation;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.InvalidationTracker;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class InvalidationTrackerTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();
    private Instrumentation mInstrumentation;
    private InvalidationTestDatabase mDb;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mDb = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                InvalidationTestDatabase.class)
                .build();
    }

    @Test
    public void testSubscribe_mainThread() throws InterruptedException {
        mDb.setShouldSpyOnInvalidation(true);
        mDb.getOpenHelper().getWritableDatabase(); // Open DB to init InvalidationTracker

        CompositeDisposable disposables = new CompositeDisposable();
        AtomicBoolean mainThreadViolation = new AtomicBoolean();

        // Expect 2 calls to addObserver
        CountDownLatch addLatch = new CountDownLatch(2);
        doAnswer(invocation -> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                mainThreadViolation.set(true);
            }
            addLatch.countDown();
            return null;
        }).when(mDb.getInvalidationTracker())
                .addObserver(any(InvalidationTracker.Observer.class));
        mInstrumentation.runOnMainSync(() -> {
            disposables.add(mDb.getItemDao().flowableItemById(1).subscribe());
            disposables.add(mDb.getItemDao().observableItemById(1).subscribe());
        });
        addLatch.await(10, TimeUnit.SECONDS);

        // Expect 2 calls to removeObserver
        CountDownLatch removeLatch = new CountDownLatch(2);
        doAnswer(invocation -> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                mainThreadViolation.set(true);
            }
            removeLatch.countDown();
            return null;
        }).when(mDb.getInvalidationTracker())
                .removeObserver(any(InvalidationTracker.Observer.class));
        mInstrumentation.runOnMainSync(disposables::dispose);
        removeLatch.await(10, TimeUnit.SECONDS);

        assertFalse("Expected no main thread disk IO violation.", mainThreadViolation.get());
    }

    @Test
    public void createLiveData() throws ExecutionException, InterruptedException, TimeoutException {
        final LiveData<Item> liveData = mDb
                .getInvalidationTracker()
                .createLiveData(new String[]{"Item"}, () -> mDb.getItemDao().itemById(1));

        mDb.getItemDao().insert(new Item(1, "v1"));

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        TestObserver<Item> observer = new MyObserver<>();
        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.hasValue(), is(false));
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        drain();
        assertThat(observer.get(), is(new Item(1, "v1")));

        mDb.getItemDao().insert(new Item(1, "v2"));
        drain();
        assertThat(observer.get(), is(new Item(1, "v2")));
    }

    @Test
    public void createLiveData_observeForever()
            throws ExecutionException, InterruptedException, TimeoutException {
        LiveData<Item> liveData = mDb
                .getInvalidationTracker()
                .createLiveData(new String[]{"Item"}, () -> mDb.getItemDao().itemById(1));

        mDb.getItemDao().insert(new Item(1, "v1"));

        TestObserver<Item> observer = new MyObserver<>();
        TestUtil.observeForeverOnMainThread(liveData, observer);
        assertThat(observer.get(), is(new Item(1, "v1")));

        mDb.getItemDao().insert(new Item(1, "v2"));
        assertThat(observer.get(), is(new Item(1, "v2")));

        //noinspection UnusedAssignment
        liveData = null;
        forceGc();
        mDb.getItemDao().insert(new Item(1, "v3"));
        assertThat(observer.get(), is(new Item(1, "v3")));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    @Database(entities = Item.class, version = 1, exportSchema = false)
    abstract static class InvalidationTestDatabase extends RoomDatabase {

        private boolean mShouldSpyOnInvalidation;

        private InvalidationTracker mInvalidationTrackerSpy;

        public void setShouldSpyOnInvalidation(boolean shouldSpyOnInvalidation) {
            mShouldSpyOnInvalidation = shouldSpyOnInvalidation;
        }

        @NonNull
        @Override
        public InvalidationTracker getInvalidationTracker() {
            if (mShouldSpyOnInvalidation) {
                if (mInvalidationTrackerSpy == null) {
                    mInvalidationTrackerSpy = Mockito.spy(super.getInvalidationTracker());
                }
                return mInvalidationTrackerSpy;
            }
            return super.getInvalidationTracker();
        }

        abstract ItemDao getItemDao();
    }

    @Entity
    static class Item {
        @PrimaryKey
        @NonNull
        public final long id;
        @NonNull
        public final String name;
        Item(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return id == item.id
                    && name.equals(item.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    @Dao
    interface ItemDao {
        @Query("select * from item where id = :id")
        Flowable<Item> flowableItemById(int id);

        @Query("select * from item where id = :id")
        Observable<Item> observableItemById(int id);

        @Query("select * from item where id = :id")
        Item itemById(int id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Item item);
    }

    private class MyObserver<T> extends TestObserver<T> {

        @Override
        protected void drain() throws TimeoutException, InterruptedException {
            InvalidationTrackerTest.this.drain();
        }
    }
}
