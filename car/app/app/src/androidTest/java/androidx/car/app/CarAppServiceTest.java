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
import androidx.car.app.testing.CarAppServiceController;
import androidx.car.app.testing.TestCarContext;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Deque;
import java.util.Locale;

/** Tests for {@link CarAppService}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CarAppServiceTest {
    @Mock
    ICarHost mMockCarHost;
    @Mock
    DefaultLifecycleObserver mLifecycleObserver;

    private TestCarContext mCarContext;
    private final Template mTemplate =
            PlaceListMapTemplate.builder()
                    .setTitle("Title")
                    .setItemList(ItemList.builder().build())
                    .build();

    private CarAppService mCarAppService;

    private Intent mIntentSet;
    private boolean mHasCarAppFinished;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mCarContext = TestCarContext.createCarContext(
                ApplicationProvider.getApplicationContext());

        mCarAppService =
                new CarAppService() {
                    @Override
                    @NonNull
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
                    public void onCarAppFinished() {
                        mHasCarAppFinished = true;
                    }

                    @Override
                    public void onNewIntent(@NonNull Intent intent) {
                        mIntentSet = intent;
                    }
                };

        CarAppServiceController.of(mCarContext, mCarAppService);
        mCarAppService.onCreate();
    }

    @Test
    @UiThreadTest
    public void onAppCreate_createsFirstScreen() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        assertThat(
                mCarAppService
                        .getCarContext()
                        .getCarService(ScreenManager.class)
                        .getTopTemplate()
                        .getTemplate())
                .isInstanceOf(PlaceListMapTemplate.class);
    }

    @Test
    @UiThreadTest
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
    @UiThreadTest
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
    @UiThreadTest
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
    @UiThreadTest
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

        assertThat(
                mCarAppService.getCarContext().getCarService(NavigationManager.class)).isNotNull();
    }

    @Test
    @UiThreadTest
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
    public void onHandshakeCompleted_updatesHostInfo() throws RemoteException, BundlerException {
        String hostPackageName = "com.google.projection.gearhead";
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        HandshakeInfo handshakeInfo = new HandshakeInfo(hostPackageName);
        carApp.onHandshakeCompleted(Bundleable.create(handshakeInfo), mock(IOnDoneCallback
                .class));
        assertThat(mCarAppService.getHostInfo().getPackageName()).isEqualTo(hostPackageName);
    }

    @Test
    @UiThreadTest
    public void onUnbind_movesLifecycleStateToStopped() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        carApp.onAppStart(mock(IOnDoneCallback.class));

        mCarAppService.getLifecycle().addObserver(mLifecycleObserver);

        assertThat(mCarAppService.onUnbind(null)).isTrue();

        verify(mLifecycleObserver).onStop(any());
    }

    @Test
    @UiThreadTest
    public void onUnbind_rebind_callsOnCreateScreen() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        carApp.onAppStart(mock(IOnDoneCallback.class));

        mCarAppService.getLifecycle().addObserver(mLifecycleObserver);

        assertThat(mCarAppService.onUnbind(null)).isTrue();
        assertThat(mHasCarAppFinished).isTrue();

        verify(mLifecycleObserver).onStop(any());

        assertThat(
                mCarAppService.getCarContext().getCarService(ScreenManager.class).getScreenStack())
                .isEmpty();

        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));
        assertThat(
                mCarAppService.getCarContext().getCarService(ScreenManager.class).getScreenStack())
                .hasSize(1);
    }

    @Test
    @UiThreadTest
    public void onUnbind_clearsScreenStack() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        Deque<Screen> screenStack =
                mCarAppService.getCarContext().getCarService(ScreenManager.class).getScreenStack();
        assertThat(screenStack).hasSize(1);

        Screen screen = screenStack.getFirst();
        assertThat(screen.getLifecycle().getCurrentState()).isAtLeast(Lifecycle.State.CREATED);

        mCarAppService.onUnbind(null);

        assertThat(screenStack).isEmpty();
        assertThat(screen.getLifecycle().getCurrentState()).isEqualTo(Lifecycle.State.DESTROYED);
        assertThat(mHasCarAppFinished).isTrue();
    }

    @Test
    @UiThreadTest
    public void finish() throws RemoteException {
        ICarApp carApp = (ICarApp) mCarAppService.onBind(null);
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mock(IOnDoneCallback.class));

        mCarAppService.finish();

        assertThat(mCarContext.hasCalledFinishCarApp()).isTrue();
    }
}
