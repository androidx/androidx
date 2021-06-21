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

import static androidx.car.app.sample.places.common.Executors.BACKGROUND_EXECUTOR;
import static androidx.car.app.sample.places.common.Executors.UI_EXECUTOR;

import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.SearchTemplate.SearchCallback;
import androidx.car.app.model.Template;
import androidx.car.app.sample.places.common.places.PlaceFinder;
import androidx.car.app.sample.places.common.places.PlaceInfo;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.Futures;

/** A screen that displays a search edit text and search results. */
public class SearchScreen extends Screen implements DefaultLifecycleObserver {
    private static final String TAG = "PlacesDemo";

    boolean mIsSearchComplete;

    private Geocoder mGeocoder;

    @NonNull
    private PlaceFinder mPlaceFinder;

    @NonNull
    private Location mSearchLocation;

    private ItemList mItemList = withNoResults(new ItemList.Builder()).build();

    SearchScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        mPlaceFinder =
                new PlaceFinder(getCarContext().getResources().getString(R.string.PLACES_API_KEY));
        mGeocoder = new Geocoder(getCarContext());
        mSearchLocation = Constants.INITIAL_SEARCH_LOCATION;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new SearchTemplate.Builder(
                new SearchCallback() {
                    @Override
                    public void onSearchTextChanged(@NonNull String searchText) {
                        doSearch(searchText);
                    }

                    @Override
                    public void onSearchSubmitted(@NonNull String searchTerm) {
                        mIsSearchComplete = true;
                        doSearch(searchTerm);
                    }
                })
                .setHeaderAction(Action.BACK)
                .setShowKeyboardByDefault(false)
                .setItemList(mItemList)
                .build();
    }

    void doSearch(String searchText) {
        ItemList.Builder builder = new ItemList.Builder();
        if (searchText.isEmpty()) {
            mItemList = withNoResults(builder).build();
        }

        Futures.transformAsync(
                // Run the query in the background thread, and update with the results in
                // the UI thread.
                Futures.submitAsync(
                        () ->
                                Futures.immediateFuture(
                                        mPlaceFinder.getPlacesByName(
                                                mSearchLocation,
                                                Constants.LOCATION_SEARCH_RADIUS_METERS,
                                                Constants.LOCATION_SEARCH_MAX_RESULTS,
                                                searchText)),
                        BACKGROUND_EXECUTOR),
                places -> {
                    if (mIsSearchComplete) {
                        if (!places.isEmpty()) {
                            setResult(places.get(0).getLocation());
                        }
                    } else {
                        if (places.isEmpty()) {
                            mItemList = withNoResults(builder).build();
                        } else {
                            for (PlaceInfo place : places) {
                                Row.Builder rowBuilder = new Row.Builder();
                                String name = place.getName();
                                if (name != null) {
                                    rowBuilder.setTitle(name);
                                }
                                String addressLine =
                                        place.getAddress(mGeocoder).getAddressLine(0);
                                if (addressLine != null) {
                                    rowBuilder.addText(addressLine);
                                }
                                rowBuilder.setOnClickListener(
                                        () -> {
                                            setResult(place.getLocation());
                                            finish();
                                        });
                                builder.addItem(rowBuilder.build());
                            }
                        }
                        mItemList = builder.build();
                    }
                    return null;
                },
                UI_EXECUTOR)
                .addListener(this::searchCompleted, UI_EXECUTOR);
    }

    private void searchCompleted() {
        if (mIsSearchComplete) {
            finish();
        } else {
            invalidate();
        }
    }

    private static ItemList.Builder withNoResults(ItemList.Builder builder) {
        return builder.setNoItemsMessage("No Results");
    }
}
