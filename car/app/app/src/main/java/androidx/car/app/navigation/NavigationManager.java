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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.LogTags.TAG_NAVIGATION_MANAGER;
import static androidx.car.app.utils.ThreadUtils.checkMainThread;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.managers.Manager;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.RemoteUtils;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.Executor;

/**
 * Manager for communicating navigation related events with the host.
 *
 * <p>Navigation apps must use this interface to coordinate with the car system for navigation
 * specific resources such as vehicle cluster and heads-up displays.
 *
 * <p>When a navigation app receives a user action to start navigating, it should call {@link
 * #navigationStarted()} to indicate it is currently navigating. When the app receives a user action
 * to end navigation or when the destination is reached, {@link #navigationEnded()} should be
 * called.
 *
 * <p>Navigation apps must also register a {@link NavigationManagerCallback} to handle callbacks to
 * {@link NavigationManagerCallback#onStopNavigation()} issued by the host.
 */
public class NavigationManager implements Manager {
    private final CarContext mCarContext;
    private final INavigationManager.Stub mNavigationManager;
    private final HostDispatcher mHostDispatcher;

    // Guarded by main thread access.
    @Nullable
    private NavigationManagerCallback mNavigationManagerCallback;
    @Nullable
    private Executor mNavigationManagerCallbackExecutor;
    private boolean mIsNavigating;
    private boolean mIsAutoDriveEnabled;

    /**
     * Sends the destinations, steps, and trip estimates to the host.
     *
     * <p>The data <b>may</b> be rendered at different places in the car such as the instrument
     * cluster screen or the heads-up display.
     *
     * <p>This method should only be invoked once the navigation app has called {@link
     * #navigationStarted()}, or else the updates will be dropped by the host. Once the app has
     * called {@link #navigationEnded()} or received
     * {@link NavigationManagerCallback#onStopNavigation()} it should stop sending updates.
     *
     * <p>As the location changes, and in accordance with speed and rounded distance changes, the
     * {@link TravelEstimate}s in the provided {@link Trip} should be rebuilt and this method called
     * again. For example, when the next step is greater than 10 kilometers away and the display
     * unit is kilometers, updates should occur roughly every kilometer.
     *
     * <p>Data provided to the cluster display depends on the vehicle capabilities. In some
     * instances the information may not be shown at all. On some vehicles {@link
     * androidx.car.app.navigation.model.Maneuver}s of unknown type may be skipped while on other
     * displays the associated icon may be shown.
     *
     * @param trip destination, steps, and trip estimates to be sent to the host
     * @throws HostException            if the call is invoked by an app that is not declared as
     *                                  a navigation app in the manifest
     * @throws IllegalStateException    if the call occurs when navigation is not started (see
     *                                  {@link #navigationStarted()} for more info), or if the
     *                                  current thread is not the main thread
     * @throws IllegalArgumentException if any of the destinations, steps, or trip position is
     *                                  not well formed
     */
    @MainThread
    public void updateTrip(@NonNull Trip trip) {
        checkMainThread();
        if (!mIsNavigating) {
            throw new IllegalStateException("Navigation is not started");
        }

        Bundleable bundle;
        try {
            bundle = Bundleable.create(trip);
        } catch (BundlerException e) {
            throw new IllegalArgumentException("Serialization failure", e);
        }

        mHostDispatcher.dispatch(
                CarContext.NAVIGATION_SERVICE,
                "updateTrip", (INavigationHost service) -> {
                    service.updateTrip(bundle);
                    return null;
                }
        );
    }

    /**
     * Sets a callback to start receiving navigation manager events.
     *
     * Note that the callback events will be executed on the main thread using
     * {@link Looper#getMainLooper()}. To specify the execution thread, use
     * {@link #setNavigationManagerCallback(Executor, NavigationManagerCallback)}.
     *
     * @param callback the {@link NavigationManagerCallback} to use
     * @throws IllegalStateException if the current thread is not the main thread
     */
    @SuppressLint("ExecutorRegistration")
    @MainThread
    public void setNavigationManagerCallback(@NonNull NavigationManagerCallback callback) {
        checkMainThread();
        Executor executor = ContextCompat.getMainExecutor(mCarContext);
        setNavigationManagerCallback(executor, callback);
    }

    /**
     * Sets a callback to start receiving navigation manager events.
     *
     * @param executor the executor which will be used for invoking the callback
     * @param callback the {@link NavigationManagerCallback} to use
     * @throws IllegalStateException if the current thread is not the main thread
     */
    @MainThread
    public void setNavigationManagerCallback(@NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull NavigationManagerCallback callback) {
        checkMainThread();

        mNavigationManagerCallbackExecutor = executor;
        mNavigationManagerCallback = callback;
        if (mIsAutoDriveEnabled) {
            onAutoDriveEnabled();
        }
    }

