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

package androidx.car.app.testing.navigation;

import androidx.car.app.HostDispatcher;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The {@link NavigationManager} that is used for testing.
 *
 * <p>This class will track the following usages of the {@link NavigationManager} throughout your
 * test:
 *
 * <ul>
 *   <li>All the {@link Trip}s sent via {@link NavigationManager#updateTrip}.
 *   <li>All the {@link NavigationManagerCallback}s set via
 *   {@link NavigationManager#setNavigationManagerCallback}.
 *   <li>Count of times that the navigation was started via {@link
 *       NavigationManager#navigationStarted()}.
 *   <li>Count of times that the navigation was ended via {@link NavigationManager#navigationEnded}.
 * </ul>
 */
public class TestNavigationManager extends NavigationManager {
    private final List<Trip> mTripsSent = new ArrayList<>();
    private @Nullable NavigationManagerCallback mCallback;
    private int mNavigationStartedCount;
    private int mNavigationEndedCount;

    /** Resets the values tracked by this {@link TestNavigationManager}. */
    public void reset() {
        mTripsSent.clear();
        mCallback = null;
        mNavigationStartedCount = 0;
        mNavigationEndedCount = 0;
    }

    /**
     * Returns all the {@link Trip}s sent via {@link NavigationManager#updateTrip}.
     *
     * <p>The trips are stored in the order in which they were sent, where the first trip in the
     * list, is the first trip sent.
     *
     * <p>The trips will be stored until {@link #reset} is called.
     */
    public @NonNull List<Trip> getTripsSent() {
        return CollectionUtils.unmodifiableCopy(mTripsSent);
    }

    /**
     * Returns the callback set via {@link NavigationManager#setNavigationManagerCallback}.
     *
     * <p>The listener will be {@code null} if one was never set, or if
     * {@link NavigationManager#clearNavigationManagerCallback()}  or {@link #reset} was called.
     */
    public @Nullable NavigationManagerCallback getNavigationManagerCallback() {
        return mCallback;
    }

    /**
     * Returns the number of times that navigation was started via {@link
     * NavigationManager#navigationStarted()} since creation or the last call to {@link #reset}.
     */
    public int getNavigationStartedCount() {
        return mNavigationStartedCount;
    }

    /**
     * Returns the number of times that navigation was ended via {@link
     * NavigationManager#navigationEnded()} since creation or the last call to {@link #reset}.
     */
    public int getNavigationEndedCount() {
        return mNavigationEndedCount;
    }

    @Override
    public void updateTrip(@NonNull Trip trip) {
        mTripsSent.add(trip);
        super.updateTrip(trip);
    }

    @Override
    public void setNavigationManagerCallback(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull NavigationManagerCallback callback) {
        mCallback = callback;
        super.setNavigationManagerCallback(executor, callback);
    }

    @Override
    public void clearNavigationManagerCallback() {
        mCallback = null;
        super.clearNavigationManagerCallback();
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

    public TestNavigationManager(@NonNull TestCarContext testCarContext,
            @NonNull HostDispatcher hostDispatcher) {
        super(testCarContext, hostDispatcher, testCarContext.getLifecycleOwner().mRegistry);
    }
}
