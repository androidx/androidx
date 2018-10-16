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
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.AbstractResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages {@link SequencedFuture} that contains sequence number to be shared across the process.
 */
@TargetApi(Build.VERSION_CODES.P)
class SequencedFutureManager<T> implements AutoCloseable {
    private static final String TAG = "SequencedFutureManager";
    private final Object mLock = new Object();
    private final T mResultWhenClosed;

    @GuardedBy("mLock")
    private int mNextSequenceNumber;
    @GuardedBy("mLock")
    private ArrayMap<Integer, SequencedFuture> mSeqToFutureMap = new ArrayMap<>();

    /**
     * Constructor with the value to be used for pending result when closed.
     *
     * @param resultWhenClosed
     */
    SequencedFutureManager(T resultWhenClosed) {
        mResultWhenClosed = resultWhenClosed;
    }

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
    public SequencedFuture<T> createSequencedFuture() {
        final SequencedFuture<T> result;
        final int seq;
        synchronized (mLock) {
            seq = obtainNextSequenceNumber();
            result = SequencedFuture.create(seq);
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
    public void setFutureResult(int seq, T result) {
        synchronized (mLock) {
            SequencedFuture future = mSeqToFutureMap.remove(seq);
            if (future != null) {
                future.set(result);
            } else {
                Log.w(TAG, "Unexpected sequence number, seq=" + seq,
                        new IllegalArgumentException());
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
            result.set(mResultWhenClosed);
        }
    }

    static final class SequencedFuture<T> extends AbstractResolvableFuture<T> {
        private final int mSequenceNumber;

        /**
         * Creates a new {@code ResolvableFuture} that can be completed or cancelled by a later
         * method call.
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        static <T> SequencedFuture<T> create(int seq) {
            return new SequencedFuture<T>(seq);
        }

        @Override
        public boolean set(T value) {
            return super.set(value);
        }

        public int getSequenceNumber() {
            return mSequenceNumber;
        }

        private SequencedFuture(int seq) {
            mSequenceNumber = seq;
        }
    }
}
