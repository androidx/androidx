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

package androidx.car.app.sample.places.common;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import static androidx.car.app.sample.places.common.Executors.BACKGROUND_EXECUTOR;
import static androidx.car.app.sample.places.common.Executors.UI_EXECUTOR;

import android.location.Geocoder;
import android.location.Location;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.places.common.places.PlaceCategory;
import androidx.car.app.sample.places.common.places.PlaceFinder;
import androidx.car.app.sample.places.common.places.PlaceInfo;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/** A screen that displays a list of places for a given category, around a given location. */
public class PlaceListScreen extends Screen implements DefaultLifecycleObserver {
    private static final int METERS_TO_KMS = 1000;

    private final Location mSearchCenter;

    @Nullable
    private final Location mAnchor;
    private final int mRadiusMeters;
    private final PlaceCategory mCategory;
    private final int mMaxSearchResults;

    private Geocoder mGeocoder;

    @Nullable
    private List<PlaceInfo> mPlaces;

    @NonNull
    private PlaceFinder mPlaceFinder;

    /**
     * Returns a screen showing the places that result by querying around the given location and
     * radius (in meters), for the given category.
     */
    static PlaceListScreen create(
            @NonNull CarContext carContext,
            @NonNull Location searchCenter,
            int radiusMeters,
            int maxSearchResults,
            PlaceCategory category,
            @Nullable Location anchor) {
        return new PlaceListScreen(
                carContext, searchCenter, radiusMeters, maxSearchResults, category, anchor);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        mGeocoder = new Geocoder(getCarContext());
        mPlaceFinder =
                new PlaceFinder(getCarContext().getResources().getString(R.string.PLACES_API_KEY));
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        update();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        boolean hasPlaces = false;

        // If we don't have any places yet, show a loading progress indicator.
        if (mPlaces != null) {
            // Add one row per place in the results.
            for (int i = 0; i < mPlaces.size(); i++) {
                if (i >= 6) {
                    // only 6 rows allowed.
                    break;
                }

                PlaceInfo place = mPlaces.get(i);
                Location location = place.getLocation();
                int distanceMeters = getDistanceFromSearchCenter(location);
                int distanceKm = distanceMeters / METERS_TO_KMS;

                SpannableString address =
                        new SpannableString(
                                "   \u00b7 " + place.getAddress(mGeocoder).getAddressLine(0));
                DistanceSpan distanceSpan =
                        DistanceSpan.create(Distance.create(distanceKm, Distance.UNIT_KILOMETERS));
                address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
                listBuilder.addItem(
                        new Row.Builder()
                                // Clicking on the place shows a toast with the full place address.
                                .setOnClickListener(() -> onClickPlace(place))
                                .setTitle(place.getName())
                                .addText(address)
                                .setMetadata(
                                        new Metadata.Builder()
                                                .setPlace(
                                                        new Place.Builder(
                                                                CarLocation.create(
                                                                        location))
                                                                .setMarker(
                                                                        new PlaceMarker.Builder()
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());

                hasPlaces = true;
            }
        }

        // Anchor the map around the search center if there is no place results, or if the anchor
        // location has been explicitly set.
        Place anchor = null;
        if (mAnchor != null) {
            anchor =
                    new Place.Builder(
                            CarLocation.create(
                                    mAnchor.getLatitude(), mAnchor.getLongitude()))
                            .setMarker(new PlaceMarker.Builder().setColor(CarColor.BLUE).build())
                            .build();
        } else if (!hasPlaces) {
            anchor =
                    new Place.Builder(
                            CarLocation.create(
                                    mSearchCenter.getLatitude(),
                                    mSearchCenter.getLongitude()))
                            .build();
        }

        PlaceListMapTemplate.Builder builder =
                new PlaceListMapTemplate.Builder()
                        .setTitle(mCategory.getDisplayName())
                        .setHeaderAction(Action.BACK)
                        .setAnchor(anchor)
                        .setCurrentLocationEnabled(true);
        if (mPlaces == null) {
            return builder.setLoading(true).build();
        } else {
            return builder.setItemList(listBuilder.build()).build();
        }
    }

    private void onClickPlace(PlaceInfo place) {
        getScreenManager().push(PlaceDetailsScreen.create(getCarContext(), place));
    }

    @SuppressWarnings({"FutureReturnValueIgnored"})
    private ListenableFuture<Void> update() {
        return Futures.transformAsync(
                // Run the query in the background thread, and update with the results in the UI
                // thread.
                Futures.submitAsync(
                        () ->
                                Futures.immediateFuture(
                                        mPlaceFinder.getPlacesByCategory(
                                                mSearchCenter,
                                                mRadiusMeters,
                                                mMaxSearchResults,
                                                mCategory.getCategory())),
                        BACKGROUND_EXECUTOR),
                places -> {
                    mPlaces = places;
                    invalidate();
                    return null;
                },
                UI_EXECUTOR);
    }

    /** Returns the disntance in meters of the {@code location} from the {@link #mSearchCenter}. */
    private int getDistanceFromSearchCenter(Location location) {
        return (int) mSearchCenter.distanceTo(location);
    }

    private PlaceListScreen(
            @NonNull CarContext carContext,
            @NonNull Location searchCenter,
            int radiusMeters,
            int maxSearchResults,
            PlaceCategory category,
            @Nullable Location anchor) {
        super(carContext);

        mSearchCenter = searchCenter;
        mRadiusMeters = radiusMeters;
        mMaxSearchResults = maxSearchResults;
        mCategory = category;
        mAnchor = anchor;

        getLifecycle().addObserver(this);
    }
}
