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

package androidx.car.app.testing.navigation;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.HostDispatcher;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerListener;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.testing.navigation.model.TripController;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link NavigationManager} that is used for testing.
 *
 * <p>This class will track the following usages of the {@link NavigationManager} throughout your
 * test:
 *
 * <ul>
 *   <li>All the {@link Trip}s sent via {@link NavigationManager#updateTrip}.
 *   <li>All the {@link NavigationManagerListener}s set via {@link NavigationManager#setListener}.
 *   <li>Count of times that the navigation was started via {@link
 *       NavigationManager#navigationStarted()}.
 *   <li>Count of times that the navigation was ended via {@link NavigationManager#navigationEnded}.
 * </ul>
 */
public class TestNavigationManager extends NavigationManager {
    private final List<TripController> mTripsSent = new ArrayList<>();
    private final List<NavigationManagerListener> mListenersSet = new ArrayList<>();
    private int mNavigationStartedCount;
    private int mNavigationEndedCount;

    /** Resets the values tracked by this {@link TestNavigationManager}. */
    public void reset() {
        mTripsSent.clear();
        mListenersSet.clear();
        mNavigationStartedCount = 0;
        mNavigationEndedCount = 0;
    }

    /**
     * Retrieves all the {@link Trip}s sent via {@link NavigationManager#updateTrip}.
     *
     * <p>The trips are stored in order of calls.
     *
     * <p>The trips will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<TripController> getTripsSent() {
        return mTripsSent;
    }

    /**
     * Retrieves all the {@link NavigationManagerListener}s added via {@link
     * NavigationManager#setListener(NavigationManagerListener)}.
     *
     * <p>The listeners are stored in order of calls.
     *
     * <p>The listeners will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<NavigationManagerListener> getNavigationManagerListenersSet() {
        return mListenersSet;
    }

    /**
     * Retrieves the number of times that navigation was started via {@link
     * NavigationManager#navigationStarted()} since the creation or the last call to {@link #reset}.
     */
    public int getNavigationStartedCount() {
        return mNavigationStartedCount;
    }

    /**
     * Retrieves the number of times that navigation was ended via {@link
     * NavigationManager#navigationEnded()} since the creation or the last call to {@link #reset}.
     */
    public int getNavigationEndedCount() {
        return mNavigationEndedCount;
    }

    @Override
    public void updateTrip(@NonNull Trip trip) {
        mTripsSent.add(TripController.of(requireNonNull(trip)));
        super.updateTrip(trip);
    }

    @Override
    public void setListener(@Nullable NavigationManagerListener listener) {
        mListenersSet.add(listener);
        super.setListener(listener);
    }

    @Override
    public void navigationStarted() {
        mNavigationStartedCount++;
        super.navigationStarted();
    }

    @Override
    public void navigationEnded() {
        mNavigationEndedCount++;
        super.navigationEnded();
    }

    public TestNavigationManager(HostDispatcher hostDispatcher) {
        super(hostDispatcher);
    }
}
