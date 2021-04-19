/*
 * Copyright 2021 The Android Open Source Project
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
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.InvalidationTracker;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Regression test for a situation where an InvalidationTracker callback may intermittently be
 * invoked too early, too late, or not at all, due to missing transactionality in tracking table
 * code, when distinct database updates occur in close temporal proximity.
 */
@LargeTest
@FlakyTest(
        bugId = 154040286,
        detail = "Behavioral test for potentially intermittent InvalidationTracker problems"
)
@RunWith(AndroidJUnit4.class)
public class InvalidationTrackerBehavioralTest {
    private ExecutorService mExecutorService;

    @Before
    public void setup() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        mExecutorService.shutdown();
    }

    @Test
    public void testInserts_JournalModeTruncate() throws ExecutionException, InterruptedException {
        testInserts(RoomDatabase.JournalMode.TRUNCATE);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public void testInserts_JournalModeWAL() throws ExecutionException, InterruptedException {
        testInserts(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING);
    }

    private void testInserts(RoomDatabase.JournalMode journalMode)
            throws ExecutionException, InterruptedException {
        testInserts(journalMode, true);
        testInserts(journalMode, false);
    }

    private void testInserts(RoomDatabase.JournalMode journalMode, boolean multiInstance)
            throws ExecutionException, InterruptedException {
        final RoomDatabase.Builder<DB> dbBuilder = Room
                // We need a physical DB to more easily reproduce invalidation callback errors,
                // and to support enableMultiInstanceInvalidation, which in turn helps reproduce
                // missed invalidation callbacks
                .databaseBuilder(ApplicationProvider.getApplicationContext(), DB.class, DB.NAME)
                .setJournalMode(journalMode);
        if (multiInstance) {
            // Helps reproduce missed invalidation callbacks
            dbBuilder.enableMultiInstanceInvalidation();
        }

        DB db = dbBuilder.build();

        try {
            testInserts(db, 30, 0L, 0);
            testInserts(db, 30, 0L, 10);
            testInserts(db, 30, 0L, 100);
            testInserts(db, 30, 0L, 1_000);
            testInserts(db, 30, 0L, 10_000);
            testInserts(db, 30, 0L, 100_000);
            testInserts(db, 30, 1L, 0);
        } finally {
            db.close();
            ApplicationProvider.getApplicationContext().deleteDatabase(DB.NAME);
        }
    }

    /**
     * Uses repetitions within the test to better approximate real-life behavior, rather than
     * scheduling the whole test for repeated runs from the outside.
     */
    private void testInserts(
            final DB db, final int iterations, final long delayMillis, final int delayNanos
    ) throws ExecutionException, InterruptedException {
        final AtomicInteger missedInvalidations = new AtomicInteger();
        final AtomicInteger spuriousInvalidations = new AtomicInteger();

        // Does not terminate execution as soon as a problem is detected, for simplicity.
        // Usually there should not be a problem; termination is delayed only when there is a
        // problem.
        mExecutorService.submit(new Runnable() {
            @Nullable
            volatile CountDownLatch mLatch = null;

            // Releases latch when change notification received, increments
            // spuriousInvalidations when notification received without a recent change
            final InvalidationTracker.Observer mInvalidationObserver =
                    new InvalidationTracker.Observer(Counter2.TABLE_NAME) {
                        @Override
                        public void onInvalidated(@NonNull Set<String> tables) {
                            if (tables.contains(Counter2.TABLE_NAME)) {
                                // Reading the latch field value is a bit racy,
                                // but it does not matter:
                                //
                                // If we see null here then we're either too early or too late;
                                // too late means that our long delay was too short, so we'd
                                // need to adjust it because now the test failed.
                                // Too early means that we received a spurious invalidation, so
                                // we need to fail the test.
                                //
                                // If we see non-null here instead of null due to a race then
                                // our long delay was just too short and we'll need to adjust it
                                // because the test will have failed. latch.countDown() happens
                                // too late in this case but it has no particular effect.
                                final CountDownLatch latch = mLatch;
                                if (latch == null) {
                                    // Spurious invalidation callback; this might occur due to a
                                    // large delay beyond the provisioned margin, or due to a
                                    // bug in the code under test
                                    spuriousInvalidations.incrementAndGet();
                                } else {
                                    latch.countDown();
                                }
                            }
                        }
                    };

            @Override
            public void run() {
                // Ulterior use of this background thread to add the observer, which is not
                // legal to do from main thread.
                // To be close to a real use case we only register the observer once,
                // we do not re-register for each loop iteration.
                db.getInvalidationTracker().addObserver(mInvalidationObserver);

                try {
                    // Resets latch and updates missedInvalidations when change notification failed
                    for (int i = 0; i < iterations; ++i) {
                        // The Counter1 table exists just to make InvalidationTracker's life more
                        // difficult, we are not interested in notifications from this one;
                        // inserts may trigger undefined invalidation callback behavior,
                        // depending on table update timing
                        db.counterDao().insert(new Counter1());

                        // Use variable delay to detect different kinds of timing-related problems
                        try {
                            Thread.sleep(delayMillis, delayNanos);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        final CountDownLatch latch = new CountDownLatch(1);

                        db.runInTransaction(() -> {
                            db.counterDao().insert(new Counter2());

                            // Flag that we have inserted a new value, expect invalidation callback;
                            // do this as late as possible prior to the end of the transaction;
                            // this might cause an occasional false negative due to a race,
                            // where a buggy InvalidationTracker could log successful tracking
                            // even though the transaction is not completed yet, but it does not
                            // matter much, as this is an intentionally flaky test; on another run
                            // it should become apparent that InvalidationTracker is buggy.
                            mLatch = latch;
                        });

                        // Use sufficient delay to give invalidation tracker ample time to catch up;
                        // this would need to be increased if the test had false positives.
                        try {
                            if (!latch.await(10L, TimeUnit.SECONDS)) {
                                // The tracker still has not been called, log an error
                                missedInvalidations.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        mLatch = null;
                    }
                } finally {
                    db.getInvalidationTracker().removeObserver(mInvalidationObserver);
                }
            }
        }).get();

        assertThat("Missed invalidations on " + iterations + " iterations with delay of " +
                delayMillis + " ms, " + delayNanos + " ns", missedInvalidations.get(), is(0));
        assertThat("Spurious invalidations on " + iterations + " iterations with delay of " +
                delayMillis + " ms, " + delayNanos + " ns", spuriousInvalidations.get(), is(0));
    }

    @Database(entities = { Counter1.class, Counter2.class }, version = 1, exportSchema = false)
    abstract static class DB extends RoomDatabase {
        static final String NAME = "invalidationtrackerbehavioraltest";

        abstract CounterDao counterDao();
    }

    @Entity(tableName = Counter1.TABLE_NAME)
    static final class Counter1 {
        static final String TABLE_NAME = "counter1";

        @PrimaryKey(autoGenerate = true)
        long value;
    }

    @Entity(tableName = Counter2.TABLE_NAME)
    static final class Counter2 {
        static final String TABLE_NAME = "counter2";

        @PrimaryKey(autoGenerate = true)
        long value;
    }

    @Dao
    abstract static class CounterDao {
        @Insert
        abstract void insert(Counter1 entity);

        @Insert
        abstract void insert(Counter2 entity);
    }
}
