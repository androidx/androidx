/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.navigation.common.car;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.sample.navigation.common.R;
import androidx.car.app.sample.navigation.common.model.Instruction;
import androidx.car.app.sample.navigation.common.nav.NavigationService;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;

/** Session class for the Navigation sample app. */
class NavigationSession extends Session implements NavigationScreen.Listener {
    static final String TAG = NavigationSession.class.getSimpleName();

    static final String URI_SCHEME = "samples";
    static final String URI_HOST = "navigation";

    @Nullable
    NavigationScreen mNavigationScreen;

    @Nullable
    SurfaceRenderer mNavigationCarSurface;

    // A reference to the navigation service used to get location updates and routing.
    @Nullable
    NavigationService mService;

    @NonNull
    Action mSettingsAction;

    final NavigationService.Listener mServiceListener =
            new NavigationService.Listener() {
                @Override
                public void navigationStateChanged(
                        boolean isNavigating,
                        boolean isRerouting,
                        boolean hasArrived,
                        @Nullable List<Destination> destinations,
                        @Nullable List<Step> steps,
                        @Nullable TravelEstimate nextDestinationTravelEstimate,
                        @Nullable Distance nextStepRemainingDistance,
                        boolean shouldShowNextStep,
                        boolean shouldShowLanes,
                        @Nullable CarIcon junctionImage) {
                    mNavigationScreen.updateTrip(
                            isNavigating,
                            isRerouting,
                            hasArrived,
                            destinations,
                            steps,
                            nextDestinationTravelEstimate,
                            nextStepRemainingDistance,
                            shouldShowNextStep,
                            shouldShowLanes,
                            junctionImage);
                }
            };

    // A listener to periodically update the surface with the location coordinates
    LocationListener mLocationListener =
            new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mNavigationCarSurface.updateLocationString(getLocationString(location));
                }

                /** @deprecated This callback will never be invoked on Android Q and above. */
                @Override
                @Deprecated
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };

    // Monitors the state of the connection to the Navigation service.
    final ServiceConnection mServiceConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.i(TAG, "In onServiceConnected() component:" + name);
                    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
                    mService = binder.getService();
                    mService.setCarContext(getCarContext(), mServiceListener);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.i(TAG, "In onServiceDisconnected() component:" + name);
                    // Unhook map models here
                    mService.clearCarContext();
                    mService = null;
                }
            };

    private final LifecycleObserver mLifeCycleObserver =
            new DefaultLifecycleObserver() {

                @Override
                public void onCreate(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onCreate()");
                }

                @Override
                public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onStart()");
                    getCarContext()
                            .bindService(
                                    new Intent(getCarContext(), NavigationService.class),
                                    mServiceConnection,
                                    Context.BIND_AUTO_CREATE);
                }

                @Override
                public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onResume()");
                }

                @Override
                public void onPause(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onPause()");
                }

                @Override
                public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onStop()");
                    getCarContext().unbindService(mServiceConnection);
                    mService = null;
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                    Log.i(TAG, "In onDestroy()");

                    LocationManager locationManager =
                            (LocationManager)
                                    getCarContext().getSystemService(Context.LOCATION_SERVICE);
                    locationManager.removeUpdates(mLocationListener);
                }
            };

    NavigationSession() {
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(mLifeCycleObserver);
    }

    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        Log.i(TAG, "In onCreateScreen()");

        mSettingsAction =
                new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(), R.drawable.ic_settings))
                                        .build())
                        .setOnClickListener(
                                () -> {
                                    getCarContext()
                                            .getCarService(ScreenManager.class)
                                            .push(new SettingsScreen(getCarContext()));
                                })
                        .build();

        mNavigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
        mNavigationScreen =
                new NavigationScreen(getCarContext(), mSettingsAction, this, mNavigationCarSurface);

        String action = intent.getAction();
        if (action != null && CarContext.ACTION_NAVIGATE.equals(action)) {
            CarToast.makeText(
                    getCarContext(),
                    "Navigation intent: " + intent.getDataString(),
                    CarToast.LENGTH_LONG)
                    .show();
        }

        if (getCarContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            // If we do not have the location permission, preseed the navigation screen first, and
            // push
            // the request permission screen. When the user grants the location permission, the
            // request
            // permission screen will be popped and the navigation screen will be displayed.
            getCarContext().getCarService(ScreenManager.class).push(mNavigationScreen);
            return new RequestPermissionScreen(getCarContext(), this::requestLocationUpdates);
        }

        return mNavigationScreen;
    }

    @Override
    public void onNewIntent(@NonNull Intent intent) {
        Log.i(TAG, "In onNewIntent() " + intent);
        ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);
        if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
            Uri uri = Uri.parse("http://" + intent.getDataString());
            screenManager.popToRoot();
            screenManager.pushForResult(
                    new SearchResultsScreen(
                            getCarContext(),
                            mSettingsAction,
                            mNavigationCarSurface,
                            uri.getQueryParameter("q")),
                    (obj) -> {
                        if (obj != null) {
                            // Need to copy over each element to satisfy Java type safety.
                            List<?> results = (List<?>) obj;
                            List<Instruction> instructions = new ArrayList<Instruction>();
                            for (Object result : results) {
                                instructions.add((Instruction) result);
                            }
                            executeScript(instructions);
                        }
                    });

            return;
        }

        // Process the intent from DeepLinkNotificationReceiver. Bring the routing screen back to
        // the
        // top if any other screens were pushed onto it.
        Uri uri = intent.getData();
        if (uri != null
                && URI_SCHEME.equals(uri.getScheme())
                && URI_HOST.equals(uri.getSchemeSpecificPart())) {

            Screen top = screenManager.getTop();
            switch (uri.getFragment()) {
                case NavigationService.DEEP_LINK_ACTION:
                    if (!(top instanceof NavigationScreen)) {
                        screenManager.popToRoot();
                    }
                    break;
                default:
                    // No-op
            }
        }
    }

    @Override
    public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
        mNavigationCarSurface.onCarConfigurationChanged();
    }

    @Override
    public void executeScript(@NonNull List<Instruction> instructions) {
        if (mService != null) {
            mService.executeInstructions(instructions);
        }
    }

    @Override
    public void stopNavigation() {
        if (mService != null) {
            mService.stopNavigation();
        }
    }

    /**
     * Requests location updates for the navigation surface.
     *
     * @throws java.lang.SecurityException if the app does not have the location permission.
     */
    @SuppressLint("MissingPermission")
    void requestLocationUpdates() {
        LocationManager locationManager =
                (LocationManager) getCarContext().getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mNavigationCarSurface.updateLocationString(getLocationString(location));
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                /* minTimeMs= */ 1000,
                /* minDistanceM= */ 1,
                mLocationListener);
    }

    static String getLocationString(@Nullable Location location) {
        if (location == null) {
            return "unknown";
        }
        return "time: "
                + location.getTime()
                + " lat: "
                + location.getLatitude()
                + " lng: "
                + location.getLongitude();
    }
}
