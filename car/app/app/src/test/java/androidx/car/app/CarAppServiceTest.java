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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.RemoteException;

import androidx.annotation.NonNull;
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
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Deque;
import java.util.Locale;


/** Tests for {@link CarAppService} and related classes for establishing a host connection. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class CarAppServiceTest {
    @Mock
    ICarHost mMockCarHost;
    @Mock
    DefaultLifecycleObserver mLifecycleObserver;

    private TestCarContext mCarContext;
    private final Template mTemplate =
            new PlaceListMapTemplate.Builder()
                    .setTitle("Title")
                    .setItemList(new ItemList.Builder().build())
                    .build();

    private CarAppService mCarAppService;
    private Intent mIntentSet;
    @Captor
    ArgumentCaptor<Bundleable> mBundleableArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCarAppService =
                new CarAppService() {
                    @Override
                    @NonNull
                    public HostValidator createHostValidator() {
                        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
                    }

                    @Override
                    @NonNull
                    public Session onCreateSession() {
                        // Recreate a new CarContext, because the previous one would have been
                        // destroyed in an unbind-rebind scenario.
                        mCarContext = TestCarContext.createCarContext(
                                ApplicationProvider.getApplicationContext());
                        Session session = createTestSession();
                        SessionController.of(session, mCarContext);
                        return session;
                    }
                };

        // Sets a default handshake info. OnAppCreate depends on this being non-null.
        String hostPackageName = "com.google.projection.gearhead";
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName,
                CarAppApiLevels.getLatest());
        mCarAppService.setHandshakeInfo(handshakeInfo);
    }

    private Session createTestSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                mIntentSet = intent;
                return new Screen(getCarContext()) {
                    @Override
                    @NonNull
                    public Template onGetTemplate() {
                        return mTemplate;
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
    public void onAppCreate_updatesCarApiLevel() throws RemoteException, BundlerException {
        String hostPackageName = "com.google.projection.gearhead";
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        int hostApiLevel = CarAppApiLevels.LEVEL_1;
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, hostApiLevel);

        mCarAppService.setCurrentSession(null);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), mock(IOnDoneCallback.class));
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        assertThat(
                mCarAppService.getCurrentSession().getCarContext().getCarAppApiLevel()).isEqualTo(
                hostApiLevel);
    }

    @Test
    public void onAppCreate_createsFirstScreen() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        assertThat(
                mCarAppService
                        .getCurrentSession()
                        .getCarContext()
                        .getCarService(ScreenManager.class)
                        .getTopTemplate()
                        .getTemplate())
                .isInstanceOf(PlaceListMapTemplate.class);
    }

    @Test
    public void onAppCreate_withIntent_callsWithOnCreateScreenWithIntent() throws
            RemoteException {
        IOnDoneCallback callback = mock(IOnDoneCallback.class);
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        Intent intent = new Intent("Foo");
        carApp.onAppCreate(mMockCarHost, intent, new Configuration(), callback);

        assertThat(mIntentSet).isEqualTo(intent);
        verify(callback).onSuccess(any());
    }

    @Test
    public void onAppCreate_alreadyPreviouslyCreated_callsOnNewIntent() throws RemoteException {
        IOnDoneCallback callback = mock(IOnDoneCallback.class);

        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        Intent intent = new Intent("Foo");
        carApp.onAppCreate(mMockCarHost, intent, new Configuration(), callback);
        verify(callback).onSuccess(any());

        IOnDoneCallback callback2 = mock(IOnDoneCallback.class);
        Intent intent2 = new Intent("Foo2");
        carApp.onAppCreate(mMockCarHost, intent2, new Configuration(), callback2);

        assertThat(mIntentSet).isEqualTo(intent2);
        verify(callback2).onSuccess(any());
    }

    @Test
    public void onAppCreate_updatesTheConfiguration() throws RemoteException {
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, configuration, mock(IOnDoneCallback.class));

        assertThat(mCarContext.getResources().getConfiguration().getLocales().get(0))
                .isEqualTo(Locale.CANADA_FRENCH);
    }

    @Test
    public void onNewIntent_callsOnNewIntentWithIntent() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        Intent intent = new Intent("Foo");
        carApp.onAppCreate(mMockCarHost, intent, new Configuration(), mock(IOnDoneCallback.class));

        Intent intent2 = new Intent("Foo2");
        carApp.onNewIntent(intent2, mock(IOnDoneCallback.class));

        assertThat(mIntentSet).isEqualTo(intent2);
    }

    @Test
    public void getNavigationManager() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        assertThat(mCarAppService.getCurrentSession().getCarContext().getCarService(
                NavigationManager.class)).isNotNull();
    }

    @Test
    public void onConfigurationChanged_updatesTheConfiguration() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.setLocale(Locale.CANADA_FRENCH);

        carApp.onConfigurationChanged(configuration, mock(IOnDoneCallback.class));

        assertThat(mCarContext.getResources().getConfiguration().getLocales().get(0))
                .isEqualTo(Locale.CANADA_FRENCH);
    }

    @Test
    public void getAppInfo() throws RemoteException, BundlerException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        IOnDoneCallback callback = mock(IOnDoneCallback.class);

        carApp.getAppInfo(callback);

        verify(callback).onSuccess(mBundleableArgumentCaptor.capture());
        AppInfo receivedAppInfo = (AppInfo) mBundleableArgumentCaptor.getValue().get();
        assertThat(receivedAppInfo.getMinCarAppApiLevel())
                .isEqualTo(appInfo.getMinCarAppApiLevel());
        assertThat(receivedAppInfo.getLatestCarAppApiLevel())
                .isEqualTo(appInfo.getLatestCarAppApiLevel());
        assertThat(receivedAppInfo.getLibraryDisplayVersion()).isEqualTo(
                appInfo.getLibraryDisplayVersion());
    }

    @Test
    public void onHandshakeCompleted_updatesHostInfo()
            throws RemoteException, BundlerException, InterruptedException {
        String hostPackageName = "com.google.projection.gearhead";
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, CarAppApiLevels.LEVEL_1);

        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), mock(IOnDoneCallback.class));

        assertThat(mCarAppService.getHostInfo().getPackageName()).isEqualTo(hostPackageName);
    }

    @Test
    public void onHandshakeCompleted_updatesHandshakeInfo() throws RemoteException,
            BundlerException {
        String hostPackageName = "com.google.projection.gearhead";
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);

        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, CarAppApiLevels.LEVEL_1);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), mock(IOnDoneCallback.class));
        assertThat(mCarAppService.getHandshakeInfo()).isNotNull();
        assertThat(mCarAppService.getHandshakeInfo().getHostCarAppApiLevel()).isEqualTo(
                handshakeInfo.getHostCarAppApiLevel());
        assertThat(mCarAppService.getHandshakeInfo().getHostPackageName()).isEqualTo(
                handshakeInfo.getHostPackageName());
    }

    @Test
    public void onHandshakeCompleted_lowerThanMinApiLevel_throws() throws BundlerException,
            RemoteException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);

        HandshakeInfo handshakeInfo = new HandshakeInfo("bar",
                appInfo.getMinCarAppApiLevel() - 1);
        IOnDoneCallback callback = mock(IOnDoneCallback.class);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), callback);

        verify(callback).onFailure(any());
    }

    @Test
    public void onHandshakeCompleted_higherThanCurrentApiLevel_throws() throws BundlerException,
            RemoteException {
        AppInfo appInfo = new AppInfo(3, 4, "foo");
        mCarAppService.setAppInfo(appInfo);
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);

        HandshakeInfo handshakeInfo = new HandshakeInfo("bar",
                appInfo.getLatestCarAppApiLevel() + 1);
        IOnDoneCallback callback = mock(IOnDoneCallback.class);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), callback);

        verify(callback).onFailure(any());
    }

    @Test
    public void onUnbind_movesLifecycleStateToDestroyed() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        carApp.onAppStart(mock(IOnDoneCallback.class));

        mCarAppService.getCurrentSession().getLifecycle().addObserver(mLifecycleObserver);

        assertThat(mCarAppService.onUnbind(null)).isTrue();

        verify(mLifecycleObserver).onDestroy(any());
    }

    @Test
    public void onUnbind_rebind_callsOnCreateScreen() throws RemoteException, BundlerException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);

        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        carApp.onAppStart(mock(IOnDoneCallback.class));

        Session currentSession = mCarAppService.getCurrentSession();
        currentSession.getLifecycle().addObserver(mLifecycleObserver);
        assertThat(mCarAppService.onUnbind(null)).isTrue();

        verify(mLifecycleObserver).onDestroy(any());

        assertThat(currentSession.getCarContext().getCarService(
                ScreenManager.class).getScreenStack()).isEmpty();
        assertThat(mCarAppService.getCurrentSession()).isNull();

        String hostPackageName = "com.google.projection.gearhead";
        int hostApiLevel = CarAppApiLevels.LEVEL_1;
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName, hostApiLevel);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), mock(IOnDoneCallback.class));
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        currentSession = mCarAppService.getCurrentSession();
        assertThat(currentSession.getCarContext().getCarService(
                ScreenManager.class).getScreenStack()).hasSize(1);
    }

    @Test
    public void onUnbind_clearsScreenStack() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        Deque<Screen> screenStack =
                mCarAppService.getCurrentSession().getCarContext().getCarService(
                        ScreenManager.class).getScreenStack();
        assertThat(screenStack).hasSize(1);

        Screen screen = screenStack.getFirst();
        assertThat(screen.getLifecycle().getCurrentState()).isAtLeast(Lifecycle.State.CREATED);

        mCarAppService.onUnbind(null);

        assertThat(screenStack).isEmpty();
        assertThat(screen.getLifecycle().getCurrentState()).isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void finish() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        mCarAppService.getCurrentSession().getCarContext().finishCarApp();

        assertThat(mCarContext.hasCalledFinishCarApp()).isTrue();
    }

    @Test
    public void onNewIntent_callsSessionIntent() throws
            RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);

        // onAppCreate must be called first to create the Session before onNewIntent.
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        assertThat(mIntentSet).isNull();

        IOnDoneCallback callback = mock(IOnDoneCallback.class);
        Intent intent = new Intent("Foo");
        carApp.onNewIntent(intent, callback);

        assertThat(mIntentSet).isEqualTo(intent);
        verify(callback).onSuccess(any());
    }
}
