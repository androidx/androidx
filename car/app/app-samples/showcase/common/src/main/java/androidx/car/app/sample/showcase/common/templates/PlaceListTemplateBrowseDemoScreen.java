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

package androidx.car.app.sample.showcase.common.templates;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Creates a screen using the {@link PlaceListMapTemplate}.
 *
 * <p>This screen shows the ability to anchor the map around the current location when there are
 * no other POI markers present.
 */
public final class PlaceListTemplateBrowseDemoScreen extends Screen {
    private static final int LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1000;
    private static final int LOCATION_UPDATE_MIN_DISTANCE_METER = 1;

    final LocationListener mLocationListener;
    final HandlerThread mLocationUpdateHandlerThread;

    @Nullable
    private Location mCurrentLocation;

    public PlaceListTemplateBrowseDemoScreen(@NonNull CarContext carContext) {
        super(carContext);

        mLocationUpdateHandlerThread = new HandlerThread("LocationThread");
        mLocationListener = location -> {
            mCurrentLocation = location;
            invalidate();
        };

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                if (carContext.checkSelfPermission(ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        || carContext.checkSelfPermission(ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    LocationManager locationManager =
                            carContext.getSystemService(LocationManager.class);
                    locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER,
                            LOCATION_UPDATE_MIN_INTERVAL_MILLIS,
                            LOCATION_UPDATE_MIN_DISTANCE_METER,
                            mLocationListener,
                            mLocationUpdateHandlerThread.getLooper());
                } else {
                    CarToast.makeText(carContext,
                            "Grant location Permission to see current location",
                            CarToast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                LocationManager locationManager =
                        getCarContext().getSystemService(LocationManager.class);
                locationManager.removeUpdates(mLocationListener);
            }
        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        PlaceListMapTemplate.Builder builder = new PlaceListMapTemplate.Builder()
                .setItemList(new ItemList.Builder()
                        .addItem(new Row.Builder()
                                .setTitle("Browse Places")
                                .setBrowsable(true)
                                .setOnClickListener(
                                        () -> getScreenManager().push(
                                                new PlaceListTemplateDemoScreen(
                                                        getCarContext()))).build())
                        .build())
                .setTitle("Place List Template Demo")
                .setHeaderAction(Action.BACK)
                .setCurrentLocationEnabled(true);

        if (mCurrentLocation != null) {
            builder.setAnchor(new Place.Builder(CarLocation.create(mCurrentLocation)).build());
        }

        return builder.build();
    }
}
