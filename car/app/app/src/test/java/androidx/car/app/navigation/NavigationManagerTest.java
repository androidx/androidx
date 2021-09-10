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

package androidx.car.app.navigation;

import static androidx.car.app.TestUtils.createDateTimeWithZone;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.testing.TestCarContext;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/** Tests for {@link NavigationManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class NavigationManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private INavigationHost.Stub mMockNavHost;
    @Mock
    private NavigationManagerCallback mNavigationListener;

    private final HostDispatcher mHostDispatcher = new HostDispatcher();
    private NavigationManager mNavigationManager;

    private final Destination mDestination =
            new Destination.Builder().setName("Home").setAddress("123 State Street").build();
    private final Step mStep = new Step.Builder("Straight Ahead").build();
    private final TravelEstimate mStepTravelEstimate =
            new TravelEstimate.Builder(
                    Distance.create(/* displayDistance= */ 10, Distance.UNIT_KILOMETERS),
                    createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific"))
                    .setRemainingTimeSeconds(TimeUnit.HOURS.toSeconds(1)).build();
    private final TravelEstimate mDestinationTravelEstimate =
            new TravelEstimate.Builder(
                    Distance.create(/* displayDistance= */ 100, Distance.UNIT_KILOMETERS),
                    createDateTimeWithZone("2020-04-14T16:57:00", "US/Pacific"))
                    .setRemainingTimeSeconds(TimeUnit.HOURS.toSeconds(1)).build();
    private static final String CURRENT_ROAD = "State St.";
    private final Trip mTrip =
            new Trip.Builder()
                    .addDestination(mDestination, mDestinationTravelEstimate)
                    .addStep(mStep, mStepTravelEstimate)
                    .setCurrentRoad(CURRENT_ROAD)
                    .build();
    private TestCarContext mTestCarContext;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mTestCarContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

        INavigationHost navHostStub =
                new INavigationHost.Stub() {
                    @Override
                    public void updateTrip(Bundleable trip) throws RemoteException {
                        mMockNavHost.updateTrip(trip);
                    }

                    @Override
                    public void navigationStarted() throws RemoteException {
                        mMockNavHost.navigationStarted();
                    }

                    @Override
                    public void navigationEnded() throws RemoteException {
                        mMockNavHost.navigationEnded();
                    }
                };
        when(mMockCarHost.getHost(any())).thenReturn(navHostStub.asBinder());

        mHostDispatcher.setCarHost(mMockCarHost);

        mNavigationManager = NavigationManager.create(mTestCarContext, mHostDispatcher,
                mTestCarContext.getLifecycleOwner().mRegistry);
    }

    @Test
    public void navigationStarted_sendState_navigationEnded() throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost);

        mNavigationManager.setNavigationManagerCallback(mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.updateTrip(mTrip);
        inOrder.verify(mMockNavHost).updateTrip(any(Bundleable.class));

        mNavigationManager.navigationEnded();
        inOrder.verify(mMockNavHost).navigationEnded();
    }

    @Test
    public void navigationStarted_noListenerSet() throws RemoteException {
        assertThrows(IllegalStateException.class, () -> mNavigationManager.navigationStarted());
    }

    @Test
    public void navigationStarted_multiple() throws RemoteException {

        mNavigationManager.setNavigationManagerCallback(mNavigationListener);
        mNavigationManager.navigationStarted();

        mNavigationManager.navigationStarted();
        verify(mMockNavHost).navigationStarted();
    }

    @Test
    public void navigationEnded_multiple_not_started() throws RemoteException {
        mNavigationManager.navigationEnded();
        mNavigationManager.navigationEnded();
        mNavigationManager.navigationEnded();
        verify(mMockNavHost, never()).navigationEnded();
    }

    @Test
    public void sendNavigationState_notStarted() throws RemoteException {
        assertThrows(IllegalStateException.class, () -> mNavigationManager.updateTrip(mTrip));
    }

    @Test
    public void lifecycleDestroyed_callsOnStopNavigation() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mNavigationManager.setNavigationManagerCallback(new SynchronousExecutor(),
                mNavigationListener);
        mNavigationManager.navigationStarted();
        verify(mMockNavHost).navigationStarted();

        mTestCarContext.getLifecycleOwner().mRegistry.handleLifecycleEvent(
                Lifecycle.Event.ON_DESTROY);

        verify(mNavigationListener).onStopNavigation();
    }

    @Test
    public void onStopNavigation_notNavigating() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        mNavigationManager.setNavigationManagerCallback(mNavigationListener);

        mNavigationManager.getIInterface().onStopNavigation(mock(IOnDoneCallback.class));

        verify(mNavigationListener, never()).onStopNavigation();
    }

    @Test
    public void onStopNavigation_lifecycleNotCreated_doesNotDispatch() throws RemoteException {
        mNavigationManager.setNavigationManagerCallback(mNavigationListener);
        mNavigationManager.navigationStarted();
        verify(mMockNavHost).navigationStarted();

        mNavigationManager.getIInterface().onStopNavigation(mock(IOnDoneCallback.class));

        verify(mNavigationListener, never()).onStopNavigation();
    }

    @Test
    public void onStopNavigation_navigating_restart() throws RemoteException {
        mTestCarContext.getLifecycleOwner().mRegistry.setCurrentState(Lifecycle.State.CREATED);
        InOrder inOrder = inOrder(mMockNavHost, mNavigationListener);

        mNavigationManager.setNavigationManagerCallback(new SynchronousExecutor(),
                mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.getIInterface().onStopNavigation(mock(IOnDoneCallback.class));

        inOrder.verify(mNavigationListener).onStopNavigation();

        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();
    }

    @Test
    public void onStopNavigation_noListener_doesNotThrow() throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost, mNavigationListener);

        mNavigationManager.setNavigationManagerCallback(new SynchronousExecutor(),
                mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.onStopNavigation();
        mNavigationManager.clearNavigationManagerCallback();
        inOrder.verify(mNavigationListener).onStopNavigation();

        mNavigationManager.getIInterface().onStopNavigation(mock(IOnDoneCallback.class));

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void onStopNavigation_asynchronousCallback_callsIt() throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost, mNavigationListener);

        AsynchronousExecutor executor = new AsynchronousExecutor();
        mNavigationManager.setNavigationManagerCallback(executor,
                mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.onStopNavigation();
        executor.run();

        inOrder.verify(mNavigationListener).onStopNavigation();
    }

    @Test
    public void onStopNavigation_asynchronousCallbackClearedBeforeExecution_doesNotCallIt()
            throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost, mNavigationListener);

        AsynchronousExecutor executor = new AsynchronousExecutor();
        mNavigationManager.setNavigationManagerCallback(executor,
                mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.onStopNavigation();
        mNavigationManager.clearNavigationManagerCallback();
        executor.run();

        inOrder.verify(mNavigationListener, never()).onStopNavigation();
    }

    @Test
    public void onAutoDriveEnabled_callsListener() {
        mNavigationManager.setNavigationManagerCallback(new SynchronousExecutor(),
                mNavigationListener);
        mNavigationManager.onAutoDriveEnabled();

        verify(mNavigationListener).onAutoDriveEnabled();
    }

    @Test
    public void onAutoDriveEnabledBeforeRegisteringListener_callsListener() {
        mNavigationManager.onAutoDriveEnabled();
        mNavigationManager.setNavigationManagerCallback(new SynchronousExecutor(),
                mNavigationListener);

        verify(mNavigationListener).onAutoDriveEnabled();
    }

    static class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            r.run();
        }
    }

    static class AsynchronousExecutor implements Executor {
        private Runnable mToRun;

        @Override
        public void execute(Runnable r) {
            mToRun = r;
        }

        void run() {
            if (mToRun != null) {
                mToRun.run();
            }
        }
    }

}