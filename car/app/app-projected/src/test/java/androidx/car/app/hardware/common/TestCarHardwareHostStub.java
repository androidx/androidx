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

import android.os.RemoteException;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.hardware.ICarHardwareHost;
import androidx.car.app.hardware.ICarHardwareResult;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.jspecify.annotations.Nullable;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake {@link ICarHardwareHost} for testing.
 *
 * <p>This class adds the following conveniences:
 * <ul>
 *     <li>captures calls to {@link ICarHardwareHost#getCarHardwareResult} and
 *     {@link ICarHardwareHost#subscribeCarHardwareResult}
 *     <li>invokes the listener with provided data a fixed number of times
 *     <li>optional mock for additional parameter checking
 * </ul>
 */
public class TestCarHardwareHostStub extends ICarHardwareHost.Stub {

    // Map of result type -> parameter -> callback.
    private final Map<Integer, Map<Object, ICarHardwareResult>> mCallbackMap = new HashMap<>();
    private final @Nullable ICarHardwareHost mMockCarHardwareHost;

    IOnDoneCallback.Stub mDoneCallback = new IOnDoneCallback.Stub() {
        @Override
        public void onSuccess(Bundleable response) throws RemoteException {

        }

        @Override
        public void onFailure(Bundleable failureResponse) throws RemoteException {

        }
    };

    /** Creates an instance with optional underlying mock. */
    public TestCarHardwareHostStub(@Nullable ICarHardwareHost mockCarHardwareHost) {
        mMockCarHardwareHost = mockCarHardwareHost;
    }

    /**
     * Looks up the result callback based on the result type and param and if found calls it
     * with the data provided.
     *
     * @return {@code true} if the callback was found and {@code onCarHardwareResult} was called
     */
    public boolean sendResult(int resultType, Object params, boolean isSupported,
            Bundleable result, int times) throws RemoteException {
        Map<Object, ICarHardwareResult> paramMap = mCallbackMap.get(resultType);
        if (paramMap == null) {
            return false;
        }
        ICarHardwareResult callback = paramMap.get(params);
        if (callback == null) {
            return false;
        }
        for (int i = 0; i < times; ++i) {
            callback.onCarHardwareResult(resultType, isSupported, result, mDoneCallback);
        }
        return true;
    }


    @Override
    public void getCarHardwareResult(int resultType, @Nullable Bundleable param,
            ICarHardwareResult callback) throws RemoteException {
        Map<Object, ICarHardwareResult> paramMap = mCallbackMap.get(resultType);
        if (paramMap == null) {
            paramMap = new HashMap<>();
            mCallbackMap.put(resultType, paramMap);
        }
        // Need to try/catch the BundlerException because function signature is overidden.
        try {
            paramMap.put(param.get(), callback);
            // Record the call in the mock
            if (mMockCarHardwareHost != null) {
                mMockCarHardwareHost.getCarHardwareResult(resultType, param, callback);
            }
        } catch (BundlerException e) {
            Assert.fail("Bundler exception");
        }
    }

    @Override
    public void subscribeCarHardwareResult(int resultType, @Nullable Bundleable param,
            ICarHardwareResult callback) throws RemoteException {
        Map<Object, ICarHardwareResult> paramMap = mCallbackMap.get(resultType);
        if (paramMap == null) {
            paramMap = new HashMap<>();
            mCallbackMap.put(resultType, paramMap);
        }
        // Need to try/catch the BundlerException because function signature is overidden.
        try {
            paramMap.put(param.get(), callback);
            // Record the call in the mock
            if (mMockCarHardwareHost != null) {
                mMockCarHardwareHost.subscribeCarHardwareResult(resultType, param, callback);
            }
        } catch (BundlerException e) {
            Assert.fail("Bundler exception");
        }
    }

    @Override
    public void unsubscribeCarHardwareResult(int resultType, @Nullable Bundleable param)
            throws RemoteException {
        if (mMockCarHardwareHost != null) {
            mMockCarHardwareHost.unsubscribeCarHardwareResult(resultType, param);
        }
        Map<Object, ICarHardwareResult> paramMap = mCallbackMap.get(resultType);
        if (paramMap == null) {
            return;
        }
        // Need to try/catch the BundlerException because function signature is overidden.
        try {
            paramMap.remove(param.get());
        } catch (BundlerException e) {
            Assert.fail("Bundler exception");
        }
        if (!paramMap.isEmpty()) {
            return;
        }
        mCallbackMap.remove(resultType);
    }
}
