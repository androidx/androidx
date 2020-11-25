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
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

/** Tests for {@link NavigationManager}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class NavigationManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private INavigationHost.Stub mMockNavHost;
    @Mock
    private NavigationManagerListener mNavigationListener;

    private final HostDispatcher mHostDispatcher = new HostDispatcher();
    private NavigationManager mNavigationManager;

    private final Destination mDestination =
            Destination.builder().setName("Home").setAddress("123 State Street").build();
    private final Step mStep = Step.builder("Straight Ahead").build();
    private final TravelEstimate mStepTravelEstimate =
            TravelEstimate.create(
                    Distance.create(/* displayDistance= */ 10, Distance.UNIT_KILOMETERS),
                    TimeUnit.HOURS.toSeconds(1),
                    createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific"));
    private final TravelEstimate mDestinationTravelEstimate =
            TravelEstimate.create(
                    Distance.create(/* displayDistance= */ 100, Distance.UNIT_KILOMETERS),
                    TimeUnit.HOURS.toSeconds(1),
                    createDateTimeWithZone("2020-04-14T16:57:00", "US/Pacific"));
    private static final String CURRENT_ROAD = "State St.";
    private final Trip mTrip =
            Trip.builder()
                    .addDestination(mDestination)
                    .addStep(mStep)
                    .addDestinationTravelEstimate(mDestinationTravelEstimate)
                    .addStepTravelEstimate(mStepTravelEstimate)
                    .setCurrentRoad(CURRENT_ROAD)
                    .build();

    @Before
    @UiThreadTest
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

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

        mNavigationManager = NavigationManager.create(mHostDispatcher);
    }

    @Test
    @UiThreadTest
    public void navigationStarted_sendState_navigationEnded() throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost);

        mNavigationManager.setListener(mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.updateTrip(mTrip);
        inOrder.verify(mMockNavHost).updateTrip(any(Bundleable.class));

        mNavigationManager.navigationEnded();
        inOrder.verify(mMockNavHost).navigationEnded();
    }

    @Test
    @UiThreadTest
    public void navigationStarted_noListenerSet() throws RemoteException {
        assertThrows(IllegalStateException.class, () -> mNavigationManager.navigationStarted());
    }

    @Test
    @UiThreadTest
    public void navigationStarted_multiple() throws RemoteException {

        mNavigationManager.setListener(mNavigationListener);
        mNavigationManager.navigationStarted();

        mNavigationManager.navigationStarted();
        verify(mMockNavHost).navigationStarted();
    }

    @Test
    @UiThreadTest
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
    @UiThreadTest
    public void stopNavigation_notNavigating() throws RemoteException {
        mNavigationManager.setListener(mNavigationListener);
        mNavigationManager.getIInterface().stopNavigation(mock(IOnDoneCallback.class));
        verify(mNavigationListener, never()).stopNavigation();
    }

    @Test
    @UiThreadTest
    public void stopNavigation_navigating_restart() throws RemoteException {
        InOrder inOrder = inOrder(mMockNavHost, mNavigationListener);

        mNavigationManager.setListener(mNavigationListener);
        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();

        mNavigationManager.getIInterface().stopNavigation(mock(IOnDoneCallback.class));
        inOrder.verify(mNavigationListener).stopNavigation();

        mNavigationManager.navigationStarted();
        inOrder.verify(mMockNavHost).navigationStarted();
    }

    @Test
    @UiThreadTest
    public void onAutoDriveEnabled_callsListener() {
        mNavigationManager.setListener(mNavigationListener);
        mNavigationManager.onAutoDriveEnabled();

        verify(mNavigationListener).onAutoDriveEnabled();
    }

    @Test
    @UiThreadTest
    public void onAutoDriveEnabledBeforeRegisteringListener_callsListener() {
        mNavigationManager.onAutoDriveEnabled();
        mNavigationManager.setListener(mNavigationListener);

        verify(mNavigationListener).onAutoDriveEnabled();
    }
}
