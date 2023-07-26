/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.mocks;

import static junit.framework.TestCase.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Observable;
import androidx.camera.testing.impl.mocks.helpers.CallTimes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementation of {@link Observable.Observer} to verify proper invocations in tests.
 *
 * @param <T> the parameter type to be observed
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MockObserver<T> implements Observable.Observer<T> {
    private final Map<T, Integer> mNewDataCount = new HashMap<>();

    private CallTimes mCallTimes;
    private T mValueToVerify;
    private CountDownLatch mLatch;

    private boolean isVerified() {
        if (mCallTimes == null) {
            return false;
        }

        int count = mNewDataCount.get(mValueToVerify) == null
                ? 0 : mNewDataCount.get(mValueToVerify);
        return mCallTimes.isSatisfied(count);
    }

    /**
     * Verifies if {@link #onNewData} method was invoked properly during test.
     *
     * <p>{@code callTimes} defaults to {@code new CallTimes(1)} which ensures that
     * {@link #onNewData(Object)} method was invoked exactly once with given verification
     * parameters.
     *
     * @see #verifyOnNewDataCall(Object, long, CallTimes)
     */
    public void verifyOnNewDataCall(@Nullable T value,
            long timeoutInMillis) {
        verifyOnNewDataCall(value, timeoutInMillis, new CallTimes(1));
    }

    /**
     * Verifies if {@link #onNewData} method was invoked properly during test.
     *
     * @param value the value to match with the value parameter of {@link #onNewData(Object)}
     * @param timeoutInMillis the time limit to wait for asynchronous operation after which
     *                       {@link junit.framework.AssertionFailedError} is thrown
     * @param callTimes the condition for how many times {@link #onNewData} method should be called
     */
    public void verifyOnNewDataCall(@Nullable T value,
            long timeoutInMillis, @NonNull CallTimes callTimes) {
        mValueToVerify = value;
        mCallTimes = callTimes;

        if (!isVerified()) {
            mLatch = new CountDownLatch(1);

            try {
                assertTrue("Test failed for a timeout of " + timeoutInMillis + " ms",
                        mLatch.await(timeoutInMillis, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertTrue("onNewData() called "
                            + mNewDataCount.get(value) + " time(s) with " + value,
                    isVerified());

            mLatch = null;
        }
    }

    @Override
    public void onNewData(@Nullable T value) {
        Integer prevCount = mNewDataCount.get(value);
        mNewDataCount.put(value, (prevCount == null ? 0 : prevCount) + 1);

        if (mLatch != null && isVerified()) {
            mLatch.countDown();
        }
    }

    @Override
    public void onError(@NonNull Throwable t) {}
}