    /**
     * Clears the callback for receiving navigation manager events.
     *
     * @throws IllegalStateException if navigation is started (see {@link #navigationStarted()}
     *                               for more info), or if the current thread is not the main thread
     */
    @MainThread
    public void clearNavigationManagerCallback() {
        checkMainThread();
        if (mIsNavigating) {
            throw new IllegalStateException("Removing callback while navigating");
        }
        mNavigationManagerCallbackExecutor = null;
        mNavigationManagerCallback = null;
    }

    /**
     * Notifies the host that the app has started active navigation.
     *
     * <p>Only one app may be actively navigating in the car at any time and ownership is managed by
     * the host. The app must call this method to inform the system that it has started
     * navigation in response to user action.
     *
     * <p>This function can only called if
     * {@link #setNavigationManagerCallback(NavigationManagerCallback)} has been
     * called with a non-{@code null} value. The callback is required so that a signal to stop
     * navigation from the host can be handled using
     * {@link NavigationManagerCallback#onStopNavigation()}.
     *
     * <p>This method is idempotent.
     *
     * @throws IllegalStateException if no navigation manager callback has been set, or if the
     *                               current thread is not the main thread
     */
    @MainThread
    public void navigationStarted() {
        checkMainThread();
        if (mIsNavigating) {
            return;
        }
        if (mNavigationManagerCallback == null) {
            throw new IllegalStateException("No callback has been set");
        }
        mIsNavigating = true;
        mHostDispatcher.dispatch(
                CarContext.NAVIGATION_SERVICE,
                "navigationStarted", (INavigationHost service) -> {
                    service.navigationStarted();
                    return null;
                }
        );
    }

    /**
     * Notifies the host that the app has ended active navigation.
     *
     * <p>Only one app may be actively navigating in the car at any time and ownership is managed by
     * the host. The app must call this method to inform the system that it has ended navigation,
     * for example, in response to the user cancelling navigation or upon reaching the destination.
     *
     * <p>This method is idempotent.
     *
     * @throws IllegalStateException if the current thread is not the main thread
     */
    @MainThread
    public void navigationEnded() {
        checkMainThread();
        if (!mIsNavigating) {
            return;
        }
        mIsNavigating = false;
        mHostDispatcher.dispatch(
                CarContext.NAVIGATION_SERVICE,
                "navigationEnded", (INavigationHost service) -> {
                    service.navigationEnded();
                    return null;
                }
        );
    }

    /**
     * Creates an instance of {@link NavigationManager}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public static NavigationManager create(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        requireNonNull(carContext);
        requireNonNull(hostDispatcher);
        requireNonNull(lifecycle);

        return new NavigationManager(carContext, hostDispatcher, lifecycle);
    }

    /**
     * Returns the {@code INavigationManager.Stub} binder object.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public INavigationManager.Stub getIInterface() {
        return mNavigationManager;
    }

    /**
     * Tells the app to stop navigating.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @MainThread
    public void onStopNavigation() {
        checkMainThread();
        if (!mIsNavigating) {
            return;
        }
        mIsNavigating = false;

        if (mNavigationManagerCallbackExecutor == null) {
            return;
        }

        mNavigationManagerCallbackExecutor.execute(() -> {
            NavigationManagerCallback callback = mNavigationManagerCallback;
            if (callback != null) {
                callback.onStopNavigation();
            }
        });
    }

    /**
     * Signifies that from this point, until {@link CarContext#finishCarApp()} is called, any
     * navigation should automatically start driving to the destination as if the user was moving.
     *
     * <p>This is used in a testing environment, allowing testing the navigation app's navigation
     * capabilities without being in a car.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @MainThread
    public void onAutoDriveEnabled() {
        checkMainThread();
        if (Log.isLoggable(TAG_NAVIGATION_MANAGER, Log.DEBUG)) {
            Log.d(TAG_NAVIGATION_MANAGER, "Executing onAutoDriveEnabled");
        }

        mIsAutoDriveEnabled = true;

        NavigationManagerCallback callback = mNavigationManagerCallback;
        Executor executor = mNavigationManagerCallbackExecutor;
        if (callback == null || executor == null) {
            Log.w(TAG_NAVIGATION_MANAGER,
                    "NavigationManagerCallback not set, skipping onAutoDriveEnabled");
            return;
        }

        executor.execute(callback::onAutoDriveEnabled);
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    @SuppressWarnings({"methodref.receiver.bound.invalid"})
    protected NavigationManager(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        mCarContext = requireNonNull(carContext);
        mHostDispatcher = requireNonNull(hostDispatcher);
        mNavigationManager =
                new INavigationManager.Stub() {
                    @Override
                    public void onStopNavigation(IOnDoneCallback callback) {
                        RemoteUtils.dispatchCallFromHost(
                                lifecycle, callback,
                                "onStopNavigation",
                                () -> {
                                    NavigationManager.this.onStopNavigation();
                                    return null;
                                });
                    }
                };
        LifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                NavigationManager.this.onStopNavigation();
                lifecycle.removeObserver(this);
            }
        };
        lifecycle.addObserver(observer);
    }
}
