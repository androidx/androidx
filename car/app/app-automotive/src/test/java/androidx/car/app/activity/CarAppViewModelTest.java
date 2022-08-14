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

import static androidx.car.app.SessionInfo.DEFAULT_SESSION_INFO;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;

import androidx.car.app.HandshakeInfo;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IInsetsListener;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link CarAppViewModel} */
@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = {"androidx.car.app.activity"})
@DoNotInstrument
public class CarAppViewModelTest {
    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName(
            ApplicationProvider.getApplicationContext(), "Class1");
    private static final int TEST_DISPLAY_ID = 123;
    private static final Intent TEST_INTENT = new Intent("TestAction");

    private final Application mApplication = ApplicationProvider.getApplicationContext();
    private CarAppViewModel mCarAppViewModel;
    private final CarAppActivity mCarAppActivity = mock(CarAppActivity.class);
    private ICarAppActivity mICarAppActivity;
    private ShadowLooper mMainLooper;
    private final ServiceConnectionManager mServiceConnectionManager =
            mock(ServiceConnectionManager.class);
    private final IInsetsListener mIInsetsListener = mock(IInsetsListener.class);
    private final ServiceDispatcher mServiceDispatcher = mock(ServiceDispatcher.class);

    @Before
    public void setUp() {
        mCarAppViewModel = new CarAppViewModel(mApplication, TEST_COMPONENT_NAME,
                DEFAULT_SESSION_INFO);
        mCarAppViewModel.setActivity(mCarAppActivity);
        mICarAppActivity = mock(ICarAppActivity.class);
        mMainLooper = shadowOf(mApplication.getMainLooper());
    }

    @Test
    public void constructor() {
        assertThat(mCarAppViewModel.getServiceConnectionManager()).isNotNull();
        assertThat(mCarAppViewModel.getServiceConnectionManager().getServiceComponentName())
                .isEqualTo(TEST_COMPONENT_NAME);
        assertThat(mCarAppViewModel.getServiceDispatcher()).isNotNull();
        assertThat(mCarAppViewModel.getError().getValue()).isNull();
        assertThat(mCarAppViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.IDLE);
    }

    @Test
    public void bind_startsConnection() {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        mCarAppViewModel.bind(TEST_INTENT, mICarAppActivity, TEST_DISPLAY_ID);

        verify(mServiceConnectionManager).bind(TEST_INTENT, mICarAppActivity, TEST_DISPLAY_ID);

        mMainLooper.idle();

        assertThat(mCarAppViewModel.getState().getValue())
                .isEqualTo(CarAppViewModel.State.CONNECTING);
    }

    @Test
    public void unbind_disconnects() {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        mCarAppViewModel.unbind();

        verify(mServiceConnectionManager).unbind();

        mMainLooper.idle();

        assertThat(mCarAppViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.IDLE);
    }

    @Test
    public void onError_unbinds() {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        mCarAppViewModel.onError(ErrorHandler.ErrorType.HOST_ERROR);

        verify(mServiceConnectionManager).unbind();

        mMainLooper.idle();

        assertThat(mCarAppViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.ERROR);
        assertThat(mCarAppViewModel.getError().getValue())
                .isEqualTo(ErrorHandler.ErrorType.HOST_ERROR);
    }

    @Test
    public void onConnect_clearsError() {
        mCarAppViewModel.onConnect();

        mMainLooper.idle();

        assertThat(mCarAppViewModel.getState().getValue())
                .isEqualTo(CarAppViewModel.State.CONNECTED);
        assertThat(mCarAppViewModel.getError().getValue()).isNull();
    }

    @Test
    public void retryBind_clearsErrorAndBinds() {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        mCarAppViewModel.retryBinding();

        verify(mCarAppActivity).recreate();

        mMainLooper.idle();

        assertThat(mCarAppViewModel.getState().getValue())
                .isEqualTo(CarAppViewModel.State.IDLE);
        assertThat(mCarAppViewModel.getError().getValue()).isNull();
    }

    @Test
    public void dispatchInsetsUpdates_whenApiLevel5Above_shouldCallWindowsInsetsChanged() throws
            RemoteException {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        when(mServiceConnectionManager.getHandshakeInfo()).thenReturn(createHandshakeInfo("TestApp",
                5));
        when(mServiceConnectionManager.getServiceDispatcher()).thenReturn(mServiceDispatcher);
        mCarAppViewModel.setInsetsListener(mIInsetsListener);
        verify(mServiceDispatcher).dispatch(eq("onWindowInsetsChanged"), any());
    }

    @Test
    public void dispatchInsetsUpdates_whenApiLevelBelow5_shouldCallInsetsChanges() throws
            RemoteException {
        mCarAppViewModel.setServiceConnectionManager(mServiceConnectionManager);
        when(mServiceConnectionManager.getHandshakeInfo()).thenReturn(
                createHandshakeInfo("TestApp", 4));
        when(mServiceConnectionManager.getServiceDispatcher()).thenReturn(mServiceDispatcher);
        mCarAppViewModel.setInsetsListener(mIInsetsListener);
        verify(mServiceDispatcher).dispatch(eq("onInsetsChanged"), any());
    }

    private static HandshakeInfo createHandshakeInfo(String packageName, int hostApiLevel) {
        return new HandshakeInfo(packageName, hostApiLevel);
    }
}
