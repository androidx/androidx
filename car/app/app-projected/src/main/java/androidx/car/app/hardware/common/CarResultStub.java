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
package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.hardware.ICarHardwareResult;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.RemoteUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Convenience class wrapping all the calls to car hardware host
 *
 * @param <T> represents the result data type which this stub is returning
 *
 */
@RestrictTo(LIBRARY_GROUP)
public class CarResultStub<T> extends ICarHardwareResult.Stub {

    private final CarHardwareHostDispatcher mHostDispatcher;
    private final int mResultType;
    private final @Nullable Bundleable mBundle;
    private final boolean mIsSingleShot;
    private final Map<OnCarDataAvailableListener<T>, Executor> mListeners = new HashMap<>();
    private final T mUnsupportedValue;

    /**
     * Creates an instance of the result stub.
     *
     * @param resultType the result type to fetch
     * @param bundle optional parameters
     * @param isSingleShot whether the result stub will return a single value or multiple values
     * @param unsupportedValue value to be returned if the host does not support the result type
     * @param hostDispatcher dispatcher to be used for host calls
     *
     * @throws NullPointerException if {@code unsupportedValue} is {@code null} or if
     *                              {@code hostDispatcher} is {@code null}
     */
    public CarResultStub(int resultType, @Nullable Bundleable bundle, boolean isSingleShot,
            @NonNull T unsupportedValue,
            @NonNull CarHardwareHostDispatcher hostDispatcher) {
        mHostDispatcher = requireNonNull(hostDispatcher);
        mResultType = resultType;
        mBundle = bundle;
        mIsSingleShot = isSingleShot;
        mUnsupportedValue = requireNonNull(unsupportedValue);
    }

    /**
     * Adds a listener for the given result type and replaces any previous parameter.
     *
     * <p>This call also kicks off the initial call to the host if needed. If the
     * {@code listener} was added previously then the executor is updated.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener listener for the results
     *
     * @throws NullPointerException if {@code executor} is {@code null} or if {@code listener} is
     *                              {@code null}
     */
    public void addListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<T> listener) {
        boolean alreadySubscribedToHost = !mListeners.isEmpty();
        mListeners.put(requireNonNull(listener), executor);

        if (alreadySubscribedToHost) {
            return;
        }
        if (mIsSingleShot) {
            mHostDispatcher.dispatchGetCarHardwareResult(mResultType, mBundle, this);
        } else {
            mHostDispatcher.dispatchSubscribeCarHardwareResult(mResultType, mBundle, this);
        }
    }

    /**
     * Removes a previously registered listener and returns {@code true} if there are no more
     * listeners attached to this stub.
     *
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public boolean removeListener(@NonNull OnCarDataAvailableListener<T> listener) {
        mListeners.remove(requireNonNull(listener));
        if (!mListeners.isEmpty()) {
            return false;
        }
        if (!mIsSingleShot) {
            mHostDispatcher.dispatchUnsubscribeCarHardwareResult(mResultType, mBundle);
        }
        return true;
    }

    @Override
    public void onCarHardwareResult(int resultType, boolean isSupported, @NonNull Bundleable result,
            @NonNull IBinder callback) throws RemoteException {
        IOnDoneCallback doneCallback = IOnDoneCallback.Stub.asInterface(callback);
        RemoteUtils.dispatchCallFromHost(doneCallback, "onCarHardwareResult",
                () -> {
                    notifyResults(isSupported, result);
                    return null;
                });
    }

    private void notifyResults(boolean isSupported, @NonNull Bundleable result)
            throws BundlerException {
        T data = isSupported ? convertAndRecast(result) : mUnsupportedValue;
        for (Map.Entry<OnCarDataAvailableListener<T>, Executor> entry: mListeners.entrySet()) {
            entry.getValue().execute(() -> entry.getKey().onCarDataAvailable(data));
        }
        if (mIsSingleShot) {
            mListeners.clear();
        }
    }

    @SuppressWarnings({"unchecked", "cast.unsafe"}) // Cannot check if instanceof T
    private T convertAndRecast(@NonNull Bundleable bundleable) throws BundlerException {
        Object object = bundleable.get();
        T data;
        try {
            data = (T) object;
        } catch (ClassCastException e) {
            throw new BundlerException("Incorrect type unbundled", e);
        }
        return data;
    }
};
