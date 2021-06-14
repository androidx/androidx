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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.hardware.ICarHardwareHost;
import androidx.car.app.hardware.ICarHardwareResult;
import androidx.car.app.hardware.ICarHardwareResultTypes;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarHardwareHostDispatcherTest {

    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private ICarHardwareHost.Stub mMockCarHardwareHost;

    private HostDispatcher mHostDispatcher = new HostDispatcher();
    private CarHardwareHostDispatcher mCarHardwareHostDispatcher =
            new CarHardwareHostDispatcher(mHostDispatcher);
    private CarHardwareHostStub mCarHardwareHost = new CarHardwareHostStub();

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        when(mMockCarHost.getHost(CarContext.HARDWARE_SERVICE))
                .thenReturn(mCarHardwareHost.asBinder());
        mHostDispatcher.setCarHost(mMockCarHost);
    }

    @Test
    public void dispatchGetCarHardwareResult() throws BundlerException,
            RemoteException {
        Integer desiredResult = 5;
        Bundleable desiredBundleable = Bundleable.create(desiredResult);
        int desiredResultType = ICarHardwareResultTypes.TYPE_INFO_MODEL;
        Integer unsupportedResult = new Integer(-1);

        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        mCarHardwareHost.setResult(true, desiredBundleable, 3);

        mCarHardwareHostDispatcher.dispatchGetCarHardwareResult(desiredResultType, paramBundle,
                new ICarHardwareResult.Stub() {

                    @Override
                    public void onCarHardwareResult(int resultType, boolean isSupported,
                            Bundleable result, IBinder callback) throws RemoteException {
                        assertThat(resultType).isEqualTo(desiredResultType);
                        assertThat(isSupported).isTrue();
                        assertThat(result).isEqualTo(desiredBundleable);
                    }
                });
        verify(mMockCarHardwareHost).getCarHardwareResult(eq(desiredResultType),
                eq(paramBundle), any());

    }

    private class CarHardwareHostStub extends ICarHardwareHost.Stub {

        private boolean mIsSupported;
        @Nullable
        private Bundleable mResult;
        private int mCallbackTimes;
        private ICarHardwareResult mCallback;

        IOnDoneCallback.Stub mDoneCallback = new IOnDoneCallback.Stub() {
            @Override
            public void onSuccess(Bundleable response) throws RemoteException {

            }

            @Override
            public void onFailure(Bundleable failureResponse) throws RemoteException {

            }
        };

        public void setResult(boolean isSupported, Bundleable bundleable, int times) {
            mIsSupported = isSupported;
            mResult = bundleable;
            mCallbackTimes = times;
        }

        @Override
        public void getCarHardwareResult(int resultType, @Nullable Bundleable params,
                ICarHardwareResult callback) throws RemoteException {
            mCallback = callback;
            // Record the call in the mock
            mMockCarHardwareHost.getCarHardwareResult(resultType, params, callback);
            // Send the result back.
            for (int i = 0; i < mCallbackTimes; ++i) {
                callback.onCarHardwareResult(resultType, mIsSupported, mResult, mDoneCallback);
            }
        }

        @Override
        public void subscribeCarHardwareResult(int resultType, @Nullable Bundleable params,
                ICarHardwareResult callback) throws RemoteException {
        }

        @Override
        public void unsubscribeCarHardwareResult(int resultType, @Nullable Bundleable params)
                throws RemoteException {
        }
    }
}
