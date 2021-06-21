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

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_R;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.car.app.CarAppService;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.activity.renderer.surface.LegacySurfacePackage;
import androidx.car.app.activity.renderer.surface.SurfaceControlCallback;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link CarAppActivity}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarAppActivityTest {
    private final ComponentName mRendererComponent = new ComponentName(
            ApplicationProvider.getApplicationContext(), getClass().getName());
    private final String mFakeCarAppServiceClass = "com.fake.FakeCarAppService";
    private final ComponentName mFakeCarAppServiceComponent = new ComponentName(
            ApplicationProvider.getApplicationContext(), mFakeCarAppServiceClass);
    private final IRendererService mRenderService = mock(IRendererService.class);
    private final RenderServiceDelegate mRenderServiceDelegate =
            new RenderServiceDelegate(mRenderService);

    private void setupCarAppActivityForTesting() {
        try {
            Application app = ApplicationProvider.getApplicationContext();

            // Register fake {@code CarAppService}
            PackageManager packageManager = app.getPackageManager();
            ShadowPackageManager spm = shadowOf(packageManager);
            spm.addServiceIfNotPresent(mFakeCarAppServiceComponent);
            spm.addIntentFilterForService(mFakeCarAppServiceComponent,
                    new IntentFilter(CarAppService.SERVICE_INTERFACE));

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

        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void testRendererInitialization() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    verify(mRenderService, times(1)).initialize(
                            mRenderServiceDelegate.getCarAppActivity(),
                            mFakeCarAppServiceComponent, activity.getDisplayId());
                    verify(mRenderService, times(1)).onNewIntent(activity.getIntent(),
                            mFakeCarAppServiceComponent, activity.getDisplayId());
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testActivityLifecycleCallbacks() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                IRendererCallback callback = mock(IRendererCallback.class);
                try {
                    mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
                    // Last observed event is reported as soon as callback is set.
                    verify(callback, times(1)).onResume();
                    // Verify lifecycle events are reported to registered callback.
                    scenario.moveToState(Lifecycle.State.STARTED);
                    verify(callback, times(1)).onPause();
                    scenario.moveToState(Lifecycle.State.RESUMED);
                    verify(callback, times(2)).onResume();
                    scenario.moveToState(Lifecycle.State.CREATED);
                    verify(callback, times(1)).onStop();
                    scenario.moveToState(Lifecycle.State.CREATED);
                    verify(callback, times(1)).onStop();
                    scenario.moveToState(Lifecycle.State.DESTROYED);
                    verify(callback, times(1)).onDestroyed();
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testOnServiceConnectionError() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    IRendererCallback callback = mock(IRendererCallback.class);
                    mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
                    // Last observed event is reported as soon as callback is set.
                    verify(callback, times(1)).onResume();

                    // Add a test-specific lifecycle callback to activity.
                    ActivityLifecycleCallbacks activityCallback = mock(
                            ActivityLifecycleCallbacks.class);
                    activity.registerActivityLifecycleCallbacks(activityCallback);
                    // Report service connection error.
                    CarAppViewModel viewModel =
                            new ViewModelProvider(activity).get(CarAppViewModel.class);
                    viewModel.onError(ErrorHandler.ErrorType.HOST_ERROR);

                    assertThat(activity.isFinishing()).isEqualTo(false);

                    // After service connection error has been reported, test that lifecycle
                    // events are no longer reported to host lifecycle listener.
                    scenario.moveToState(Lifecycle.State.STARTED);
                    verify(activityCallback, times(1)).onActivityPaused(activity);
                    verify(callback, times(0)).onPause();
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testOnBackPressed() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    IRendererCallback callback = mock(IRendererCallback.class);
                    mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
                    activity.onBackPressed();
                    verify(callback, times(1)).onBackPressed();
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testUnbindOnDestroy() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    ServiceConnectionManager serviceConnectionManager =
                            activity.mViewModel.getServiceConnectionManager();
                    ServiceConnection serviceConnection =
                            spy(serviceConnectionManager.getServiceConnection());
                    serviceConnectionManager.setServiceConnection(serviceConnection);

                    // Destroy activity to force unbind.
                    scenario.moveToState(Lifecycle.State.DESTROYED);

                    // Verify Activity onDestroy even is reported to renderer.
                    verify(mRenderService, times(1)).terminate(
                            mFakeCarAppServiceComponent);
                    // Verify service connection is closed.
                    verify(serviceConnection, times(1)).onServiceDisconnected(
                            mRendererComponent);
                    assertThat(serviceConnectionManager.isBound()).isFalse();
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testLegacySurfacePackageEvents() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    SurfaceControlCallback callback = mock(SurfaceControlCallback.class);
                    IRendererCallback rendererCallback = mock(IRendererCallback.class);

                    ICarAppActivity carAppActivity = mRenderServiceDelegate.getCarAppActivity();
                    carAppActivity.setSurfacePackage(
                            Bundleable.create(new LegacySurfacePackage(callback)));
                    carAppActivity.registerRendererCallback(rendererCallback);

                    // Verify back events on the activity are sent to host.
                    activity.onBackPressed();
                    verify(rendererCallback, times(1)).onBackPressed();

                    // Verify focus request sent to host.
                    activity.mSurfaceView.requestFocus();
                    verify(callback, times(1)).onWindowFocusChanged(true, false);
                    activity.mSurfaceView.clearFocus();
                    verify(callback, times(1)).onWindowFocusChanged(false, false);

                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = SystemClock.uptimeMillis();
                    int action = MotionEvent.ACTION_UP;
                    int x = 50;
                    int y = 50;
                    int metaState = 0;
                    MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y,
                            metaState);
                    activity.mSurfaceView.dispatchTouchEvent(event);
                    ArgumentCaptor<MotionEvent> argument = ArgumentCaptor.forClass(
                            MotionEvent.class);
                    verify(callback, times(1)).onTouchEvent(argument.capture());
                    // Compare string representations as equals in MotionEvent checks for same
                    // object.
                    assertThat(argument.getValue().toString()).isEqualTo(event.toString());
                } catch (RemoteException | BundlerException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testKeyboardInputWithoutStartInput() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    EditorInfo editorInfo = new EditorInfo();
                    IRendererCallback callback = mock(IRendererCallback.class);
                    IProxyInputConnection inputConnection = mock(IProxyInputConnection.class);
                    when(callback.onCreateInputConnection(any())).thenReturn(inputConnection);
                    when(inputConnection.getEditorInfo()).thenReturn(editorInfo);

                    mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
                    // Create input connection without first calling ICarAppActivity#startInput().
                    InputConnection remoteProxyInputConnection =
                            activity.mSurfaceView.onCreateInputConnection(editorInfo);

                    assertThat(remoteProxyInputConnection).isNull();
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    @Test
    public void testKeyboardInput() {
        setupCarAppActivityForTesting();
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    EditorInfo editorInfo = new EditorInfo();
                    IRendererCallback callback = mock(IRendererCallback.class);
                    IProxyInputConnection inputConnection = mock(IProxyInputConnection.class);
                    when(callback.onCreateInputConnection(any())).thenReturn(inputConnection);
                    when(inputConnection.getEditorInfo()).thenReturn(editorInfo);

                    mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
                    mRenderServiceDelegate.getCarAppActivity().onStartInput();
                    InputConnection remoteProxyInputConnection =
                            activity.mSurfaceView.onCreateInputConnection(editorInfo);

                    assertThat(remoteProxyInputConnection).isNotNull();

                    // Verify input events re proxied to host.
                    KeyEvent event = new KeyEvent(ACTION_UP, KEYCODE_R);
                    remoteProxyInputConnection.sendKeyEvent(event);
                    verify(inputConnection, times(1)).sendKeyEvent(event);
                } catch (RemoteException e) {
                    fail(Log.getStackTraceString(e));
                }
            });
        }
    }

    // Use delegate to forward events to a mock. Mockito interceptor is not maintained on
    // top-level IBinder after call to IRenderService.Stub.asInterface() in CarAppActivity.
    private static class RenderServiceDelegate extends IRendererService.Stub {
        private final IRendererService mService;
        private ICarAppActivity mCarAppActivity;

        RenderServiceDelegate(IRendererService service) {
            mService = service;
        }

        @Override
        public boolean initialize(ICarAppActivity carActivity, ComponentName serviceName,
                int displayId) throws RemoteException {
            mCarAppActivity = carActivity;
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

        public ICarAppActivity getCarAppActivity() {
            return mCarAppActivity;
        }
    }
}
