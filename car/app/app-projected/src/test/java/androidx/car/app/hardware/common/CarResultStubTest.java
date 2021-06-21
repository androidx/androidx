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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarResultStubTest {

    @Mock
    private ICarHost mMockCarHost;

    @Mock private ICarHardwareHost.Stub mMockCarHardwareHost;

    private HostDispatcher mHostDispatcher = new HostDispatcher();
    private CarHardwareHostDispatcher mCarHardwareHostDispatcher =
            new CarHardwareHostDispatcher(mHostDispatcher);
    private TestCarHardwareHostStub mCarHardwareHost;

    private final Executor mExecutor = command -> command.run();
    @Mock OnCarDataListener<Integer> mMockCarDataListener;

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
    public void addListener_callHost_returnsValue_singleShot() throws BundlerException,
            RemoteException {
        Integer desiredResult = 5;
        Bundleable desiredBundleable = Bundleable.create(desiredResult);
        int desiredResultType = ICarHardwareResultTypes.TYPE_INFO_MODEL;
        Integer unsupportedResult = new Integer(-1);

        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        CarResultStub<Integer> carResultStub =
                new CarResultStub<Integer>(desiredResultType, paramBundle,
                        true, unsupportedResult,
                        mCarHardwareHostDispatcher);

        carResultStub.addListener(mExecutor, mMockCarDataListener);
        mCarHardwareHost.sendResult(desiredResultType, param, true, desiredBundleable, 3);
        verify(mMockCarDataListener, times(1)).onCarData(eq(desiredResult));
        verify(mMockCarHardwareHost).getCarHardwareResult(eq(desiredResultType),
                eq(paramBundle), any());
    }

    @Test
    public void addListener_callHost_unsupported_singleShot() throws BundlerException,
            RemoteException {
        int desiredResultType = ICarHardwareResultTypes.TYPE_INFO_MODEL;
        Integer unsupportedResult = new Integer(-1);

        String param = "param";
        Bundleable paramBundle = Bundleable.create(param);

        CarResultStub<Integer> carResultStub =
                new CarResultStub<Integer>(desiredResultType, paramBundle,
                        true, unsupportedResult,
                        mCarHardwareHostDispatcher);

        carResultStub.addListener(mExecutor, mMockCarDataListener);
        mCarHardwareHost.sendResult(desiredResultType, param, false, null, 3);
        verify(mMockCarDataListener, times(1)).onCarData(eq(unsupportedResult));
        verify(mMockCarHardwareHost).getCarHardwareResult(eq(desiredResultType),
                eq(paramBundle), any());
    }
}
