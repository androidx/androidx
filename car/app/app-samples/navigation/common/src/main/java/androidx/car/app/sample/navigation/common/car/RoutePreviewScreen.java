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

import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import androidx.car.app.sample.navigation.common.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The route preview screen for the app. */
public final class RoutePreviewScreen extends Screen {
    private static final String TAG = "NavigationDemo";

    @NonNull
    private final Action mSettingsAction;
    @NonNull
    private final SurfaceRenderer mSurfaceRenderer;
    @NonNull
    private final List<Row> mRouteRows;

    int mLastSelectedIndex = -1;

    public RoutePreviewScreen(
            @NonNull CarContext carContext,
            @NonNull Action settingsAction,
            @NonNull SurfaceRenderer surfaceRenderer) {
        super(carContext);
        mSettingsAction = settingsAction;
        mSurfaceRenderer = surfaceRenderer;

        mRouteRows = new ArrayList<>();
        SpannableString firstRoute = new SpannableString("   \u00b7 Shortest route");
        firstRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1, 0);
        SpannableString secondRoute = new SpannableString("   \u00b7 Less busy");
        secondRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(24)), 0, 1, 0);
        SpannableString thirdRoute = new SpannableString("   \u00b7 HOV friendly");
        thirdRoute.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867)), 0, 1, 0);

        mRouteRows.add(new Row.Builder().setTitle(firstRoute).addText("Via NE 8th Street").build());
        mRouteRows.add(new Row.Builder().setTitle(secondRoute).addText("Via NE 1st Ave").build());
        mRouteRows.add(new Row.Builder().setTitle(thirdRoute).addText("Via NE 4th Street").build());
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Log.i(TAG, "In RoutePreviewScreen.onGetTemplate()");
        onRouteSelected(0);

        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder
                .setOnSelectedListener(this::onRouteSelected)
                .setOnItemsVisibilityChangedListener(this::onRoutesVisible);
        for (Row row : mRouteRows) {
            listBuilder.addItem(row);
        }
        return new RoutePreviewNavigationTemplate.Builder()
                .setItemList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.route_preview))
                .setActionStrip(new ActionStrip.Builder().addAction(mSettingsAction).build())
                .setHeaderAction(Action.BACK)
                .setNavigateAction(
                        new Action.Builder()
                                .setTitle("Continue to route")
                                .setOnClickListener(this::onNavigate)
                                .build())
                .build();
    }

    private void onRouteSelected(int index) {
        mLastSelectedIndex = index;
        mSurfaceRenderer.updateMarkerVisibility(
                /* showMarkers=*/ true,
                /* numMarkers=*/ mRouteRows.size(),
                /* activeMarker=*/ mLastSelectedIndex);
    }

    private void onRoutesVisible(int startIndex, int endIndex) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(
                    TAG,
                    String.format(
                            "In RoutePreviewScreen.onRoutesVisible start:%d end:%d",
                            startIndex, endIndex));
        }
    }

    private void onNavigate() {
        setResult(mLastSelectedIndex);
        finish();
    }
}
