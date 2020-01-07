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

package androidx.message.browser;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.AbstractResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages {@link SequencedFuture} that contains sequence number to be shared across the process.
 */
class SequencedFutureManager {
    private static final String TAG = "SequencedFutureManager";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mLock = new Object();
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayMap<Integer, SequencedFuture<?>> mSeqToFutureMap = new ArrayMap<>();
    @GuardedBy("mLock")
    private int mNextSequenceNumber;

    /**
     * Obtains next sequence number without creating future. Used for methods with no return
     * (e.g. close())
     *
     * @return sequence number
     */
    public int obtainNextSequenceNumber() {
        synchronized (mLock) {
            return mNextSequenceNumber++;
        }
    }

    /**
     * Creates {@link SequencedFuture} with sequence number. Used to return
     * {@link ListenableFuture} for remote process call.
     *
     * @return AbstractFuture with sequence number
     */
    public <T> SequencedFuture<T> createSequencedFuture(T resultWhenClosed) {
        synchronized (mLock) {
            int seq = obtainNextSequenceNumber();
            SequencedFuture<T> result = new SequencedFuture<>(seq, resultWhenClosed);
            mSeqToFutureMap.put(seq, result);
            return result;
        }
    }

    /**
     * Sets result of the {@link SequencedFuture} with the sequence id. Specified future will be
     * removed from the manager.
     *
     * @param seq sequence number to find future
     * @param result result to set
     */
    @SuppressWarnings("unchecked")
    public <T> void setFutureResult(int seq, T result) {
        synchronized (mLock) {
            SequencedFuture<?> future = mSeqToFutureMap.remove(seq);
            if (future != null) {
                if (result == null
                        || future.getResultWhenClosed().getClass() == result.getClass()) {
                    ((SequencedFuture<T>) future).set(result);
                } else {
                    Log.w(TAG, "Type mismatch, expected "
                            + future.getResultWhenClosed().getClass()
                            + ", but was " + result.getClass());
                }
            }
        }
    }

    public void close() {
        List<SequencedFuture<?>> pendingResults;
        synchronized (mLock) {
            pendingResults = new ArrayList<>(mSeqToFutureMap.values());
            mSeqToFutureMap.clear();
        }
        for (SequencedFuture<?> result: pendingResults) {
            result.setWithTheValueOfResultWhenClosed();
        }
    }

    // TODO: Find a way to remove @SuppressLint
    @SuppressLint("RestrictedApi")
    static final class SequencedFuture<T> extends AbstractResolvableFuture<T> {
        private final int mSequenceNumber;
        private final T mResultWhenClosed;

        SequencedFuture(int seq, @NonNull T resultWhenClosed) {
            mSequenceNumber = seq;
            mResultWhenClosed = resultWhenClosed;
        }

        @Override
        public boolean set(@Nullable T value) {
            return super.set(value);
        }

        void setWithTheValueOfResultWhenClosed() {
            set(mResultWhenClosed);
        }

        public int getSequenceNumber() {
            return mSequenceNumber;
        }

        public @NonNull T getResultWhenClosed() {
            return mResultWhenClosed;
        }
    }
}
