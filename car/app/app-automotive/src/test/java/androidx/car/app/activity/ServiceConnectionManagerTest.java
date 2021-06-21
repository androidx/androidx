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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link ServiceConnectionManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ServiceConnectionManagerTest {

    private static final int TEST_DISPLAY_ID = 123;
    private static final Intent TEST_INTENT = new Intent("Test");

    private final ComponentName mRendererComponent = new ComponentName(
            ApplicationProvider.getApplicationContext(), getClass().getName());

    private final String mFakeCarAppServiceClass = "com.fake.FakeCarAppService";
    private final ComponentName mFakeCarAppServiceComponent = new ComponentName(
            ApplicationProvider.getApplicationContext(), mFakeCarAppServiceClass);
    private final IRendererService mRenderService = mock(IRendererService.class);
    private final RenderServiceDelegate mRenderServiceDelegate =
            new RenderServiceDelegate(mRenderService);
    private final CarAppViewModel mViewModel =
            new CarAppViewModel(ApplicationProvider.getApplicationContext(),
                    mFakeCarAppServiceComponent);
    private final ServiceConnectionManager mServiceConnectionManager =
            mViewModel.getServiceConnectionManager();
    private final ShadowLooper mMainLooper = shadowOf(getMainLooper());

    private void setupCarAppActivityForTesting() {
        try {
            Application app = ApplicationProvider.getApplicationContext();

            PackageManager packageManager = app.getPackageManager();
            ShadowPackageManager spm = shadowOf(packageManager);

            // Register fake renderer service which will be simulated by {@code mRenderService}.
            spm.addServiceIfNotPresent(mRendererComponent);
            spm.addIntentFilterForService(mRendererComponent,
                    new IntentFilter(CarAppActivity.ACTION_RENDER));

            when(mRenderService.initialize(any(ICarAppActivity.class),
                    any(ComponentName.class),
                    anyInt())).thenReturn(true);
            when(mRenderService.onNewIntent(any(Intent.class), any(ComponentName.class),
                    anyInt())).thenReturn(true);

            ShadowApplication sa = shadowOf(app);
            sa.setComponentNameAndServiceForBindService(mRendererComponent, mRenderServiceDelegate);
        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void testIsBound_serviceNotNull_returnsTrue() {
        mServiceConnectionManager.setRendererService(mock(IRendererService.class));
        assertThat(mServiceConnectionManager.isBound()).isTrue();
    }

    @Test
    public void testIsBound_serviceIsNull_returnsFalse() {
        mServiceConnectionManager.setRendererService(null);
        assertThat(mServiceConnectionManager.isBound()).isFalse();
    }

    @Test
    public void testBind_unbound_bindsToRenderer() {
        setupCarAppActivityForTesting();
        ICarAppActivity iCarAppActivity = mock(ICarAppActivity.class);

        mServiceConnectionManager.bind(TEST_INTENT, iCarAppActivity, TEST_DISPLAY_ID);
        mMainLooper.idle();
        try {
            assertThat(mViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.CONNECTED);
            assertThat(mViewModel.getError().getValue()).isNull();
            verify(mRenderService).initialize(iCarAppActivity, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
            verify(mRenderService).onNewIntent(TEST_INTENT, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
        assertThat(mServiceConnectionManager.isBound()).isTrue();
    }

    @Test
    public void testBind_bound_doesNotRebound() {
        setupCarAppActivityForTesting();
        ICarAppActivity iCarAppActivity = mock(ICarAppActivity.class);

        IRendererService renderService = createMockRendererService();
        mServiceConnectionManager.setRendererService(renderService);
        mServiceConnectionManager.bind(TEST_INTENT, iCarAppActivity, TEST_DISPLAY_ID);
        mMainLooper.idle();
        try {
            assertThat(mViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.CONNECTED);
            assertThat(mViewModel.getError().getValue()).isNull();
            verify(mRenderService, never()).initialize(iCarAppActivity, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
            verify(mRenderService, never()).onNewIntent(TEST_INTENT, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
        assertThat(mServiceConnectionManager.isBound()).isTrue();
    }

    @Test
    public void testBind_bound_initializes() {
        setupCarAppActivityForTesting();
        ICarAppActivity iCarAppActivity = mock(ICarAppActivity.class);

        IRendererService renderService = createMockRendererService();

        mServiceConnectionManager.setRendererService(renderService);
        mServiceConnectionManager.bind(TEST_INTENT, iCarAppActivity, TEST_DISPLAY_ID);
        mMainLooper.idle();

        try {
            assertThat(mViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.CONNECTED);
            assertThat(mViewModel.getError().getValue()).isNull();
            verify(renderService).initialize(iCarAppActivity, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
            verify(renderService).onNewIntent(TEST_INTENT, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
        assertThat(mServiceConnectionManager.isBound()).isTrue();
    }

    @Test
    public void testBind_bound_noRebound() {
        setupCarAppActivityForTesting();
        ICarAppActivity iCarAppActivity = mock(ICarAppActivity.class);
        IRendererService renderService = createMockRendererService();

        mServiceConnectionManager.setRendererService(renderService);
        mServiceConnectionManager.bind(TEST_INTENT, iCarAppActivity, TEST_DISPLAY_ID);
        mMainLooper.idle();
        try {
            assertThat(mViewModel.getState().getValue()).isEqualTo(CarAppViewModel.State.CONNECTED);
            assertThat(mViewModel.getError().getValue()).isNull();
            verify(mRenderService, never()).initialize(iCarAppActivity, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
            verify(mRenderService, never()).onNewIntent(TEST_INTENT, mFakeCarAppServiceComponent,
                    TEST_DISPLAY_ID);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
        assertThat(mServiceConnectionManager.isBound()).isTrue();
    }

    @Test
    public void testBind_unbound_failure() {
        setupCarAppActivityForTesting();
        ICarAppActivity iCarAppActivity = mock(ICarAppActivity.class);

        try {
            when(mRenderService.initialize(any(ICarAppActivity.class),
                    any(ComponentName.class),
                    anyInt())).thenReturn(false);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }

        mServiceConnectionManager.bind(TEST_INTENT, iCarAppActivity, TEST_DISPLAY_ID);
        mMainLooper.idle();

        assertThat(mViewModel.getError().getValue())
                .isEqualTo(ErrorHandler.ErrorType.HOST_ERROR);
    }

    @Test
    public void testUnBind_bound_terminate() {
        setupCarAppActivityForTesting();

        IRendererService renderService = mock(IRendererService.class);
        mServiceConnectionManager.setRendererService(renderService);
        mServiceConnectionManager.unbind();
        mMainLooper.idle();

        try {
            verify(renderService).terminate(mFakeCarAppServiceComponent);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }

        assertThat(mServiceConnectionManager.isBound()).isFalse();
    }

    @Test
    public void testUnBind_unbound_doNothing() {
        setupCarAppActivityForTesting();

        mServiceConnectionManager.unbind();
        mMainLooper.idle();

        try {
            verify(mRenderService, never()).terminate(mFakeCarAppServiceComponent);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }

        assertThat(mServiceConnectionManager.isBound()).isFalse();
    }

    // Use delegate to forward events to a mock. Mockito interceptor is not maintained on
    // top-level IBinder after call to IRenderService.Stub.asInterface() in CarAppActivity.
    private static class RenderServiceDelegate extends IRendererService.Stub {
        private final IRendererService mService;

        RenderServiceDelegate(IRendererService service) {
            mService = service;
        }

        @Override
        public boolean initialize(ICarAppActivity carActivity, ComponentName serviceName,
                int displayId) throws RemoteException {
            return mService.initialize(carActivity, serviceName, displayId);
        }

        @Override
        public boolean onNewIntent(Intent intent, ComponentName serviceName, int displayId)
                throws RemoteException {
            return mService.onNewIntent(intent, serviceName, displayId);
        }

        @Override
        public void terminate(ComponentName serviceName) throws RemoteException {
            mService.terminate(serviceName);
        }
    }

    private IRendererService createMockRendererService() {
        IRendererService renderService = mock(IRendererService.class);
        try {
            when(renderService.initialize(any(ICarAppActivity.class),
                    any(ComponentName.class),
                    anyInt())).thenReturn(true);
            when(renderService.onNewIntent(any(Intent.class), any(ComponentName.class),
                    anyInt())).thenReturn(true);
        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
        return renderService;
    }
}
