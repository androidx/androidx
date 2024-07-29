/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.guava;

import static com.google.common.truth.Truth.assertThat;

import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.concurrent.Executor;

@SmallTest
public class GuavaRoomTest {

    @Test
    @SuppressWarnings("deprecation")
    public void queryIsCancelled() {
        Executor executor = runnable -> { /* nothing to do */ };

        CancellationSignal signal = new CancellationSignal();
        ListenableFuture<Integer> future = GuavaRoom.createListenableFuture(
                new TestDatabase(executor), false, () -> 1, RoomSQLiteQuery.acquire("", 0), false,
                signal);

        future.cancel(true);

        assertThat(signal.isCanceled()).isTrue();
    }

    private static class TestDatabase extends RoomDatabase {

        private final Executor mTestExecutor;

        private TestDatabase(Executor testExecutor) {
            mTestExecutor = testExecutor;
        }

        @NonNull
        @Override
        public Executor getQueryExecutor() {
            return mTestExecutor;
        }

        @NonNull
        @Override
        protected InvalidationTracker createInvalidationTracker() {
            return null;
        }

        @Override
        public void clearAllTables() {
            throw new UnsupportedOperationException("Shouldn't be called!");
        }
    }

}
