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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.graphics.Rect;
import android.location.Location;
import android.os.RemoteException;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.Template;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.testing.TestCarContext;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;

/** Tests for {@link AppManager}. */
@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = { "androidx.activity" })
@DoNotInstrument
public final class AppManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IAppHost.Stub mMockAppHost;
    @Mock
    private IOnDoneCallback mMockOnDoneCallback;
    @Mock
    private OnBackPressedCallback mMockOnBackPressedCallback;
    @Mock
    private SurfaceCallback mSurfaceCallback;

    @Captor
    private ArgumentCaptor<ISurfaceCallback> mSurfaceCallbackCaptor;

    private Application mApplication;
    private TestCarContext mTestCarContext;
    private final HostDispatcher mHostDispatcher = new HostDispatcher();

    private AppManager mAppManager;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mApplication = ApplicationProvider.getApplicationContext();
        mTestCarContext = TestCarContext.createCarContext(mApplication);

        IAppHost appHost =
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
                    public void setSurfaceCallback(@Nullable ISurfaceCallback surfaceCallback)
                            throws RemoteException {
                        mMockAppHost.setSurfaceCallback(surfaceCallback);
                    }

                    @Override
                    public void sendLocation(Location location) throws RemoteException {
                        mMockAppHost.sendLocation(location);
                    }
                };
        when(mMockCarHost.getHost(any())).thenReturn(appHost.asBinder());

        mHostDispatcher.setCarHost(mMockCarHost);

        mAppManager = AppManager.create(mTestCarContext, mHostDispatcher,
                mTestCarContext.getLifecycleOwner().mRegistry);
    }

    @Test
    public void getTemplate_lifecycleCreated_sendsToApp() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mTestCarContext
                .getCarService(ScreenManager.class)
                .push(new Screen(mTestCarContext) {
                    @NonNull
                    @Override
                    public Template onGetTemplate() {
                        return new TestTemplate();
                    }
                });

        mAppManager.getIInterface().getTemplate(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void getTemplate_lifecycleNotCreated_doesNotSendToApp() throws RemoteException {
        mTestCarContext
                .getCarService(ScreenManager.class)
                .push(new Screen(mTestCarContext) {
                    @NonNull
                    @Override
                    public Template onGetTemplate() {
                        return new TestTemplate() {
                        };
                    }
                });

        mAppManager.getIInterface().getTemplate(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void getTemplate_serializationFails_sendsFailureToHost()
            throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mTestCarContext
                .getCarService(ScreenManager.class)
                .push(new Screen(mTestCarContext) {
                    @NonNull
                    @Override
                    public Template onGetTemplate() {
                        return new NonBundleableTemplate("foo") {
                        };
                    }
                });

        mAppManager.getIInterface().getTemplate(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onBackPressed_lifecycleCreated_sendsToApp() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        when(mMockOnBackPressedCallback.isEnabled()).thenReturn(true);
        mTestCarContext.getOnBackPressedDispatcher().addCallback(mMockOnBackPressedCallback);

        mAppManager.getIInterface().onBackPressed(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
        verify(mMockOnBackPressedCallback).handleOnBackPressed();
    }

    @Test
    public void onBackPressed_lifecycleNotCreated_doesNotSendToApp() throws RemoteException {
        when(mMockOnBackPressedCallback.isEnabled()).thenReturn(true);
        mTestCarContext.getOnBackPressedDispatcher().addCallback(mMockOnBackPressedCallback);

        mAppManager.getIInterface().onBackPressed(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
        verify(mMockOnBackPressedCallback, never()).handleOnBackPressed();
    }

    @Test
    public void invalidate_forwardsRequestToHost() throws RemoteException {
        mAppManager.invalidate();

        verify(mMockAppHost).invalidate();
    }

    @Test
    public void invalidate_hostThrowsRemoteException_doesNotThrow() throws
            RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).invalidate();

        mAppManager.invalidate();
    }

    @Test
    public void invalidate_hostThrowsRuntimeException_throwsHostException() throws
            RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).invalidate();

        assertThrows(HostException.class, () -> mAppManager.invalidate());
    }

    @Test
    public void showToast_forwardsRequestToHost() throws RemoteException {
        String text = "Toast";
        int duration = 10;
        mAppManager.showToast(text, duration);

        verify(mMockAppHost).showToast(text, duration);
    }

    @Test
    public void showToast_hostThrowsRemoteException_doesNotThrow() throws RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).showToast(anyString(), anyInt());

        mAppManager.showToast("foo", 1);
    }

    @Test
    public void showToast_hostThrowsRuntimeException_throwsHostException() throws
            RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).showToast(anyString(), anyInt());

        assertThrows(HostException.class, () -> mAppManager.showToast("foo", 1));
    }

    @Test
    public void setSurfaceListener_forwardsRequestToHost() throws RemoteException {
        mAppManager.setSurfaceCallback(null);

        verify(mMockAppHost).setSurfaceCallback(null);
    }

    @Test
    public void setSurfaceListener_hostThrowsSecurityException_throwsSecurityException()
            throws RemoteException {
        doThrow(new SecurityException()).when(mMockAppHost).setSurfaceCallback(any());

        assertThrows(SecurityException.class, () -> mAppManager.setSurfaceCallback(null));
    }

    @Test
    public void etSurfaceListener_hostThrowsRemoteException_doesNotThrow()
            throws RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).setSurfaceCallback(any());

        mAppManager.setSurfaceCallback(null);
    }

    @Test
    public void setSurfaceListener_hostThrowsRuntimeException_throwsHostException()
            throws RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).setSurfaceCallback(any());

        assertThrows(HostException.class, () -> mAppManager.setSurfaceCallback(null));
    }

    @Test
    public void onSurfaceAvailable_dispatches()
            throws RemoteException, BundlerException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        SurfaceContainer surfaceContainer = new SurfaceContainer(null, 1, 2, 3);

        mSurfaceCallbackCaptor.getValue().onSurfaceAvailable(Bundleable.create(surfaceContainer),
                mMockOnDoneCallback);

        verify(mSurfaceCallback).onSurfaceAvailable(any());
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onSurfaceAvailable_lifecycleNotCreated_doesNotDispatch_sendsAFailure()
            throws RemoteException, BundlerException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        SurfaceContainer surfaceContainer = new SurfaceContainer(null, 1, 2, 3);

        mSurfaceCallbackCaptor.getValue().onSurfaceAvailable(Bundleable.create(surfaceContainer),
                mMockOnDoneCallback);

        verify(mSurfaceCallback, never()).onSurfaceAvailable(surfaceContainer);
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onVisibleAreaChanged_dispatches()
            throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        Rect rect = new Rect(0, 0, 1, 1);

        mSurfaceCallbackCaptor.getValue().onVisibleAreaChanged(rect, mMockOnDoneCallback);

        verify(mSurfaceCallback).onVisibleAreaChanged(rect);
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onVisibleAreaChanged_lifecycleNotCreated_doesNotDispatch_sendsAFailure()
            throws RemoteException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        Rect rect = new Rect(0, 0, 1, 1);

        mSurfaceCallbackCaptor.getValue().onVisibleAreaChanged(rect, mMockOnDoneCallback);

        verify(mSurfaceCallback, never()).onVisibleAreaChanged(any());
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onStableAreaChanged_dispatches()
            throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        Rect rect = new Rect(0, 0, 1, 1);

        mSurfaceCallbackCaptor.getValue().onStableAreaChanged(rect, mMockOnDoneCallback);

        verify(mSurfaceCallback).onStableAreaChanged(rect);
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onStableAreaChanged_lifecycleNotCreated_doesNotDispatch_sendsAFailure()
            throws RemoteException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        Rect rect = new Rect(0, 0, 1, 1);

        mSurfaceCallbackCaptor.getValue().onStableAreaChanged(rect, mMockOnDoneCallback);

        verify(mSurfaceCallback, never()).onStableAreaChanged(any());
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onSurfaceDestroyed_dispatches()
            throws RemoteException, BundlerException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        SurfaceContainer surfaceContainer = new SurfaceContainer(null, 1, 2, 3);

        mSurfaceCallbackCaptor.getValue().onSurfaceDestroyed(Bundleable.create(surfaceContainer),
                mMockOnDoneCallback);

        verify(mSurfaceCallback).onSurfaceDestroyed(any());
        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void onSurfaceDestroyed_lifecycleNotCreated_doesNotDispatch_sendsAFailure()
            throws RemoteException, BundlerException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());
        SurfaceContainer surfaceContainer = new SurfaceContainer(null, 1, 2, 3);

        mSurfaceCallbackCaptor.getValue().onSurfaceDestroyed(Bundleable.create(surfaceContainer),
                mMockOnDoneCallback);

        verify(mSurfaceCallback, never()).onSurfaceDestroyed(surfaceContainer);
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void onSurfaceScroll_dispatches()
            throws RemoteException, BundlerException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onScroll(1, 2);

        verify(mSurfaceCallback).onScroll(1, 2);
    }

    @Test
    public void onSurfaceScroll_lifecycleNotCreated_doesNotDispatch()
            throws RemoteException, BundlerException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onScroll(1, 2);

        verify(mSurfaceCallback, never()).onScroll(anyFloat(), anyFloat());
    }

    @Test
    public void onSurfaceFling_dispatches()
            throws RemoteException, BundlerException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onFling(1, 2);

        verify(mSurfaceCallback).onFling(1, 2);
    }

    @Test
    public void onSurfaceFling_lifecycleNotCreated_doesNotDispatch()
            throws RemoteException, BundlerException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onFling(1, 2);

        verify(mSurfaceCallback, never()).onFling(anyFloat(), anyFloat());
    }

    @Test
    public void onSurfaceScale_dispatches()
            throws RemoteException, BundlerException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onScale(1, 2, 3);

        verify(mSurfaceCallback).onScale(1, 2, 3);
    }

    @Test
    public void onSurfaceScale_lifecycleNotCreated_doesNotDispatch()
            throws RemoteException, BundlerException {
        mAppManager.setSurfaceCallback(mSurfaceCallback);
        verify(mMockAppHost).setSurfaceCallback(mSurfaceCallbackCaptor.capture());

        mSurfaceCallbackCaptor.getValue().onScale(1, 2, 3);

        verify(mSurfaceCallback, never()).onScale(anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    public void startLocationUpdates_permissionNotGranted_failure() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.getIInterface().startLocationUpdates(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void startLocationUpdates_lifecycleCreated_sendsToApp() throws RemoteException {
        ShadowApplication app = shadowOf(mApplication);
        app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.getIInterface().startLocationUpdates(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void startLocationUpdates_lifecycleNotCreated_doesNotSendToApp() throws RemoteException {
        ShadowApplication app = shadowOf(mApplication);
        app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        mAppManager.getIInterface().startLocationUpdates(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void stopLocationUpdates_lifecycleCreated_sendsToApp() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mAppManager.getIInterface().stopLocationUpdates(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onSuccess(any());
    }

    @Test
    public void stopLocationUpdates_lifecycleNotCreated_doesNotSendToApp() throws RemoteException {
        mAppManager.getIInterface().stopLocationUpdates(mMockOnDoneCallback);

        verify(mMockOnDoneCallback).onFailure(any());
    }

    private static class NonBundleableTemplate implements Template {
        NonBundleableTemplate(String s) {
        }
    }

    private static class TestTemplate implements Template {
    }
}
