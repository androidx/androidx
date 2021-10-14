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

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.managers.Manager;
import androidx.car.app.utils.RemoteUtils;
import androidx.lifecycle.Lifecycle;

/** Manages the communication between the app and the host. */
public class AppManager implements Manager {
    private static final int LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1000;
    private static final int LOCATION_UPDATE_MIN_DISTANCE_METER = 1;

    @NonNull
    private final CarContext mCarContext;
    @NonNull
    private final IAppManager.Stub mAppManager;
    @NonNull
    private final HostDispatcher mHostDispatcher;
    @NonNull
    private final Lifecycle mLifecycle;

    /**
     * {@link LocationListener} for getting location updates within the app and sends them over to
     * the car host.
     *
     * <p>This should only be enabled when the car host explicitly calls {@code IAppManager
     * .startLocationUpdates}.
     */
    private final LocationListener mLocationListener;
    @VisibleForTesting
    final HandlerThread mLocationUpdateHandlerThread;

    /**
     * Sets the {@link SurfaceCallback} to get changes and updates to the surface on which the
     * app can draw custom content, or {@code null} to reset the listener.
     *
     * <p>This call requires the {@code androidx.car.app.ACCESS_SURFACE}
     * permission to be declared.
     *
     * <p>The {@link Surface} can be used to draw custom content such as a navigation app's map.
     *
     * <p>Note that the listener relates to UI events and will be executed on the main thread
     * using {@link Looper#getMainLooper()}.
     *
     * @throws SecurityException if the app does not have the required permissions to access the
     *                           surface
     * @throws HostException     if the remote call fails
     */
    // TODO(b/178748627): the nullable annotation from the AIDL file is not being considered.
    @SuppressWarnings("NullAway")
    @SuppressLint("ExecutorRegistration")
    public void setSurfaceCallback(@Nullable SurfaceCallback surfaceCallback) {
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                "setSurfaceListener", (IAppHost host) -> {
                    host.setSurfaceCallback(
                            RemoteUtils.stubSurfaceCallback(mLifecycle, surfaceCallback));
                    return null;
                }
        );
    }

    /**
     * Requests the current template to be invalidated, which eventually triggers a call to {@link
     * Screen#onGetTemplate} to get the new template to display.
     *
     * @throws HostException if the remote call fails
     */
    public void invalidate() {
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                "invalidate", (IAppHost host) -> {
                    host.invalidate();
                    return null;
                }
        );
    }

    /**
     * Shows a toast on the car screen.
     *
     * @param text     the text to show
     * @param duration how long to display the message
     * @throws HostException        if the remote call fails
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void showToast(@NonNull CharSequence text, @CarToast.Duration int duration) {
        requireNonNull(text);
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                "showToast", (IAppHost host) -> {
                    host.showToast(text, duration);
                    return null;
                }
        );
    }

    /** Returns the {@code IAppManager.Stub} binder. */
    IAppManager.Stub getIInterface() {
        return mAppManager;
    }

    @NonNull
    Lifecycle getLifecycle() {
        return mLifecycle;
    }

    /**
     * Start requesting location updates from the app.
     *
     * <p>This is only called from the host. If location permission(s) have not been granted, we
     * return a {@link FailureResponse} back to the host and would not call this method.
     */
    // Location permissions should be granted by the app if they need this API.
    @SuppressLint("MissingPermission")
    void startLocationUpdates() {
        stopLocationUpdates();
        LocationManager locationManager = mCarContext.getSystemService(LocationManager.class);
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER,
                LOCATION_UPDATE_MIN_INTERVAL_MILLIS,
                LOCATION_UPDATE_MIN_DISTANCE_METER,
                mLocationListener,
                mLocationUpdateHandlerThread.getLooper());
    }

    /**
     * Stops requesting location updates from the app.
     */
    void stopLocationUpdates() {
        LocationManager locationManager = mCarContext.getSystemService(LocationManager.class);
        locationManager.removeUpdates(mLocationListener);
    }

    /** Creates an instance of {@link AppManager}. */
    static AppManager create(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        requireNonNull(carContext);
        requireNonNull(hostDispatcher);
        requireNonNull(lifecycle);

        return new AppManager(carContext, hostDispatcher, lifecycle);
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    protected AppManager(@NonNull CarContext carContext, @NonNull HostDispatcher hostDispatcher,
            @NonNull Lifecycle lifecycle) {
        mCarContext = carContext;
        mHostDispatcher = hostDispatcher;
        mLifecycle = lifecycle;
        mAppManager = new IAppManager.Stub() {
            @Override
            public void getTemplate(IOnDoneCallback callback) {
                RemoteUtils.dispatchCallFromHost(getLifecycle(), callback, "getTemplate",
                        carContext.getCarService(ScreenManager.class)::getTopTemplate);
            }

            @Override
            public void onBackPressed(IOnDoneCallback callback) {
                RemoteUtils.dispatchCallFromHost(getLifecycle(), callback,
                        "onBackPressed",
                        () -> {
                            carContext.getOnBackPressedDispatcher().onBackPressed();
                            return null;
                        });
            }

            @Override
            public void startLocationUpdates(IOnDoneCallback callback) {
                if (carContext.checkSelfPermission(ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_DENIED && carContext.checkSelfPermission(
                        ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_DENIED) {
                    RemoteUtils.sendFailureResponseToHost(callback, "startLocationUpdates",
                            new SecurityException("Location permission(s) not granted."));
                }


                RemoteUtils.dispatchCallFromHost(getLifecycle(), callback,
                        "startLocationUpdates",
                        () -> {
                            carContext.getCarService(AppManager.class).startLocationUpdates();
                            return null;
                        });
            }

            @Override
            public void stopLocationUpdates(IOnDoneCallback callback) {
                RemoteUtils.dispatchCallFromHost(getLifecycle(), callback,
                        "stopLocationUpdates",
                        () -> {
                            carContext.getCarService(AppManager.class).stopLocationUpdates();
                            return null;
                        });
            }
        };

        mLocationUpdateHandlerThread = new HandlerThread("LocationUpdateThread");
        mLocationListener = location -> {
            mHostDispatcher.dispatch(
                    CarContext.APP_SERVICE,
                    "sendLocation", (IAppHost host) -> {
                        host.sendLocation(location);
                        return null;
                    }
            );
        };
    }
}
