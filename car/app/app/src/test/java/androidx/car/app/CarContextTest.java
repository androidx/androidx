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

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;

import androidx.activity.OnBackPressedCallback;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.managers.Manager;
import androidx.car.app.managers.ResultManager;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.suggestion.SuggestionManager;
import androidx.car.app.testing.TestLifecycleOwner;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tests for {@link CarContext}. */
@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = {"androidx.activity"})
@DoNotInstrument
public class CarContextTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final String APP_SERVICE = "app";
    private static final String NAVIGATION_SERVICE = "navigation";
    private static final String SCREEN_SERVICE = "screen";
    private static final String HARDWARE_SERVICE = "hardware";
    private static final String SUGGESTION_SERVICE = "suggestion";

    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IStartCarApp mMockStartCarApp;
    @Mock
    private Screen mMockScreen1;
    @Mock
    private Screen mMockScreen2;

    private CarContext mCarContext;
    private Screen mScreen1;
    private Screen mScreen2;
    private final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Before
    public void setUp() throws RemoteException {
        when(mMockCarHost.getHost(CarContext.APP_SERVICE)).thenReturn(new IAppHost.Stub() {
            @Override
            public void invalidate() {
            }

            @Override
            public void showToast(CharSequence text, int duration) {
            }

            @Override
            public void setSurfaceCallback(@Nullable ISurfaceCallback callback) {
            }

            @Override
            public void sendLocation(Location location) {
            }

            @Override
            public void showAlert(Bundleable alert) {
            }

            @Override
            public void dismissAlert(int alertId) {
            }

            @Override
            public Bundleable openMicrophone(Bundleable openMicrophoneRequest) {
                return null;
            }
        }.asBinder());

        TestStartCarAppStub startCarAppStub = new TestStartCarAppStub(mMockStartCarApp);

        Bundle extras = new Bundle(1);
        extras.putBinder(CarContext.EXTRA_START_CAR_APP_BINDER_KEY, startCarAppStub.asBinder());
        Intent intentFromNotification = new Intent().putExtras(extras);

        mCarContext = CarContext.create(mLifecycleOwner.mRegistry);
        mCarContext.attachBaseContext(ApplicationProvider.getApplicationContext(),
                ApplicationProvider.getApplicationContext().getResources().getConfiguration());
        mCarContext.setCarHost(mMockCarHost);
        // Set the app's lifecycle to STARTED so that screens would be set to the STARTED state when
        // pushed. Otherwise we may run into issues in tests when popping screens, where they go
        // from the INITIALIZED state to the DESTROYED state.
        mLifecycleOwner.mRegistry.setCurrentState(State.STARTED);

        mScreen1 = new TestScreen(mCarContext, mMockScreen1);
        mScreen2 = new TestScreen(mCarContext, mMockScreen2);
    }

    @Test
    public void getCarService_app() {
        assertThat(mCarContext.getCarService(CarContext.APP_SERVICE)).isEqualTo(
                mCarContext.getCarService(AppManager.class));
        assertThat(mCarContext.getCarService(CarContext.APP_SERVICE)).isNotNull();
    }

    @Test
    public void getCarService_navigation() {
        assertThat(mCarContext.getCarService(CarContext.NAVIGATION_SERVICE)).isEqualTo(
                mCarContext.getCarService(NavigationManager.class));
        assertThat(mCarContext.getCarService(CarContext.NAVIGATION_SERVICE)).isNotNull();
    }

    @Test
    public void getCarService_screenManager() {
        assertThat(mCarContext.getCarService(CarContext.SCREEN_SERVICE)).isEqualTo(
                mCarContext.getCarService(ScreenManager.class));
        assertThat(mCarContext.getCarService(CarContext.SCREEN_SERVICE)).isNotNull();
    }

    @Test
    public void getCarService_hardwareManager() {
        assertThrows(IllegalStateException.class,
                () -> mCarContext.getCarService(CarContext.HARDWARE_SERVICE));
    }

    @Test
    public void getCarService_suggestionManager() {
        assertThat(mCarContext.getCarService(CarContext.SUGGESTION_SERVICE)).isEqualTo(
                mCarContext.getCarService(SuggestionManager.class));
        assertThat(mCarContext.getCarService(CarContext.SUGGESTION_SERVICE)).isNotNull();
    }

    @Test
    public void getCarService_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> mCarContext.getCarService("foo"));
    }

    @Test
    public void getCarService_null_throws() {
        assertThrows(NullPointerException.class, () -> mCarContext.getCarService((String) null));
        assertThrows(NullPointerException.class,
                () -> mCarContext.getCarService((Class<Manager>) null));
    }

    @Test
    public void getCarServiceName_app() {
        assertThat(mCarContext.getCarServiceName(AppManager.class)).isEqualTo(APP_SERVICE);
    }

    @Test
    public void getCarServiceName_navigation() {
        assertThat(mCarContext.getCarServiceName(NavigationManager.class)).isEqualTo(
                NAVIGATION_SERVICE);
    }

    @Test
    public void getCarServiceName_suggestion() {
        assertThat(mCarContext.getCarServiceName(SuggestionManager.class)).isEqualTo(
                SUGGESTION_SERVICE);
    }

    @Test
    public void getCarServiceName_screenManager() {
        assertThat(mCarContext.getCarServiceName(ScreenManager.class)).isEqualTo(SCREEN_SERVICE);
    }

    @Test
    public void getCarServiceName_hardwareManager_throws() {
        assertThat(mCarContext.getCarServiceName(CarHardwareManager.class)).isEqualTo(
                HARDWARE_SERVICE);
    }

    @Test
    public void getCarServiceName_null_throws() {
        assertThrows(NullPointerException.class, () -> mCarContext.getCarServiceName(null));
    }

    @Test
    public void startCarApp_callsICarHostStartCarApp() throws RemoteException {
        Intent foo = new Intent("foo");
        mCarContext.startCarApp(foo);

        verify(mMockCarHost).startCarApp(foo);
    }

    @Test
    public void finishCarApp() throws RemoteException {
        mCarContext.finishCarApp();

        verify(mMockCarHost).finish();
    }

    @Test
    public void getCallingComponent_resultsManagerAvailable_returnsComponent() {
        ComponentName mockCallingComponent = new ComponentName("foo", "bar");
        ResultManager manager = mock(ResultManager.class);
        when(manager.getCallingComponent()).thenReturn(mockCallingComponent);
        mCarContext.getManagers().addFactory(ResultManager.class, null, () -> manager);

        ComponentName name = mCarContext.getCallingComponent();

        assertThat(name).isEqualTo(mockCallingComponent);
    }

    @Test
    public void getCallingComponent_resultsManagerNotAvailable_returnsNull() {
        ComponentName name = mCarContext.getCallingComponent();

        assertThat(name).isNull();
    }

    @Ignore // b/238635208
    @Test
    public void onConfigurationChanged_updatesTheConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        mCarContext.onCarConfigurationChanged(configuration);

        assertThat(mCarContext.getResources().getConfiguration().getLocales().get(0)).isEqualTo(
                Locale.CANADA_FRENCH);
    }

    @Test
    public void onConfigurationChanged_loadsCorrectNewResource() {
        Configuration ldpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        ldpiConfig.densityDpi = 120;

        mCarContext.onCarConfigurationChanged(ldpiConfig);

        Drawable ldpiDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(ldpiDrawable.getIntrinsicHeight()).isEqualTo(48);

        Configuration mdpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        mdpiConfig.densityDpi = 160;

        mCarContext.onCarConfigurationChanged(mdpiConfig);

        Drawable mdpiDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(mdpiDrawable.getIntrinsicHeight()).isEqualTo(64);

        Configuration hdpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        hdpiConfig.densityDpi = 240;

        mCarContext.onCarConfigurationChanged(hdpiConfig);

        Drawable hdpiDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(hdpiDrawable.getIntrinsicHeight()).isEqualTo(96);
    }

    @Test
    // TODO(rampara): Investigate removing usage of deprecated updateConfiguration API
    @SuppressWarnings("deprecation")
    public void changingApplicationContextConfiguration_doesNotChangeTheCarContextConfiguration() {
        Configuration ldpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        ldpiConfig.densityDpi = 120;

        mCarContext.onCarConfigurationChanged(ldpiConfig);

        Drawable ldpiDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(ldpiDrawable.getIntrinsicHeight()).isEqualTo(48);

        Configuration mdpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        mdpiConfig.densityDpi = 160;

        Context applicationContext = mCarContext.getApplicationContext();
        applicationContext.getResources().updateConfiguration(mdpiConfig,
                applicationContext.getResources().getDisplayMetrics());

        Drawable carContextDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(carContextDrawable.getIntrinsicHeight()).isEqualTo(48);

        Drawable applicationContextDrawable = TestUtils.getTestDrawable(applicationContext,
                "banana");
        assertThat(applicationContextDrawable.getIntrinsicHeight()).isEqualTo(64);
    }

    @Test
    // TODO(rampara): Investigate removing usage of deprecated updateConfiguration API
    @SuppressWarnings("deprecation")
    public void changingApplicationContextDisplayMetrics_doesNotChangeCarContextDisplayMetrics() {
        Configuration ldpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        ldpiConfig.densityDpi = 120;

        mCarContext.onCarConfigurationChanged(ldpiConfig);

        Drawable ldpiDrawable = TestUtils.getTestDrawable(mCarContext, "banana");
        assertThat(ldpiDrawable.getIntrinsicHeight()).isEqualTo(48);

        Configuration mdpiConfig = new Configuration(mCarContext.getResources().getConfiguration());
        mdpiConfig.densityDpi = 160;

        Context applicationContext = mCarContext.getApplicationContext();

        VirtualDisplay display = ((DisplayManager) applicationContext.getSystemService(
                Context.DISPLAY_SERVICE)).createVirtualDisplay("CarAppService",
                mdpiConfig.screenWidthDp, mdpiConfig.screenHeightDp, 5, null,
                VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
        DisplayMetrics newDisplayMetrics = new DisplayMetrics();
        display.getDisplay().getMetrics(newDisplayMetrics);

        applicationContext.getResources().updateConfiguration(mdpiConfig, newDisplayMetrics);

        assertThat(applicationContext.getResources().getConfiguration()).isEqualTo(mdpiConfig);
        assertThat(applicationContext.getResources().getDisplayMetrics()).isEqualTo(
                newDisplayMetrics);

        assertThat(mCarContext.getResources().getConfiguration()).isNotEqualTo(mdpiConfig);
        assertThat(mCarContext.getResources().getDisplayMetrics()).isNotEqualTo(newDisplayMetrics);
    }

    @Test
    public void isDarkMode_returnsExpectedValue() {
        assertThat(mCarContext.isDarkMode()).isFalse();

        mCarContext.getResources().getConfiguration().uiMode = Configuration.UI_MODE_NIGHT_YES;

        assertThat(mCarContext.isDarkMode()).isTrue();
    }

    private static class TestStartCarAppStub extends IStartCarApp.Stub {
        private final IStartCarApp mMockableStub;

        private TestStartCarAppStub(IStartCarApp mockableStub) {
            this.mMockableStub = mockableStub;
        }

        @Override
        public void startCarApp(Intent startCarAppIntent) throws RemoteException {
            mMockableStub.startCarApp(startCarAppIntent);
        }
    }

    @Test
    public void getOnBackPressedDispatcher_noListeners_popsAScreen() {
        mCarContext.getCarService(ScreenManager.class).push(mScreen1);
        mCarContext.getCarService(ScreenManager.class).push(mScreen2);

        mCarContext.getOnBackPressedDispatcher().onBackPressed();

        verify(mMockScreen1, never()).dispatchLifecycleEvent(Event.ON_DESTROY);
        verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
    }

    @Test
    public void getOnBackPressedDispatcher_withListenerThatIsStarted_callsListenerAndDoesNotPop() {
        mCarContext.getCarService(ScreenManager.class).push(mScreen1);
        mCarContext.getCarService(ScreenManager.class).push(mScreen2);

        OnBackPressedCallback callback = mock(OnBackPressedCallback.class);
        when(callback.isEnabled()).thenReturn(true);

        TestLifecycleOwner callbackLifecycle = new TestLifecycleOwner();
        callbackLifecycle.mRegistry.setCurrentState(State.STARTED);
        mCarContext.getOnBackPressedDispatcher().addCallback(callbackLifecycle, callback);
        mCarContext.getOnBackPressedDispatcher().onBackPressed();

        verify(callback).handleOnBackPressed();
        verify(mMockScreen1, never()).dispatchLifecycleEvent(Event.ON_DESTROY);
        verify(mMockScreen2, never()).dispatchLifecycleEvent(Event.ON_DESTROY);
    }

    @Test
    public void getOnBackPressedDispatcher_withAListenerThatIsNotStarted_popsAScreen() {
        mCarContext.getCarService(ScreenManager.class).push(mScreen1);
        mCarContext.getCarService(ScreenManager.class).push(mScreen2);

        OnBackPressedCallback callback = mock(OnBackPressedCallback.class);
        when(callback.isEnabled()).thenReturn(true);

        TestLifecycleOwner callbackLifecycle = new TestLifecycleOwner();
        callbackLifecycle.mRegistry.setCurrentState(State.CREATED);
        mCarContext.getOnBackPressedDispatcher().addCallback(callbackLifecycle, callback);
        mCarContext.getOnBackPressedDispatcher().onBackPressed();

        verify(callback, never()).handleOnBackPressed();
        verify(mMockScreen1, never()).dispatchLifecycleEvent(Event.ON_DESTROY);
        verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
    }

    @Test
    public void getOnBackPressedDispatcher_callsDefaultListenerWheneverTheAddedOneIsNotSTARTED() {
        mCarContext.getCarService(ScreenManager.class).push(mScreen1);
        mCarContext.getCarService(ScreenManager.class).push(mScreen2);

        OnBackPressedCallback callback = mock(OnBackPressedCallback.class);
        when(callback.isEnabled()).thenReturn(true);

        TestLifecycleOwner callbackLifecycle = new TestLifecycleOwner();
        callbackLifecycle.mRegistry.setCurrentState(State.CREATED);
        mCarContext.getOnBackPressedDispatcher().addCallback(callbackLifecycle, callback);
        mCarContext.getOnBackPressedDispatcher().onBackPressed();

        verify(callback, never()).handleOnBackPressed();
        verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);

        callbackLifecycle.mRegistry.setCurrentState(State.STARTED);
        mCarContext.getOnBackPressedDispatcher().onBackPressed();

        verify(callback).handleOnBackPressed();
    }

    @Test
    public void lifecycleDestroyed_removesHostBinders()
            throws ReflectiveOperationException, RemoteException {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_CREATE);
        Field field = CarContext.class.getDeclaredField("mHostDispatcher");
        field.setAccessible(true);
        HostDispatcher hostDispatcher = (HostDispatcher) field.get(mCarContext);

        assertThat(hostDispatcher.getHost(CarContext.APP_SERVICE)).isNotNull();

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        assertThat(hostDispatcher.getHost(CarContext.APP_SERVICE)).isNull();
    }

    @Test
    public void requestPermissions_startsTheExpectedActivity() throws RemoteException {
        List<String> permissions = new ArrayList<>();
        permissions.add("foo");
        permissions.add("bar");

        OnRequestPermissionsListener listener = mock(OnRequestPermissionsListener.class);

        mLifecycleOwner.mRegistry.setCurrentState(State.CREATED);
        mCarContext.requestPermissions(permissions, Runnable::run, listener);

        ShadowApplication sa = shadowOf((Application) ApplicationProvider.getApplicationContext());
        Intent startActivityIntent = sa.getNextStartedActivity();

        assertThat(startActivityIntent.getAction()).isEqualTo(
                CarContext.REQUEST_PERMISSIONS_ACTION);
        assertThat(startActivityIntent.getComponent()).isEqualTo(
                new ComponentName(mCarContext, CarAppPermissionActivity.class));

        Bundle extras = startActivityIntent.getExtras();

        assertThat(extras.getStringArray(CarContext.EXTRA_PERMISSIONS_KEY)).isEqualTo(
                permissions.toArray(new String[0]));

        IBinder binder = extras.getBinder(
                CarContext.EXTRA_ON_REQUEST_PERMISSIONS_RESULT_LISTENER_KEY);

        IOnRequestPermissionsListener iListener = IOnRequestPermissionsListener.Stub.asInterface(
                binder);
        iListener.onRequestPermissionsResult(new String[]{"foo"}, new String[]{"bar"});

        List<String> approved = new ArrayList<>();
        approved.add("foo");

        List<String> rejected = new ArrayList<>();
        rejected.add("bar");

        verify(listener).onRequestPermissionsResult(approved, rejected);
    }
}
