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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.hardware.ICarHardwareHost;
import androidx.car.app.hardware.ICarHardwareResultTypes;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarResultStubMapTest {

    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private ICarHardwareHost.Stub mMockCarHardwareHost;
    private TestCarHardwareHostStub mCarHardwareHost;
    private HostDispatcher mHostDispatcher = new HostDispatcher();
    private CarHardwareHostDispatcher mCarHardwareHostDispatcher =
            new CarHardwareHostDispatcher(mHostDispatcher);

    private final Executor mExecutor = command -> command.run();
    @Captor
    private ArgumentCaptor<Bundleable> mSubscribeParamsCaptor;

    @Mock
    OnCarDataAvailableListener<Integer> mMockCarDataListener1;
    @Mock
    OnCarDataAvailableListener<Integer> mMockCarDataListener2;
    @Mock
    OnCarDataAvailableListener<Integer> mMockCarDataListener3;
    @Mock
    OnCarDataAvailableListener<Integer> mMockCarDataListener4;

    @Mock
    OnCarDataAvailableListener<String> mMockCarDataStringListener;

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
    public void addListener_single_listener_single_param() throws BundlerException,
            RemoteException {
        Integer desiredResult = 5;
        Bundleable desiredBundleable = Bundleable.create(desiredResult);
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;
        Integer unsupportedResult = new Integer(-1);
        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        CarResultStubMap<Integer, String> carResultStubMap =
                new CarResultStubMap<>(desiredResultType,
                        unsupportedResult,
                        mCarHardwareHostDispatcher);

        carResultStubMap.addListener(param, mExecutor, mMockCarDataListener1);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        verify(mMockCarDataListener1, times(3)).onCarDataAvailable(eq(desiredResult));
        verify(mMockCarHardwareHost).subscribeCarHardwareResult(eq(desiredResultType),
                mSubscribeParamsCaptor.capture(), any());
        String capturedParam = (String) mSubscribeParamsCaptor.getValue().get();
        assertThat(capturedParam).isEqualTo(param);

        carResultStubMap.removeListener(mMockCarDataListener1);
        verify(mMockCarHardwareHost).unsubscribeCarHardwareResult(eq(desiredResultType),
                mSubscribeParamsCaptor.capture());
        assertThat(capturedParam).isEqualTo(param);
    }

    @Test
    public void addListener_multiple_listener_single_param() throws BundlerException,
            RemoteException {
        Integer desiredResult = 5;
        Bundleable desiredBundleable = Bundleable.create(desiredResult);
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;
        Integer unsupportedResult = new Integer(-1);
        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        CarResultStubMap<Integer, String> carResultStubMap = new CarResultStubMap<>(
                desiredResultType,
                unsupportedResult,
                mCarHardwareHostDispatcher);

        carResultStubMap.addListener(param, mExecutor, mMockCarDataListener1);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        carResultStubMap.addListener(param, mExecutor, mMockCarDataListener2);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        carResultStubMap.addListener(param, mExecutor, mMockCarDataListener3);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        carResultStubMap.removeListener(mMockCarDataListener3);
        carResultStubMap.addListener(param, mExecutor, mMockCarDataListener4);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        carResultStubMap.removeListener(mMockCarDataListener1);
        carResultStubMap.removeListener(mMockCarDataListener2);
        carResultStubMap.removeListener(mMockCarDataListener4);
        verify(mMockCarDataListener1, times(12)).onCarDataAvailable(eq(desiredResult));
        verify(mMockCarDataListener2, times(9)).onCarDataAvailable(eq(desiredResult));
        verify(mMockCarDataListener3, times(3)).onCarDataAvailable(eq(desiredResult));
        verify(mMockCarDataListener4, times(3)).onCarDataAvailable(eq(desiredResult));

        verify(mMockCarHardwareHost).subscribeCarHardwareResult(eq(desiredResultType),
                mSubscribeParamsCaptor.capture(), any());
        String capturedParam = (String) mSubscribeParamsCaptor.getValue().get();
        assertThat(capturedParam).isEqualTo(param);
        verify(mMockCarHardwareHost).unsubscribeCarHardwareResult(eq(desiredResultType),
                mSubscribeParamsCaptor.capture());
        assertThat(capturedParam).isEqualTo(param);
    }

    @Test
    public void addListener_single_listener_multiple_param() throws BundlerException,
            RemoteException {
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;
        String unsupportedResult = "Unsupported";
        String param1 = "param1";
        String param2 = "param2";
        String param3 = "param3";
        String param4 = "param4";

        CarResultStubMap<String, String> carResultStubMap =
                new CarResultStubMap<>(desiredResultType,
                        unsupportedResult,
                        mCarHardwareHostDispatcher);
        carResultStubMap.addListener(param1, mExecutor, mMockCarDataStringListener);
        carResultStubMap.addListener(param2, mExecutor, mMockCarDataStringListener);
        carResultStubMap.addListener(param3, mExecutor, mMockCarDataStringListener);
        carResultStubMap.addListener(param4, mExecutor, mMockCarDataStringListener);
        mCarHardwareHost.sendResult(desiredResultType, param1, true, Bundleable.create(param1), 1);
        mCarHardwareHost.sendResult(desiredResultType, param2, true, Bundleable.create(param2), 2);
        mCarHardwareHost.sendResult(desiredResultType, param3, true, Bundleable.create(param3), 3);
        mCarHardwareHost.sendResult(desiredResultType, param4, true, Bundleable.create(param4), 4);
        carResultStubMap.removeListener(mMockCarDataStringListener);
        mCarHardwareHost.sendResult(desiredResultType, param1, true, Bundleable.create(param1), 1);
        mCarHardwareHost.sendResult(desiredResultType, param2, true, Bundleable.create(param2), 2);
        mCarHardwareHost.sendResult(desiredResultType, param3, true, Bundleable.create(param3), 3);
        mCarHardwareHost.sendResult(desiredResultType, param4, true, Bundleable.create(param4), 4);
        verify(mMockCarDataStringListener, times(1)).onCarDataAvailable(eq(param1));
        verify(mMockCarDataStringListener, times(2)).onCarDataAvailable(eq(param2));
        verify(mMockCarDataStringListener, times(3)).onCarDataAvailable(eq(param3));
        verify(mMockCarDataStringListener, times(4)).onCarDataAvailable(eq(param4));
    }

    @Test
    public void addListener_multiple_listener_multiple_param() throws BundlerException,
            RemoteException {
        int desiredResultType = ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER;
        Integer unsupportedResult = new Integer(-1);

        CarResultStubMap<Integer, Integer> carResultStubMap = new CarResultStubMap<>(
                desiredResultType,
                unsupportedResult,
                mCarHardwareHostDispatcher);
        mCarHardwareHost.sendResult(desiredResultType, 4, true, Bundleable.create(40), 4);
        carResultStubMap.addListener(1, mExecutor, mMockCarDataListener1);
        carResultStubMap.addListener(2, mExecutor, mMockCarDataListener2);
        mCarHardwareHost.sendResult(desiredResultType, 1, true, Bundleable.create(10), 1);
        carResultStubMap.addListener(3, mExecutor, mMockCarDataListener3);
        carResultStubMap.addListener(4, mExecutor, mMockCarDataListener4);
        mCarHardwareHost.sendResult(desiredResultType, 2, true, Bundleable.create(20), 2);
        carResultStubMap.removeListener(mMockCarDataListener1);
        mCarHardwareHost.sendResult(desiredResultType, 3, true, Bundleable.create(30), 3);
        carResultStubMap.removeListener(mMockCarDataListener2);
        mCarHardwareHost.sendResult(desiredResultType, 4, true, Bundleable.create(40), 4);
        carResultStubMap.removeListener(mMockCarDataListener3);
        carResultStubMap.removeListener(mMockCarDataListener4);
        mCarHardwareHost.sendResult(desiredResultType, 1, true, Bundleable.create(10), 1);
        mCarHardwareHost.sendResult(desiredResultType, 2, true, Bundleable.create(20), 2);
        mCarHardwareHost.sendResult(desiredResultType, 3, true, Bundleable.create(30), 3);
        mCarHardwareHost.sendResult(desiredResultType, 4, true, Bundleable.create(40), 4);
        verify(mMockCarDataListener1, times(1)).onCarDataAvailable(eq(10));
        verify(mMockCarDataListener2, times(2)).onCarDataAvailable(eq(20));
        verify(mMockCarDataListener3, times(3)).onCarDataAvailable(eq(30));
        verify(mMockCarDataListener4, times(4)).onCarDataAvailable(eq(40));

    }
}
