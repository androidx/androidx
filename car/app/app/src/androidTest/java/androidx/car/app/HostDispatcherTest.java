/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.car.app.navigation.INavigationHost;
import androidx.car.app.serialization.Bundleable;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link HostDispatcher}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class HostDispatcherTest {

    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IAppHost.Stub mMockAppHost;

    private IAppHost mAppHost;
    private INavigationHost mNavigationHost;

    private HostDispatcher mHostDispatcher = new HostDispatcher();

    @Before
    @UiThreadTest
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mAppHost =
                new IAppHost.Stub() {
                    @Override
                    public void invalidate() throws RemoteException {
                        mMockAppHost.invalidate();
                    }

                    @Override
                    public void showToast(CharSequence text, int duration) throws
                            RemoteException {
                        mMockAppHost.showToast(text, duration);
                    }

                    @Override
                    public void setSurfaceListener(@Nullable ISurfaceListener surfaceListener)
                            throws RemoteException {
                        mMockAppHost.setSurfaceListener(surfaceListener);
                    }
                };

        mNavigationHost = new INavigationHost.Stub() {
            @Override
            public void updateTrip(Bundleable trip) {
            }

            @Override
            public void navigationStarted() {
            }

            @Override
            public void navigationEnded() {
            }
        };

        when(mMockCarHost.getHost(CarContext.APP_SERVICE)).thenReturn(mAppHost.asBinder());
        when(mMockCarHost.getHost(CarContext.NAVIGATION_SERVICE)).thenReturn(
                mNavigationHost.asBinder());

        mHostDispatcher.setCarHost(mMockCarHost);
    }

    @Test
    public void dispatch_callGoesToProperRemoteService() throws RemoteException {
        mHostDispatcher.dispatch(CarContext.APP_SERVICE,
                (IAppHost service) -> {
                    service.invalidate();
                    return null;
                },
                "test");

        verify(mMockAppHost).invalidate();
    }

    @Test
    public void dispatch_callThrowsSecurityException_throwsSecurityException() {
        assertThrows(
                SecurityException.class,
                () -> mHostDispatcher.dispatch(
                        CarContext.APP_SERVICE,
                        (IAppHost service) -> {
                            throw new SecurityException();
                        },
                        "test"));
    }

    @Test
    public void dispatch_callThrowsRemoteException_throwsHostException() {
        assertThrows(
                HostException.class,
                () -> mHostDispatcher.dispatch(
                        CarContext.APP_SERVICE,
                        (IAppHost service) -> {
                            throw new RemoteException();
                        },
                        "test"));
    }

    @Test
    public void dispatch_callThrowsRuntimeException_throwsHostException() {
        assertThrows(
                HostException.class,
                () -> mHostDispatcher.dispatch(
                        CarContext.APP_SERVICE,
                        (IAppHost service) -> {
                            throw new IllegalStateException();
                        },
                        "test"));
    }

    @Test
    @UiThreadTest
    public void getHost_afterResetting_getsFromCarHost() throws RemoteException {
        assertThat(mHostDispatcher.getHost(CarContext.APP_SERVICE)).isEqualTo(mAppHost);

        mHostDispatcher.resetHosts();

        doThrow(new IllegalStateException()).when(mMockCarHost).getHost(any());

        assertThrows(HostException.class, () -> mHostDispatcher.getHost(CarContext.APP_SERVICE));
    }

    @Test
    public void getHost_returnsCached() throws RemoteException {
        IAppHost hostService = (IAppHost) mHostDispatcher.getHost(CarContext.APP_SERVICE);

        doThrow(new IllegalStateException()).when(mMockCarHost).getHost(any());

        assertThat(mHostDispatcher.getHost(CarContext.APP_SERVICE)).isEqualTo(hostService);
    }

    @Test
    public void getHost_appHost_returnsProperHostService() {
        assertThat(mHostDispatcher.getHost(CarContext.APP_SERVICE)).isEqualTo(mAppHost);
    }

    @Test
    public void getHost_appHost_hostThrowsRemoteException_throwsHostException()
            throws RemoteException {
        when(mMockCarHost.getHost(any())).thenThrow(new RemoteException());
        assertThrows(HostException.class, () -> mHostDispatcher.getHost(CarContext.APP_SERVICE));
    }

    @Test
    public void getHost_appHost_hostThrowsRuntimeException_throwsHostException()
            throws RemoteException {
        when(mMockCarHost.getHost(any())).thenThrow(new IllegalStateException());
        assertThrows(HostException.class, () -> mHostDispatcher.getHost(CarContext.APP_SERVICE));
    }

    @Test
    public void getHost_navigationHost_returnsProperHostService() {
        assertThat(mHostDispatcher.getHost(CarContext.NAVIGATION_SERVICE)).isEqualTo(
                mNavigationHost);
    }

    @Test
    public void getHost_navigationHost_hostThrowsRemoteException_throwsHostException()
            throws RemoteException {
        when(mMockCarHost.getHost(any())).thenThrow(new RemoteException());
        assertThrows(HostException.class,
                () -> mHostDispatcher.getHost(CarContext.NAVIGATION_SERVICE));
    }

    @Test
    public void getHost_navigationHost_hostThrowsRuntimeException_throwsHostException()
            throws RemoteException {
        when(mMockCarHost.getHost(any())).thenThrow(new IllegalStateException());
        assertThrows(HostException.class,
                () -> mHostDispatcher.getHost(CarContext.NAVIGATION_SERVICE));
    }

    @Test
    @UiThreadTest
    public void getHost_afterReset_throwsHostException() {
        mHostDispatcher.resetHosts();

        assertThrows(HostException.class, () -> mHostDispatcher.getHost(CarContext.APP_SERVICE));
    }

    @Test
    @UiThreadTest
    public void getHost_notBound_throwsHostException() {
        mHostDispatcher = new HostDispatcher();

        assertThrows(HostException.class, () -> mHostDispatcher.getHost(CarContext.APP_SERVICE));
    }
}
