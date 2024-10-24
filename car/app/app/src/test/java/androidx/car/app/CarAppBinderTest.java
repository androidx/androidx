/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.car.app.SessionInfo.DEFAULT_SESSION_INFO;
import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.RemoteException;

import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.testing.SessionController;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Deque;
import java.util.Locale;

/** Tests for {@link CarAppBinder} and related classes for establishing a host connection. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarAppBinderTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final Template TEST_TEMPLATE = new PlaceListMapTemplate.Builder().setTitle(
            "Title").setItemList(new ItemList.Builder().build()).build();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private DefaultLifecycleObserver mLifecycleObserver;
    @Mock
    private IOnDoneCallback mMockOnDoneCallback;
    @Captor
    private ArgumentCaptor<Bundleable> mBundleableArgumentCaptor;

    private TestCarAppService mCarAppService;
    private CarAppBinder mCarAppBinder;
    private Intent mIntentSet;

    @Before
    public void setUp() {
        mCarAppService = new TestCarAppService();

        AppInfo appInfo = new AppInfo(CarAppApiLevels.getOldest(), CarAppApiLevels.getLatest(),
                "blah");
        mCarAppService.setAppInfo(appInfo);
        mCarAppBinder = new CarAppBinder(mCarAppService, DEFAULT_SESSION_INFO);
        mCarAppService.setBinder(DEFAULT_SESSION_INFO, mCarAppBinder);

        // Sets default handshake and host info. OnAppCreate depends on these being non-null.
        String hostPackageName = "com.google.projection.gearhead";
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName,
                CarAppApiLevels.getLatest());
        HostInfo hostInfo = new HostInfo(hostPackageName, 1);
        mCarAppService.setHostInfo(hostInfo);

        mCarAppBinder.setHandshakeInfo(handshakeInfo);
    }

    private Session createTestSession() {
        return new Session() {
            @Override
            public @NonNull Screen onCreateScreen(@NonNull Intent intent) {
                mIntentSet = intent;
                return new Screen(getCarContext()) {
                    @Override
                    public @NonNull Template onGetTemplate() {
                        return TEST_TEMPLATE;
                    }
                };
            }

            @Override
            public void onNewIntent(@NonNull Intent intent) {
                mIntentSet = intent;
            }
        };
    }

    @Test
    public void onAppCreate_updatesCarApiLevel() throws BundlerException {
        String hostPackageName = "com.google.projection.gearhead";
        int hostApiLevel = CarAppApiLevels.LEVEL_1;
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, hostApiLevel);

        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        assertThat(mCarAppBinder.getCurrentSession().getCarContext().getCarAppApiLevel()).isEqualTo(
                hostApiLevel);
    }

    @Test
    public void onAppCreate_updatesContextHostInfo() throws BundlerException {
        String differentHostPackage = "different.package.name";
        HandshakeInfo handshakeInfo = new HandshakeInfo(differentHostPackage,
                CarAppApiLevels.LEVEL_1);

        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        assertThat(mCarAppBinder.getCurrentSession().getCarContext().getHostInfo().getPackageName())
                .isEqualTo(differentHostPackage);
    }

    @Test
    public void onAppCreate_createsFirstScreen() {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        assertThat(mCarAppBinder.getCurrentSession().getCarContext().getCarService(
                ScreenManager.class).getTopTemplate().getTemplate()).isInstanceOf(
                PlaceListMapTemplate.class);
    }

    @Test
    public void onAppCreate_withIntent_callsWithOnCreateScreenWithIntent() throws RemoteException {
        Intent intent = new Intent("Foo");
        mCarAppBinder.onAppCreate(mMockCarHost, intent, new Configuration(), mMockOnDoneCallback);

        assertThat(mIntentSet).isEqualTo(intent);
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onAppCreate_alreadyPreviouslyCreated_callsOnNewIntent() throws RemoteException {
        IOnDoneCallback callback = mock(IOnDoneCallback.class);

        Intent intent = new Intent("Foo");
        mCarAppBinder.onAppCreate(mMockCarHost, intent, new Configuration(), callback);
        verify(callback).onSuccess(any());

        IOnDoneCallback callback2 = mock(IOnDoneCallback.class);
        Intent intent2 = new Intent("Foo2");
        mCarAppBinder.onAppCreate(mMockCarHost, intent2, new Configuration(), callback2);

        assertThat(mIntentSet).isEqualTo(intent2);
        verify(callback2).onSuccess(any());
    }

    @SuppressLint("NewApi")
    @Ignore // b/238635208
    @Test
    public void onAppCreate_updatesTheConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        mCarAppBinder.onAppCreate(mMockCarHost, null, configuration, mMockOnDoneCallback);

        assertThat(mCarAppService.mCarContext.getResources().getConfiguration().getLocales().get(
                0)).isEqualTo(Locale.CANADA_FRENCH);
    }

    @Test
    public void onNewIntent_callsOnNewIntentWithIntent() throws RemoteException {
        Intent intent = new Intent("Foo");
        mCarAppBinder.onAppCreate(mMockCarHost, intent, new Configuration(), mMockOnDoneCallback);

        Intent intent2 = new Intent("Foo2");
        mCarAppBinder.onNewIntent(intent2, mMockOnDoneCallback);

        assertThat(mIntentSet).isEqualTo(intent2);
        verify(mMockOnDoneCallback, times(2)).onSuccess(any());
    }

    @Test
    public void onNewIntent_lifecycleNotCreated_doesNotDispatch_sendsError()
            throws RemoteException {
        mCarAppBinder.onNewIntent(new Intent("Foo"), mMockOnDoneCallback);

        assertThat(mIntentSet).isNull();
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void getNavigationManager() {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        assertThat(mCarAppBinder.getCurrentSession().getCarContext().getCarService(
                NavigationManager.class)).isNotNull();
    }

    @Test
    public void onConfigurationChanged_lifecycleNotCreated_returnsAFailure()
            throws RemoteException {
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        mCarAppBinder.onConfigurationChanged(configuration, mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @SuppressLint("NewApi")
    @Ignore // b/238635208
    @Test
    public void onConfigurationChanged_updatesTheConfiguration() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));

        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        mCarAppBinder.onConfigurationChanged(configuration, mMockOnDoneCallback);

        assertThat(mCarAppService.mCarContext.getResources().getConfiguration().getLocales().get(
                0)).isEqualTo(Locale.CANADA_FRENCH);
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void getAppInfo() throws RemoteException, BundlerException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);

        mCarAppBinder.getAppInfo(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(mBundleableArgumentCaptor.capture());
        AppInfo receivedAppInfo = (AppInfo) mBundleableArgumentCaptor.getValue().get();
        assertThat(receivedAppInfo.getMinCarAppApiLevel()).isEqualTo(
                appInfo.getMinCarAppApiLevel());
        assertThat(receivedAppInfo.getLatestCarAppApiLevel()).isEqualTo(
                appInfo.getLatestCarAppApiLevel());
        assertThat(receivedAppInfo.getLibraryDisplayVersion()).isEqualTo(
                appInfo.getLibraryDisplayVersion());
    }

    @Test
    public void onHandshakeCompleted_updatesHostInfo() throws BundlerException {
        String hostPackageName = "com.google.projection.gearhead";
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, CarAppApiLevels.LEVEL_1);

        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        assertThat(mCarAppService.getHostInfo().getPackageName()).isEqualTo(hostPackageName);
    }

    @Test
    public void onHandshakeCompleted_updatesHandshakeInfo() throws BundlerException {
        String hostPackageName = "com.google.projection.gearhead";

        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, CarAppApiLevels.LEVEL_1);
        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);
        assertThat(mCarAppBinder.getHandshakeInfo()).isNotNull();
        assertThat(mCarAppBinder.getHandshakeInfo().getHostCarAppApiLevel()).isEqualTo(
                handshakeInfo.getHostCarAppApiLevel());
        assertThat(mCarAppBinder.getHandshakeInfo().getHostPackageName()).isEqualTo(
                handshakeInfo.getHostPackageName());
    }

    @Test
    public void onHandshakeCompleted_lowerThanMinApiLevel_throws()
            throws BundlerException, RemoteException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);

        HandshakeInfo handshakeInfo = new HandshakeInfo("bar", appInfo.getMinCarAppApiLevel() - 1);
        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onHandshakeCompleted_higherThanCurrentApiLevel_throws()
            throws BundlerException, RemoteException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);

        HandshakeInfo handshakeInfo = new HandshakeInfo("bar",
                appInfo.getLatestCarAppApiLevel() + 1);
        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void destroy_movesLifecycleStateToDestroyed() {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);
        mCarAppBinder.onAppStart(mMockOnDoneCallback);

        // TODO(b/184154464): this should use the public getLifecycle() after the public
        // LifecycleRegistry is properly hooked up to the TestLifecycleOwner.
        mCarAppBinder.getCurrentSession().getLifecycleInternal().addObserver(mLifecycleObserver);

        mCarAppBinder.destroy();

        verify(mLifecycleObserver).onDestroy(any());
    }

    @Test
    public void onDestroyLifecycle_onCreate_callsOnCreateScreen() throws BundlerException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);
        mCarAppBinder.onAppStart(mMockOnDoneCallback);

        Session currentSession = mCarAppBinder.getCurrentSession();
        // TODO(b/184154464): this should use the public getLifecycle() after the public
        // LifecycleRegistry is properly hooked up to the TestLifecycleOwner.
        currentSession.getLifecycleInternal().addObserver(mLifecycleObserver);
        mCarAppBinder.onDestroyLifecycle();

        verify(mLifecycleObserver).onDestroy(any());

        assertThat(currentSession.getCarContext().getCarService(
                ScreenManager.class).getScreenStackInternal()).isEmpty();
        assertThat(mCarAppBinder.getCurrentSession()).isNull();

        String hostPackageName = "com.google.projection.gearhead";
        int hostApiLevel = CarAppApiLevels.LEVEL_1;
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, hostApiLevel);
        mCarAppBinder.onHandshakeCompleted(Bundleable.create(handshakeInfo), mMockOnDoneCallback);
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        currentSession = mCarAppBinder.getCurrentSession();
        assertThat(currentSession.getCarContext().getCarService(
                ScreenManager.class).getScreenStackInternal()).hasSize(1);
    }

    @Test
    public void onDestroyLifecycle_clearsScreenStack() {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);

        Deque<Screen> screenStack = mCarAppBinder.getCurrentSession().getCarContext().getCarService(
                ScreenManager.class).getScreenStackInternal();
        assertThat(screenStack).hasSize(1);

        Screen screen = screenStack.getFirst();
        assertThat(screen.getLifecycle().getCurrentState()).isAtLeast(Lifecycle.State.CREATED);

        mCarAppBinder.onDestroyLifecycle();

        assertThat(screenStack).isEmpty();
        assertThat(screen.getLifecycle().getCurrentState()).isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void onNewIntent_callsSessionIntent() throws RemoteException {
        // onAppCreate must be called first to create the Session before onNewIntent.
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));
        assertThat(mIntentSet).isNull();

        Intent intent = new Intent("Foo");
        mCarAppBinder.onNewIntent(intent, mMockOnDoneCallback);

        assertThat(mIntentSet).isEqualTo(intent);
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onNewIntent_notAtLeastCreated_doesCallSessionIntent_sendsFailure()
            throws RemoteException {
        Intent intent = new Intent("Foo");
        mCarAppBinder.onNewIntent(intent, mMockOnDoneCallback);

        assertThat(mIntentSet).isNull();
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onNewIntent_destroyed_doesCallSessionIntent_sendsFailure() throws RemoteException {
        // onAppCreate must be called first to create the Session before onNewIntent.
        Intent intent1 = new Intent();
        mCarAppBinder.onAppCreate(mMockCarHost, intent1, new Configuration(),
                mock(IOnDoneCallback.class));
        assertThat(mIntentSet).isEqualTo(intent1);

        mCarAppService.mCarContext.getLifecycleOwner().mRegistry.handleLifecycleEvent(
                Lifecycle.Event.ON_DESTROY);

        Intent intent2 = new Intent("Foo");
        mCarAppBinder.onNewIntent(intent2, mMockOnDoneCallback);

        assertThat(mIntentSet).isEqualTo(intent1);
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onAppStart_movesLifecycle() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));

        mCarAppBinder.onAppStart(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.STARTED);
    }

    @Test
    public void onAppStart_notAtLeastCreated_doesNotMoveLifecycle_sendsFailure()
            throws RemoteException {
        mCarAppBinder.onAppStart(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onAppStart_destroyed_doesNotMoveLifecycle_sendsFailure() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));
        mCarAppService.mCarContext.getLifecycleOwner().mRegistry.setCurrentState(
                Lifecycle.State.DESTROYED);

        mCarAppBinder.onAppStart(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void onAppResume_movesLifecycle() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));

        mCarAppBinder.onAppResume(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.RESUMED);
    }

    @Test
    public void onAppResume_notAtLeastCreated_doesNotMoveLifecycle_sendsFailure()
            throws RemoteException {
        mCarAppBinder.onAppResume(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onAppResume_destroyed_doesNotMoveLifecycle_sendsFailure() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));
        mCarAppService.mCarContext.getLifecycleOwner().mRegistry.setCurrentState(
                Lifecycle.State.DESTROYED);

        mCarAppBinder.onAppResume(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void onAppPause_movesLifecycle() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));

        mCarAppBinder.onAppPause(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.STARTED);
    }

    @Test
    public void onAppPause_notAtLeastCreated_doesNotMoveLifecycle_sendsFailure()
            throws RemoteException {
        mCarAppBinder.onAppPause(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onAppPause_destroyed_doesNotMoveLifecycle_sendsFailure() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));
        mCarAppService.mCarContext.getLifecycleOwner().mRegistry.setCurrentState(
                Lifecycle.State.DESTROYED);

        mCarAppBinder.onAppPause(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void onAppStop_movesLifecycle() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));

        mCarAppBinder.onAppStop(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.CREATED);
    }

    @Test
    public void onAppStop_notAtLeastCreated_doesNotMoveLifecycle_sendsFailure()
            throws RemoteException {
        mCarAppBinder.onAppStop(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onAppStop_destroyed_doesNotMoveLifecycle_sendsFailure() throws RemoteException {
        mCarAppBinder.onAppCreate(mMockCarHost, null, new Configuration(),
                mock(IOnDoneCallback.class));
        mCarAppService.mCarContext.getLifecycleOwner().mRegistry.setCurrentState(
                Lifecycle.State.DESTROYED);

        mCarAppBinder.onAppStop(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
        assertThat(mCarAppService.mCarContext.getLifecycleOwner().mRegistry.getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void session_screen_lifecycleEvents_inCorrectOrder() throws RemoteException {
        // We have to manually create the Session here instead of rely on using ICarApp because
        // of two issues:
        // 1. If we inject a TestCarContext, it will overwrite a TestLifeCycleOwner instance that
        // is different than the one the ScreenManager used to register a listener to.
        // 2. If we don't inject a TestCarContext, the existing logic of ICarApp in CarAppService
        // throws a NPE when trying to update the configuration of the Session/CarContext.
        Session session = createTestSession();
        ((LifecycleRegistry) session.getLifecycleInternal()).handleLifecycleEvent(ON_CREATE);
        Screen screen = session.onCreateScreen(new Intent());
        session.getCarContext().getCarService(ScreenManager.class).push(screen);

        DefaultLifecycleObserver sessionObserver = mock(DefaultLifecycleObserver.class);
        DefaultLifecycleObserver screenObserver = mock(DefaultLifecycleObserver.class);
        session.getLifecycle().addObserver(sessionObserver);
        screen.getLifecycle().addObserver(screenObserver);

        ((LifecycleRegistry) session.getLifecycleInternal()).handleLifecycleEvent(ON_DESTROY);
        InOrder inOrder = inOrder(screenObserver, sessionObserver);
        inOrder.verify(sessionObserver).onCreate(any());
        inOrder.verify(screenObserver).onCreate(any());
        inOrder.verify(screenObserver).onDestroy(any());
        inOrder.verify(sessionObserver).onDestroy(any());
    }

    /** An implementation of {@link CarAppService} for testing. */
    private class TestCarAppService extends CarAppService {
        public @Nullable SessionController mSessionController;
        public @Nullable TestCarContext mCarContext;
        public @Nullable Session mSession;
        public @Nullable SessionInfo mSessionInfo;

        @Override
        public @NonNull HostValidator createHostValidator() {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }

        @Override
        public @NonNull Session onCreateSession(@NonNull SessionInfo sessionInfo) {
            // Recreate a new CarContext, because the previous one would have been
            // destroyed in an unbind-rebind scenario.
            mCarContext = TestCarContext.createCarContext(mContext);
            mSession = createTestSession();
            mSessionController = new SessionController(mSession, mCarContext, new Intent());

            mSessionInfo = sessionInfo;
            return mSession;
        }

        @SuppressWarnings("deprecation")
        @Override
        public @NonNull Session onCreateSession() {
            throw new RuntimeException("This method should never be called");
        }
    }
}
