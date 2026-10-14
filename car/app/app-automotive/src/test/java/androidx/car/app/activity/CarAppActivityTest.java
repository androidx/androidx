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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import androidx.car.app.CarAppService;
import androidx.car.app.HandshakeInfo;
import androidx.car.app.SessionInfo;
import androidx.car.app.SessionInfoIntentEncoder;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IInsetsListener;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.activity.renderer.surface.LegacySurfacePackage;
import androidx.car.app.activity.renderer.surface.SurfaceControlCallback;
import androidx.car.app.automotive.R;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link CarAppActivity}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class CarAppActivityTest {
    public static final String INTENT_IDENTIFIER = "CarAppActivityTest";
    private static final String STORE_PACKAGE = "com.fake.store";
    private static final String OTHER_STORE_PACKAGE = "com.fake.otherstore";
    private static final String VENDING_PACKAGE = "com.android.vending";
    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String GAS_HOST_PACKAGE =
            "com.google.android.apps.automotive.templates.host";
    private final ComponentName mRendererComponent = new ComponentName(getApplicationContext(),
            getClass().getName());
    private final String mFakeCarAppServiceClass = "com.fake.FakeCarAppService";
    private final ComponentName mFakeCarAppServiceComponent = new ComponentName(
            getApplicationContext(), mFakeCarAppServiceClass);
    private final IRendererService mRenderService = mock(IRendererService.class);
    private final RenderServiceDelegate mRenderServiceDelegate = new RenderServiceDelegate(
            mRenderService);

    @Before
    public void setup() {
        try {
            Application app = getApplicationContext();

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

            when(mRenderService.initialize(any(ICarAppActivity.class), any(ComponentName.class),
                    anyInt())).thenReturn(true);
            when(mRenderService.onNewIntent(any(Intent.class), any(ComponentName.class),
                    anyInt())).thenReturn(true);

            ShadowApplication sa = shadowOf(app);
            sa.setComponentNameAndServiceForBindService(mRendererComponent, mRenderServiceDelegate);

        } catch (RemoteException e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void testRendererInitialization() {
        runOnActivity((scenario, activity) -> {
            verify(mRenderService, times(1)).initialize(mRenderServiceDelegate.getCarAppActivity(),
                    mFakeCarAppServiceComponent, activity.getDisplayId());
            verify(mRenderService, times(1)).onNewIntent(activity.getIntent(),
                    mFakeCarAppServiceComponent, activity.getDisplayId());

        });
    }

    @Test
    public void testOnNewIntent_propagatesIntent() {
        runOnActivity((scenario, activity) -> {
            // Reset mock to clear calls from initial activity launch
            reset(mRenderService);
            when(mRenderService.onNewIntent(any(), any(), anyInt())).thenReturn(true);

            Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
            newIntent.putExtra("test_key", "test_value");

            scenario.onActivity(a -> a.onNewIntent(newIntent));

            // Verify onNewIntent is called with the new intent.
            verify(mRenderService, times(1)).onNewIntent(argThat((Intent intent) ->
                            "test_value".equals(intent.getStringExtra("test_key"))),
                    eq(mFakeCarAppServiceComponent), eq(activity.getDisplayId()));

            // Properties check: Surface should be visible when connected
            assertThat(activity.mSurfaceView.getVisibility()).isEqualTo(View.VISIBLE);
            assertThat(activity.mErrorMessageView.getVisibility()).isEqualTo(View.GONE);
            assertThat(activity.mLoadingView.getVisibility()).isEqualTo(View.GONE);
        });
    }

    @Test
    public void testOnNewIntent_whenConnected_doesNotRebind() {
        runOnActivity((scenario, activity) -> {
            // Reset mock to clear calls from initial activity launch
            reset(mRenderService);
            when(mRenderService.onNewIntent(any(), any(), anyInt())).thenReturn(true);
            when(mRenderService.initialize(any(), any(), anyInt())).thenReturn(true);

            Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
            scenario.onActivity(a -> a.onNewIntent(newIntent));

            // Verify initialize was NOT called because it was already connected
            verify(mRenderService, never()).initialize(any(), any(), anyInt());

            // Properties check: Surface should still be visible
            assertThat(activity.mSurfaceView.getVisibility()).isEqualTo(View.VISIBLE);
        });
    }

    @Test
    public void testOnNewIntent_whenDisconnected_rebinds() {
        runOnActivity((scenario, activity) -> {
            CarAppViewModel viewModel = activity.mViewModel;
            assertThat(viewModel).isNotNull();

            // Force an error state
            scenario.onActivity(a -> viewModel.onError(ErrorHandler.ErrorType.HOST_ERROR));
            org.robolectric.shadows.ShadowLooper.idleMainLooper();

            // Verify surface is hidden on error
            assertThat(activity.mSurfaceView.getVisibility()).isEqualTo(View.GONE);

            reset(mRenderService);
            when(mRenderService.onNewIntent(any(), any(), anyInt())).thenReturn(true);
            when(mRenderService.initialize(any(), any(), anyInt())).thenReturn(true);
            when(mRenderService.performHandshake(any(), anyInt())).thenReturn(
                    Bundleable.create(new HandshakeInfo("", CarAppApiLevels.getLatest())));

            Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
            scenario.onActivity(a -> a.onNewIntent(newIntent));
            org.robolectric.shadows.ShadowLooper.idleMainLooper();

            // Verify initialize WAS called (since we were disconnected)
            verify(mRenderService, times(1)).initialize(any(), eq(mFakeCarAppServiceComponent),
                    eq(activity.getDisplayId()));

            // Verify onNewIntent was called
            verify(mRenderService, times(1)).onNewIntent(argThat((Intent intent) ->
                            intent.getComponent().equals(newIntent.getComponent())),
                    eq(mFakeCarAppServiceComponent), eq(activity.getDisplayId()));

            // Properties check: Surface should be visible again after rebind
            assertThat(activity.mSurfaceView.getVisibility()).isEqualTo(View.VISIBLE);
        });
    }

    @Test
    public void testActivityLifecycleCallbacks() {
        runOnActivity((scenario, activity) -> {
            IRendererCallback callback = mock(IRendererCallback.class);
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

        });
    }

    @Test
    public void testOnServiceConnectionError() {
        runOnActivity((scenario, activity) -> {
            IRendererCallback callback = mock(IRendererCallback.class);
            mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
            // Last observed event is reported as soon as callback is set.
            verify(callback, times(1)).onResume();

            // Add a test-specific lifecycle callback to activity.
            ActivityLifecycleCallbacks activityCallback = mock(ActivityLifecycleCallbacks.class);
            activity.registerActivityLifecycleCallbacks(activityCallback);
            // Report service connection error.
            CarAppViewModel viewModel = new ViewModelProvider(activity).get(CarAppViewModel.class);
            viewModel.onError(ErrorHandler.ErrorType.HOST_ERROR);

            assertThat(activity.isFinishing()).isEqualTo(false);

            // After service connection error has been reported, test that lifecycle
            // events are no longer reported to host lifecycle listener.
            scenario.moveToState(Lifecycle.State.STARTED);
            verify(activityCallback, times(1)).onActivityPaused(activity);
            verify(callback, times(0)).onPause();

        });
    }

    @Test
    public void testOnBackPressed() {
        runOnActivity((scenario, activity) -> {
            IRendererCallback callback = mock(IRendererCallback.class);
            mRenderServiceDelegate.getCarAppActivity().registerRendererCallback(callback);
            activity.onBackPressed();
            verify(callback, times(1)).onBackPressed();

        });
    }

    @Test
    public void testUnbindOnDestroy() {
        runOnActivity((scenario, activity) -> {
            ServiceConnectionManager serviceConnectionManager =
                    activity.mViewModel.getServiceConnectionManager();
            ServiceConnection serviceConnection = spy(
                    serviceConnectionManager.getServiceConnection());
            serviceConnectionManager.setServiceConnection(serviceConnection);

            // Destroy activity to force unbind.
            scenario.moveToState(Lifecycle.State.DESTROYED);

            // Verify Activity onDestroy even is reported to renderer.
            verify(mRenderService, times(1)).terminate(mFakeCarAppServiceComponent);
            // Verify service connection is closed.
            verify(serviceConnection, times(1)).onServiceDisconnected(mRendererComponent);
            assertThat(serviceConnectionManager.isBound()).isFalse();

        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLegacySurfacePackageEvents() {
        runOnActivity((scenario, activity) -> {
            SurfaceControlCallback callback = mock(SurfaceControlCallback.class);
            IRendererCallback rendererCallback = mock(IRendererCallback.class);

            ICarAppActivity carAppActivity = mRenderServiceDelegate.getCarAppActivity();
            carAppActivity.setSurfacePackage(Bundleable.create(new LegacySurfacePackage(callback)));
            carAppActivity.registerRendererCallback(rendererCallback);

            // Verify back events on the activity are sent to host.
            activity.onBackPressed();
            verify(rendererCallback, times(1)).onBackPressed();

            // Verify focus request sent to host.
            activity.mSurfaceView.clearFocus();
            verify(callback, times(1)).onWindowFocusChanged(false, false);
            activity.mSurfaceView.requestFocus();
            verify(callback, times(1)).onWindowFocusChanged(true, false);

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();
            int action = MotionEvent.ACTION_UP;
            int x = 50;
            int y = 50;
            int metaState = 0;
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
            activity.mSurfaceView.dispatchTouchEvent(event);
            ArgumentCaptor<MotionEvent> argument = ArgumentCaptor.forClass(MotionEvent.class);
            verify(callback, times(1)).onTouchEvent(argument.capture());
            // Compare string representations as equals in MotionEvent checks for same
            // object.
            assertThat(argument.getValue().toString()).isEqualTo(event.toString());
        });
    }

    @Test
    public void testKeyboardInputWithoutStartInput() {
        runOnActivity((scenario, activity) -> {
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

        });
    }

    @Test
    public void testKeyboardInput() {
        runOnActivity((scenario, activity) -> {
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

        });
    }

    @Test
    public void testWindowInsetsHandledRemotelyWhenHostIsCapable() {
        runOnActivity((scenario, activity) -> {
            IInsetsListener insetsListener = mock(IInsetsListener.class);
            mRenderServiceDelegate.getCarAppActivity().setInsetsListener(insetsListener);
            View activityContainer = activity.mActivityContainerView;
            View localContentContainer = activity.mLocalContentContainerView;
            Insets systemWindowInsets = Insets.of(10, 20, 30, 40);
            WindowInsets windowInsets = new WindowInsetsCompat.Builder().setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    systemWindowInsets).build().toWindowInsets();
            activityContainer.onApplyWindowInsets(windowInsets);

            // Verify that the host is notified and insets are not handled locally
            verify(insetsListener).onWindowInsetsChanged(eq(systemWindowInsets.toPlatformInsets()),
                    eq(Insets.NONE.toPlatformInsets()));
            assertThat(activityContainer.getPaddingBottom()).isEqualTo(0);
            assertThat(activityContainer.getPaddingTop()).isEqualTo(0);
            assertThat(activityContainer.getPaddingLeft()).isEqualTo(0);
            assertThat(activityContainer.getPaddingRight()).isEqualTo(0);
            assertThat(localContentContainer.getPaddingBottom()).isEqualTo(40);
            assertThat(localContentContainer.getPaddingTop()).isEqualTo(20);
            assertThat(localContentContainer.getPaddingLeft()).isEqualTo(10);
            assertThat(localContentContainer.getPaddingRight()).isEqualTo(30);
        });
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void testWindowInsets_whenRAndAbove_handlesInsetsCorrectly() {
        runOnActivity((scenario, activity) -> {
            IInsetsListener insetsListener = mock(IInsetsListener.class);
            mRenderServiceDelegate.getCarAppActivity().setInsetsListener(insetsListener);
            View activityContainer = activity.mActivityContainerView;
            Insets insets = Insets.of(50, 60, 70, 80);
            WindowInsets windowInsets = new WindowInsets.Builder().setInsets(
                    WindowInsets.Type.systemBars(),
                    insets.toPlatformInsets()).build();
            activityContainer.onApplyWindowInsets(windowInsets);

            // Verify that system bars insets are handled correctly.
            verify(insetsListener).onWindowInsetsChanged(eq(insets.toPlatformInsets()),
                    eq(Insets.NONE.toPlatformInsets()));

            windowInsets = new WindowInsets.Builder().setInsets(
                    WindowInsets.Type.ime(),
                    insets.toPlatformInsets()).build();
            activityContainer.onApplyWindowInsets(windowInsets);

             // Verify that ime insets are handled correctly.
            verify(insetsListener).onWindowInsetsChanged(eq(insets.toPlatformInsets()),
                    eq(Insets.NONE.toPlatformInsets()));
        });
    }

    @Test
    public void testServiceNotTerminatedWhenConfigurationChanges() {
        runOnActivity((scenario, activity) -> {
            System.out.println("before");
            scenario.recreate();
            System.out.println("after");
            verify(mRenderService, never()).terminate(mFakeCarAppServiceComponent);
        });
    }

    @Test
    public void testServiceTerminatedWhenActivityDiesWithoutConfigChange() {
        runOnActivity((scenario, activity) -> {
            System.out.println("before");
            scenario.moveToState(Lifecycle.State.DESTROYED);
            System.out.println("after");
            verify(mRenderService, times(1)).terminate(mFakeCarAppServiceComponent);
        });
    }

    @Test
    public void testLaunchWithIdentifier_passesAlongValues() {
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
        newIntent.setIdentifier(INTENT_IDENTIFIER);
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(newIntent)) {
            scenario.onActivity(activity -> {
                try {
                    verify(mRenderService, times(1)).initialize(
                            mRenderServiceDelegate.getCarAppActivity(), mFakeCarAppServiceComponent,
                            activity.getDisplayId());
                    verify(mRenderService, times(1)).onNewIntent(intentArgumentCaptor.capture(),
                            eq(mFakeCarAppServiceComponent), eq(activity.getDisplayId()));

                    Intent intent = intentArgumentCaptor.getValue();
                    SessionInfo si = SessionInfoIntentEncoder.decode(intent);

                    assertThat(si.getDisplayType()).isEqualTo(SessionInfo.DISPLAY_TYPE_MAIN);
                    assertThat(intent.getIdentifier()).isEqualTo(si.toString());
                } catch (Exception e) {
                    fail(Log.getStackTraceString(e));
                }
            });

        }

    }

    @Test
    public void testLaunchWithoutIdentifier_setsRandomIdValue() {
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(newIntent)) {
            scenario.onActivity(activity -> {
                try {
                    verify(mRenderService, times(1)).initialize(
                            mRenderServiceDelegate.getCarAppActivity(), mFakeCarAppServiceComponent,
                            activity.getDisplayId());
                    verify(mRenderService, times(1)).onNewIntent(intentArgumentCaptor.capture(),
                            eq(mFakeCarAppServiceComponent), eq(activity.getDisplayId()));

                    Intent intent = intentArgumentCaptor.getValue();
                    SessionInfo si = SessionInfoIntentEncoder.decode(intent);

                    assertThat(si.getDisplayType()).isEqualTo(SessionInfo.DISPLAY_TYPE_MAIN);
                    assertThat(intent.getIdentifier()).isNotEqualTo(INTENT_IDENTIFIER);
                    assertThat(intent.getIdentifier()).isEqualTo(si.toString());
                } catch (Exception e) {
                    fail(Log.getStackTraceString(e));
                }
            });

        }

    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.Q)
    public void testFitsSystemWindows_whenQAndBelow_shouldSetFitsSystemWindowsToFalse() {
        Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(newIntent)) {
            scenario.onActivity(activity -> {
                try {
                    assertThat(
                            activity.getWindow().getDecorView().getFitsSystemWindows()).isFalse();
                } catch (Exception e) {
                    fail(Log.getStackTraceString(e));
                }
            });

        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void testDecorFitsSystemWindows_whenRAndAbove_shouldSetDecorFitsSystemWindowsToFalse() {
        Intent newIntent = new Intent(getApplicationContext(), CarAppActivity.class);
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(newIntent)) {
            scenario.onActivity(activity -> {
                try {
                    assertThat(activity.getDecorFitsSystemWindows()).isFalse();
                } catch (Exception e) {
                    fail(Log.getStackTraceString(e));
                }
            });

        }
    }

    @Test
    public void onApplyWindowInsets_dispatchesWindowInsetsToViewModel() {
        runOnActivity((scenario, activity) -> {
            CarAppViewModel mockViewModel = mock(CarAppViewModel.class);
            activity.mViewModel = mockViewModel;

            int statusBarTop = 10;
            int navigationBarBottom = 20;
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(),
                            Insets.of(0, statusBarTop, 0, navigationBarBottom))
                    .build();

            activity.mActivityContainerView.onApplyWindowInsets(insets.toWindowInsets());

            verify(mockViewModel).updateWindowInsets(
                    eq(insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets()),
                    eq(insets.getDisplayCutout())
            );
        });
    }

    @Test
    public void onApplyWindowInsets_setsCorrectLocalPadding_systemBars() {
        runOnActivity((scenario, activity) -> {
            CarAppViewModel mockViewModel = mock(CarAppViewModel.class);
            activity.mViewModel = mockViewModel;

            int statusBarTop = 10;
            int navigationBarBottom = 20;
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(),
                            Insets.of(5, statusBarTop, 15, navigationBarBottom))
                    .build();

            activity.mActivityContainerView.onApplyWindowInsets(insets.toWindowInsets());

            // mActivityContainerView should have 0 padding
            assertThat(activity.mActivityContainerView.getPaddingLeft()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingTop()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingRight()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingBottom()).isEqualTo(0);

            // mLocalContentContainerView should have systemBars padding
            assertThat(activity.mLocalContentContainerView.getPaddingLeft()).isEqualTo(5);
            assertThat(activity.mLocalContentContainerView.getPaddingTop()).isEqualTo(statusBarTop);
            assertThat(activity.mLocalContentContainerView.getPaddingRight()).isEqualTo(15);
            assertThat(activity.mLocalContentContainerView.getPaddingBottom()).isEqualTo(navigationBarBottom);
        });
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void onApplyWindowInsets_dispatchesImeInsetsToViewModel() {
        runOnActivity((scenario, activity) -> {
            CarAppViewModel mockViewModel = mock(CarAppViewModel.class);
            activity.mViewModel = mockViewModel;

            int imeBottom = 100;
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.ime(),
                            Insets.of(0, 0, 0, imeBottom))
                    .build();

            activity.mWindowInsetsListener.onApplyWindowInsets(
                    activity.mActivityContainerView, insets.toWindowInsets());

            verify(mockViewModel).updateImeInsets(
                    eq(insets.getInsets(WindowInsetsCompat.Type.ime()).toPlatformInsets())
            );
        });
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void onApplyWindowInsets_setsCorrectLocalPadding_combined() {
        runOnActivity((scenario, activity) -> {
            CarAppViewModel mockViewModel = mock(CarAppViewModel.class);
            activity.mViewModel = mockViewModel;

            int statusBarTop = 10;
            int navigationBarBottom = 20;
            int imeBottom = 100;
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(),
                            Insets.of(5, statusBarTop, 15, navigationBarBottom))
                    .setInsets(WindowInsetsCompat.Type.ime(),
                            Insets.of(0, 0, 0, imeBottom))
                    .build();

            activity.mWindowInsetsListener.onApplyWindowInsets(
                    activity.mActivityContainerView, insets.toWindowInsets());

            // mActivityContainerView should have 0 padding
            assertThat(activity.mActivityContainerView.getPaddingLeft()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingTop()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingRight()).isEqualTo(0);
            assertThat(activity.mActivityContainerView.getPaddingBottom()).isEqualTo(0);

            // mLocalContentContainerView should have combined padding
            assertThat(activity.mLocalContentContainerView.getPaddingLeft()).isEqualTo(5);
            assertThat(activity.mLocalContentContainerView.getPaddingTop()).isEqualTo(statusBarTop);
            assertThat(activity.mLocalContentContainerView.getPaddingRight()).isEqualTo(15);
            assertThat(activity.mLocalContentContainerView.getPaddingBottom()).isEqualTo(imeBottom);
        });
    }

    @Test
    public void updateHost_installerHandlesShowAppInfo_launchesInstallerAppInfo() {
        runOnActivity((scenario, activity) -> {
            String hostPackage = mRendererComponent.getPackageName();
            ComponentName store = registerStoreActivity(STORE_PACKAGE,
                    filter(Intent.ACTION_SHOW_APP_INFO, /* withDefaultCategory= */ true));
            shadowOf(activity.getPackageManager()).setInstallSourceInfo(hostPackage,
                    STORE_PACKAGE, STORE_PACKAGE);

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertThat(launched.getAction()).isEqualTo(Intent.ACTION_SHOW_APP_INFO);
            assertThat(launched.getComponent()).isEqualTo(store);
            assertThat(launched.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(hostPackage);
            assertThat(activity.isFinishing()).isTrue();
        });
    }

    @Test
    public void updateHost_installerShowAppInfoWithoutDefaultCategory_launchesExplicitly() {
        runOnActivity((scenario, activity) -> {
            String hostPackage = mRendererComponent.getPackageName();
            ComponentName store = registerStoreActivity(STORE_PACKAGE,
                    filter(Intent.ACTION_SHOW_APP_INFO, /* withDefaultCategory= */ false));
            shadowOf(activity.getPackageManager()).setInstallSourceInfo(hostPackage,
                    STORE_PACKAGE, STORE_PACKAGE);

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertThat(launched.getAction()).isEqualTo(Intent.ACTION_SHOW_APP_INFO);
            assertThat(launched.getComponent()).isEqualTo(store);
        });
    }

    @Test
    public void updateHost_installerOnlyHandlesMarketScheme_launchesInstallerMarketDetails() {
        runOnActivity((scenario, activity) -> {
            String hostPackage = mRendererComponent.getPackageName();
            ComponentName store = registerStoreActivity(STORE_PACKAGE, marketFilter());
            shadowOf(activity.getPackageManager()).setInstallSourceInfo(hostPackage,
                    STORE_PACKAGE, STORE_PACKAGE);

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertThat(launched.getAction()).isEqualTo(Intent.ACTION_VIEW);
            assertThat(launched.getData().toString())
                    .isEqualTo(MARKET_DETAILS_URI + hostPackage);
            assertThat(launched.getComponent()).isEqualTo(store);
        });
    }

    @Test
    public void updateHost_unknownInstaller_launchesAnyMarketDetails() {
        runOnActivity((scenario, activity) -> {
            String hostPackage = mRendererComponent.getPackageName();
            ComponentName store = registerStoreActivity(STORE_PACKAGE, marketFilter());

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertThat(launched.getData().toString())
                    .isEqualTo(MARKET_DETAILS_URI + hostPackage);
            assertThat(launched.getComponent()).isEqualTo(store);
        });
    }

    @Test
    public void updateHost_noHostInstalled_launchesGasHostPageOnPlayStore() {
        runOnActivity((scenario, activity) -> {
            shadowOf(activity.getPackageManager()).removeService(mRendererComponent);
            ComponentName playStore = registerStoreActivity(VENDING_PACKAGE,
                    marketFilter());

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_NOT_FOUND);

            assertThat(launched.getData().toString()).isEqualTo(
                    MARKET_DETAILS_URI + GAS_HOST_PACKAGE);
            assertThat(launched.getComponent()).isEqualTo(playStore);
        });
    }

    @Test
    public void updateHost_noHostInstalledAndNoPlayStore_launchesAppMarket() {
        runOnActivity((scenario, activity) -> {
            shadowOf(activity.getPackageManager()).removeService(mRendererComponent);
            IntentFilter appMarketFilter = filter(Intent.ACTION_MAIN,
                    /* withDefaultCategory= */ false);
            appMarketFilter.addCategory(Intent.CATEGORY_APP_MARKET);
            ComponentName store = registerStoreActivity(STORE_PACKAGE, appMarketFilter);
            // A store which can only show app details is not used, as no host package is known.
            registerStoreActivity(OTHER_STORE_PACKAGE, marketFilter());

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_NOT_FOUND);

            assertThat(launched.getAction()).isEqualTo(Intent.ACTION_MAIN);
            assertThat(launched.getComponent()).isEqualTo(store);
        });
    }

    @Test
    public void updateHost_multipleStoresWithoutDefaultCategory_launchesExplicitly() {
        runOnActivity((scenario, activity) -> {
            shadowOf(activity.getPackageManager()).removeService(mRendererComponent);
            IntentFilter appMarketFilter = filter(Intent.ACTION_MAIN,
                    /* withDefaultCategory= */ false);
            appMarketFilter.addCategory(Intent.CATEGORY_APP_MARKET);
            ComponentName store = registerStoreActivity(STORE_PACKAGE, appMarketFilter);
            ComponentName otherStore = registerStoreActivity(OTHER_STORE_PACKAGE,
                    appMarketFilter);

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_NOT_FOUND);

            assertThat(launched.getComponent()).isAnyOf(store, otherStore);
        });
    }

    @Test
    public void updateHost_multipleStoresWithDefaultCategory_letsSystemResolve() {
        runOnActivity((scenario, activity) -> {
            String hostPackage = mRendererComponent.getPackageName();
            registerStoreActivity(STORE_PACKAGE, marketFilter());
            registerStoreActivity(OTHER_STORE_PACKAGE, marketFilter());

            Intent launched = setErrorAndClickAction(activity,
                    ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertThat(launched.getComponent()).isNull();
            assertThat(launched.getData().toString())
                    .isEqualTo(MARKET_DETAILS_URI + hostPackage);
        });
    }

    @Test
    public void updateHost_vendingInstalledButUnableToHandleIntents_hidesAction() {
        runOnActivity((scenario, activity) -> {
            // Play Store installed, but not handling any of the store intents.
            shadowOf(activity.getPackageManager()).addActivityIfNotPresent(
                    new ComponentName(VENDING_PACKAGE, "Unrelated"));

            activity.mErrorMessageView.setError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE);

            assertNoStoreAvailable(activity);
        });
    }

    @Test
    public void updateHost_storeRemovedBeforeClick_doesNotCrash() {
        runOnActivity((scenario, activity) -> {
            ComponentName store = registerStoreActivity(STORE_PACKAGE, marketFilter());
            activity.mErrorMessageView.setError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE);
            assertThat(getActionButton(activity).getVisibility()).isEqualTo(View.VISIBLE);

            shadowOf(activity.getPackageManager()).removeActivity(store);
            shadowOf((Application) getApplicationContext()).checkActivities(true);
            getActionButton(activity).performClick();

            assertNoStoreAvailable(activity);
            assertThat(activity.isFinishing()).isFalse();
        });
    }

    private Intent setErrorAndClickAction(CarAppActivity activity,
            ErrorHandler.ErrorType errorType) {
        activity.mErrorMessageView.setError(errorType);
        Button actionButton = getActionButton(activity);
        assertThat(actionButton.getVisibility()).isEqualTo(View.VISIBLE);
        actionButton.performClick();
        Intent launched = shadowOf(activity).getNextStartedActivity();
        assertThat(launched).isNotNull();
        return launched;
    }

    private static void assertNoStoreAvailable(CarAppActivity activity) {
        assertThat(getActionButton(activity).getVisibility()).isEqualTo(View.INVISIBLE);
        TextView message = activity.mErrorMessageView.findViewById(R.id.error_message);
        assertThat(message.getText().toString())
                .isEqualTo(activity.getString(R.string.error_message_no_vending));
    }

    private static Button getActionButton(CarAppActivity activity) {
        return activity.mErrorMessageView.findViewById(R.id.action_button);
    }

    private static ComponentName registerStoreActivity(String packageName, IntentFilter filter) {
        ComponentName component = new ComponentName(packageName, packageName + ".StoreActivity");
        ShadowPackageManager spm = shadowOf(
                ((Application) getApplicationContext()).getPackageManager());
        spm.addActivityIfNotPresent(component);
        spm.addIntentFilterForActivity(component, filter);
        return component;
    }

    private static IntentFilter filter(String action, boolean withDefaultCategory) {
        IntentFilter filter = new IntentFilter(action);
        if (withDefaultCategory) {
            filter.addCategory(Intent.CATEGORY_DEFAULT);
        }
        return filter;
    }

    private static IntentFilter marketFilter() {
        IntentFilter filter = filter(Intent.ACTION_VIEW, /* withDefaultCategory= */ true);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addDataScheme("market");
        filter.addDataAuthority("details", null);
        return filter;
    }

    interface CarActivityAction {
        void accept(ActivityScenario<CarAppActivity> scenario, CarAppActivity activity)
                throws Exception;
    }

    private void runOnActivity(CarActivityAction block) {
        try (ActivityScenario<CarAppActivity> scenario = ActivityScenario.launch(
                CarAppActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    block.accept(scenario, activity);
                } catch (Exception e) {
                    fail(Log.getStackTraceString(e));
                }
            });

        }
    }
}
