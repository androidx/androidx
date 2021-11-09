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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.places.common.places.PlaceCategory;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** A screen that displays a list of place categories. */
public class PlaceCategoryListScreen extends Screen implements DefaultLifecycleObserver {

    static PlaceCategoryListScreen create(@NonNull CarContext carContext) {
        return new PlaceCategoryListScreen(carContext);
    }

    private Location mAnchorLocation;

    @NonNull
    private Location mSearchLocation = Constants.INITIAL_SEARCH_LOCATION;

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        setSearchLocation(Constants.INITIAL_SEARCH_LOCATION);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        // Build a list of rows for each category.
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (PlaceCategory category : Constants.CATEGORIES) {
            PlaceListScreen screen = PlaceListScreen.create(
                    getCarContext(),
                    mSearchLocation,
                    Constants.POI_SEARCH_RADIUS_METERS,
                    Constants.POI_SEARCH_MAX_RESULTS,
                    category,
                    mAnchorLocation);

            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle(category.getDisplayName())
                            // Clicking on the row pushes a screen that shows the list of places of
                            // that
                            // category around the center location.
                            .setOnClickListener(
                                    () ->
                                            getScreenManager()
                                                    .push(screen))
                            .setBrowsable(true)
                            .build());
        }

        Place.Builder anchorBuilder;

        // If we have an anchor explicitly set, display it in the map. Otherwise, use the current
        // search location.
        if (mAnchorLocation != null) {
            anchorBuilder =
                    new Place.Builder(CarLocation.create(mAnchorLocation))
                            .setMarker(new PlaceMarker.Builder().setColor(CarColor.BLUE).build());
        } else {
            anchorBuilder = new Place.Builder(CarLocation.create(mSearchLocation));
        }

        ActionStrip actionStrip =
                new ActionStrip.Builder()
                        .addAction(
                                new Action.Builder()
                                        .setTitle("Search")
                                        .setOnClickListener(
                                                () ->
                                                        getScreenManager()
                                                                .pushForResult(
                                                                        new SearchScreen(
                                                                                getCarContext()),
                                                                        this::setSearchLocation))
                                        .build())
                        .build();

        return new PlaceListMapTemplate.Builder()
                .setItemList(listBuilder.build())
                .setHeaderAction(Action.APP_ICON)
                .setActionStrip(actionStrip)
                .setTitle("AndroidX Categories")
                .setCurrentLocationEnabled(true)
                .setAnchor(anchorBuilder.build())
                .build();
    }

    private void setSearchLocation(@Nullable Object location) {
        if (location != null) {
            mAnchorLocation = (Location) location;
            mSearchLocation = mAnchorLocation;
        }
    }

    private PlaceCategoryListScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }
}
