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

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.car.app.sample.navigation.common.R;
import androidx.car.app.sample.navigation.common.model.DemoScripts;
import androidx.car.app.sample.navigation.common.model.PlaceInfo;

import java.util.ArrayList;
import java.util.List;

/** Screen for showing a list of favorite places. */
public final class FavoritesScreen extends Screen {
    private static final String TAG = "NavigationDemo";

    @Nullable
    private List<PlaceInfo> mFavorites;
    @NonNull
    private final Action mSettingsAction;
    @NonNull
    private final SurfaceRenderer mSurfaceRenderer;

    public FavoritesScreen(
            @NonNull CarContext carContext,
            @NonNull Action settingsAction,
            @NonNull SurfaceRenderer surfaceRenderer) {
        super(carContext);
        mSettingsAction = settingsAction;
        mSurfaceRenderer = surfaceRenderer;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Log.i(TAG, "In FavoritesScreen.onGetTemplate()");
        mSurfaceRenderer.updateMarkerVisibility(
                /* showMarkers=*/ false, /* numMarkers=*/ 0, /* activeMarker=*/ -1);
        ItemList.Builder listBuilder = new ItemList.Builder();

        for (PlaceInfo place : getFavorites()) {
            SpannableString address = new SpannableString("   \u00b7 " + place.getDisplayAddress());
            DistanceSpan distanceSpan =
                    DistanceSpan.create(
                            Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
            address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle(place.getName())
                            .addText(address)
                            .setOnClickListener(() -> onClickFavorite())
                            .setMetadata(
                                    new Metadata.Builder()
                                            .setPlace(
                                                    new Place.Builder(CarLocation.create(1, 1))
                                                            .build())
                                            .build())
                            .build());
        }

        return new PlaceListNavigationTemplate.Builder()
                .setItemList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.app_name))
                .setActionStrip(new ActionStrip.Builder().addAction(mSettingsAction).build())
                .setHeaderAction(Action.BACK)
                .build();
    }

    private void onClickFavorite() {
        getScreenManager()
                .pushForResult(
                        new RoutePreviewScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
                        this::onRoutePreviewResult);
    }

    private void onRoutePreviewResult(@Nullable Object previewResult) {
        int previewIndex = previewResult == null ? -1 : (int) previewResult;
        if (previewIndex < 0) {
            return;
        }
        // Start the same demo instructions. More will be added in the future.
        setResult(DemoScripts.getNavigateHome(getCarContext()));
        finish();
    }

    @NonNull
    private List<PlaceInfo> getFavorites() {
        // Lazy initialize mFavorites.
        if (mFavorites != null) {
            return mFavorites;
        }
        ArrayList<PlaceInfo> favorites = new ArrayList<>();
        PlaceInfo home =
                new PlaceInfo(
                        getCarContext().getString(R.string.home_destination_label),
                        "9 10th Street.");
        favorites.add(home);
        PlaceInfo work =
                new PlaceInfo(
                        getCarContext().getString(R.string.work_destination_label),
                        "2 3rd Street.");
        favorites.add(work);
        mFavorites = favorites;
        return mFavorites;
    }
}
