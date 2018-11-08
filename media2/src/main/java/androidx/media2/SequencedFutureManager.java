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

package androidx.media2;

import android.annotation.TargetApi;
import android.os.Build;
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
@TargetApi(Build.VERSION_CODES.P)
class SequencedFutureManager implements AutoCloseable {
    private static final boolean DEBUG = false;
    private static final String TAG = "SequencedFutureManager";
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mNextSequenceNumber;
    @GuardedBy("mLock")
    private ArrayMap<Integer, SequencedFuture> mSeqToFutureMap = new ArrayMap<>();

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
    // TODO: Find better way to get closed result -- result has completion time, and it should be
    //       set when the manager is closed, not now.
    public <T> SequencedFuture<T> createSequencedFuture(T resultWhenClosed) {
        final SequencedFuture<T> result;
        final int seq;
        synchronized (mLock) {
            seq = obtainNextSequenceNumber();
            result = SequencedFuture.create(seq, resultWhenClosed);
            mSeqToFutureMap.put(seq, result);
        }
        return result;
    }

    /**
     * Sets result of the {@link SequencedFuture} with the sequence id. Specified future will be
     * removed from the manager.
     *
     * @param seq sequence number to find future
     * @param result result to set
     */
    public <T> void setFutureResult(int seq, T result) {
        synchronized (mLock) {
            SequencedFuture future = mSeqToFutureMap.remove(seq);
            if (future != null) {
                if (result == null
                        || future.getResultWhenClosed().getClass() == result.getClass()) {
                    future.set(result);
                } else {
                    Log.w(TAG, "Type mismatch, expected "
                            + future.getResultWhenClosed().getClass()
                            + ", but was " + result.getClass());
                }
            } else {
                if (DEBUG) {
                    // Note: May not be an error if the caller doesn't return ListenableFuture
                    //       e.g. MediaSession#broadcastCustomCommand
                    Log.d(TAG, "Unexpected sequence number, seq=" + seq,
                            new IllegalArgumentException());
                }
            }
        }
    }

    @Override
    public void close() {
        List<SequencedFuture> pendingResults = new ArrayList<>();
        synchronized (mLock) {
            pendingResults.addAll(mSeqToFutureMap.values());
            mSeqToFutureMap.clear();
        }
        for (SequencedFuture result: pendingResults) {
            result.set(result.getResultWhenClosed());
        }
    }

    static final class SequencedFuture<T> extends AbstractResolvableFuture<T> {
        private final int mSequenceNumber;
        private final T mResultWhenClosed;

        /**
         * Creates a new {@code ResolvableFuture} that can be completed or cancelled by a later
         * method call.
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        static <T> SequencedFuture<T> create(int seq, @NonNull T resultWhenClosed) {
            return new SequencedFuture<T>(seq, resultWhenClosed);
        }

        @Override
        public boolean set(@Nullable T value) {
            return super.set(value);
        }

        public int getSequenceNumber() {
            return mSequenceNumber;
        }

        public @NonNull T getResultWhenClosed() {
            return mResultWhenClosed;
        }

        private SequencedFuture(int seq, @NonNull T resultWhenClosed) {
            mSequenceNumber = seq;
            mResultWhenClosed = resultWhenClosed;
        }
    }
}
