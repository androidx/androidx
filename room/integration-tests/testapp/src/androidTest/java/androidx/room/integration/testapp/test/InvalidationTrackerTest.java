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

import static junit.framework.TestCase.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import android.app.Instrumentation;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.InvalidationTracker;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class InvalidationTrackerTest {

    private Instrumentation mInstrumentation;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testSubscribe_mainThread() throws InterruptedException {
        InvalidationTestDatabase db = Room.databaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                InvalidationTestDatabase.class,
                "test_subscribe_database")
                .build();
        db.getOpenHelper().getWritableDatabase(); // Open DB to init InvalidationTracker

        CompositeDisposable disposables = new CompositeDisposable();
        AtomicBoolean mainThreadViolation = new AtomicBoolean();

        // Expect 2 calls to addObserver
        CountDownLatch addLatch = new CountDownLatch(2);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mainThreadViolation.set(true);
                }
                addLatch.countDown();
                return null;
            }
        }).when(db.getInvalidationTracker())
                .addObserver(any(InvalidationTracker.Observer.class));
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                disposables.add(db.getItemDao().flowableItemById(1).subscribe());
                disposables.add(db.getItemDao().observableItemById(1).subscribe());
            }
        });
        addLatch.await(10, TimeUnit.SECONDS);

        // Expect 2 calls to removeObserver
        CountDownLatch removeLatch = new CountDownLatch(2);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mainThreadViolation.set(true);
                }
                removeLatch.countDown();
                return null;
            }
        }).when(db.getInvalidationTracker())
                .removeObserver(any(InvalidationTracker.Observer.class));
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                disposables.dispose();
            }
        });
        removeLatch.await(10, TimeUnit.SECONDS);

        assertFalse("Expected no main thread disk IO violation.", mainThreadViolation.get());
    }

    @Database(entities = Item.class, version = 1, exportSchema = false)
    abstract static class InvalidationTestDatabase extends RoomDatabase {

        private InvalidationTracker mInvalidationTrackerSpy;

        @NonNull
        @Override
        public InvalidationTracker getInvalidationTracker() {
            if (mInvalidationTrackerSpy == null) {
                mInvalidationTrackerSpy = Mockito.spy(super.getInvalidationTracker());
            }
            return mInvalidationTrackerSpy;
        }

        abstract ItemDao getItemDao();
    }

    @Entity
    static class Item {
        @PrimaryKey
        public long id;
    }

    @Dao
    interface ItemDao {
        @Query("select * from item where id = :id")
        Flowable<Item> flowableItemById(int id);

        @Query("select * from item where id = :id")
        Observable<Item> observableItemById(int id);
    }
}
