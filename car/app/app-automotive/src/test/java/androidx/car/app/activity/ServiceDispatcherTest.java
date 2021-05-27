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

package androidx.car.app.activity;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;

import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.serialization.BundlerException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ServiceDispatcher} */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ServiceDispatcherTest {
    private ErrorHandler mErrorHandler;
    private ServiceDispatcher mServiceDispatcher;

    @Before
    public void setup() {
        mErrorHandler = mock(ErrorHandler.class);
        mServiceDispatcher = new ServiceDispatcher(mErrorHandler, () -> false);
    }

    @Test
    public void dispatch_serviceBound_invoked() throws RemoteException {
        IRendererService rendererService = mock(IRendererService.class);
        Intent intent = mock(Intent.class);
        ComponentName componentName = mock(ComponentName.class);


        ServiceDispatcher.OneWayCall call = () -> rendererService.onNewIntent(intent,
                componentName, 0);

        mServiceDispatcher.setOnBindingListener(() -> true);
        mServiceDispatcher.dispatch(call);

        verify(rendererService, times(1)).onNewIntent(intent, componentName, 0);
    }

    @Test
    public void dispatch_serviceNotBound_notInvoked() throws BundlerException, RemoteException {
        ServiceDispatcher.OneWayCall call = mock(ServiceDispatcher.OneWayCall.class);

        mServiceDispatcher.setOnBindingListener(() -> false);
        mServiceDispatcher.dispatch(call);

        verify(call, never()).invoke();
    }

    @Test
    public void dispatch_serviceThrowsError_errorHandlerInvoked() {
        ServiceDispatcher.OneWayCall call = () -> {
            throw new RemoteException();
        };

        mServiceDispatcher.setOnBindingListener(() -> true);
        mServiceDispatcher.dispatch(call);

        verify(mErrorHandler, times(1))
                .onError(eq(ErrorHandler.ErrorType.HOST_ERROR), any());
    }

    @Test
    public void fetch_serviceBound_valueReturned() {
        ServiceDispatcher.ReturnCall<Integer> call = () -> 123;

        mServiceDispatcher.setOnBindingListener(() -> true);
        Integer result = mServiceDispatcher.fetch(234, call);

        assertThat(result).isEqualTo(123);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fetch_serviceNotBound_notInvokedAndReturnFallback()
            throws BundlerException, RemoteException {
        ServiceDispatcher.ReturnCall<Integer> call = mock(ServiceDispatcher.ReturnCall.class);

        mServiceDispatcher.setOnBindingListener(() -> false);
        Integer result = mServiceDispatcher.fetch(234, call);

        verify(call, never()).invoke();
        assertThat(result).isEqualTo(234);
    }

    @Test
    public void fetch_serviceThrowsError_errorHandlerInvokedAndReturnFallback() {
        ServiceDispatcher.ReturnCall<Integer> call = () -> {
            throw new RemoteException();
        };

        mServiceDispatcher.setOnBindingListener(() -> true);
        Integer result = mServiceDispatcher.fetch(234, call);

        verify(mErrorHandler, times(1))
                .onError(eq(ErrorHandler.ErrorType.HOST_ERROR), any());
        assertThat(result).isEqualTo(234);
    }
}
