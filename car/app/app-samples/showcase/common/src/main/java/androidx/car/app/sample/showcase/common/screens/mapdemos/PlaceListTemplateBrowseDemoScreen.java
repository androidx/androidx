/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.mapdemos;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.HandlerThread;

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
import androidx.car.app.sample.showcase.common.R;
import androidx.core.location.LocationListenerCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Creates a screen using the {@link PlaceListMapTemplate}.
 *
 * <p>This screen shows the ability to anchor the map around the current location when there are
 * no other POI markers present.
 */
public final class PlaceListTemplateBrowseDemoScreen extends Screen {
    private static final int LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1000;
    private static final int LOCATION_UPDATE_MIN_DISTANCE_METER = 1;

    final LocationListenerCompat mLocationListener;
    final HandlerThread mLocationUpdateHandlerThread = new HandlerThread("LocationThread");
    boolean mHasPermissionLocation;

    private @Nullable Location mCurrentLocation;

    public PlaceListTemplateBrowseDemoScreen(@NonNull CarContext carContext) {
        super(carContext);

        mHasPermissionLocation = carContext.checkSelfPermission(ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || carContext.checkSelfPermission(ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        mLocationListener = location -> {
            mCurrentLocation = location;
            invalidate();
        };

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                mHasPermissionLocation = carContext.checkSelfPermission(ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        || carContext.checkSelfPermission(ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                if (mHasPermissionLocation) {
                    LocationManager locationManager =
                            carContext.getSystemService(LocationManager.class);
                    locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER,
                            LOCATION_UPDATE_MIN_INTERVAL_MILLIS,
                            LOCATION_UPDATE_MIN_DISTANCE_METER,
                            mLocationListener,
                            mLocationUpdateHandlerThread.getLooper());
                } else {
                    CarToast.makeText(carContext,
                            getCarContext().getString(R.string.grant_location_permission_toast_msg),
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

    @Override
    public @NonNull Template onGetTemplate() {
        PlaceListMapTemplate.Builder builder = new PlaceListMapTemplate.Builder()
                .setItemList(new ItemList.Builder()
                        .addItem(new Row.Builder()
                                .setTitle(getCarContext().getString(R.string.browse_places_title))
                                .setBrowsable(true)
                                .setOnClickListener(
                                        () -> getScreenManager().push(
                                                new PlaceListTemplateDemoScreen(
                                                        getCarContext()))).build())
                        .build())
                .setTitle(getCarContext().getString(R.string.place_list_template_demo_title))
                .setHeaderAction(Action.BACK)
                .setCurrentLocationEnabled(mHasPermissionLocation);

        if (mCurrentLocation != null) {
            builder.setAnchor(new Place.Builder(CarLocation.create(mCurrentLocation)).build());
        }

        return builder.build();
    }
}
