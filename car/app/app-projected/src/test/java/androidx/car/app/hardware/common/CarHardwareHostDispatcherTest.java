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

import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
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
    private TestCarHardwareHostStub mCarHardwareHost;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        // Perform after mocks initialized.
        mCarHardwareHost = new TestCarHardwareHostStub(mMockCarHardwareHost);

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

        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

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

    @Test
    public void dispatchSubscribeCarHardwareResult() throws BundlerException, RemoteException {
        Integer desiredResult = 5;
        Bundleable desiredBundleable = Bundleable.create(desiredResult);
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;

        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        mCarHardwareHostDispatcher.dispatchSubscribeCarHardwareResult(desiredResultType,
                paramBundle,
                new ICarHardwareResult.Stub() {

                    @Override
                    public void onCarHardwareResult(int resultType, boolean isSupported,
                            Bundleable result, IBinder callback) throws RemoteException {
                        assertThat(resultType).isEqualTo(desiredResultType);
                        assertThat(isSupported).isTrue();
                        assertThat(result).isEqualTo(desiredBundleable);
                    }
                });
        verify(mMockCarHardwareHost).subscribeCarHardwareResult(eq(desiredResultType),
                eq(paramBundle), any());
    }

    @Test
    public void dispatchUnsubscribeCarHardwareResult() throws RemoteException, BundlerException {
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;
        Bundleable bundle = Bundleable.create(new Integer(10));
        mCarHardwareHostDispatcher.dispatchUnsubscribeCarHardwareResult(desiredResultType, bundle);
        verify(mMockCarHardwareHost).unsubscribeCarHardwareResult(eq(desiredResultType),
                eq(bundle));
    }
}
