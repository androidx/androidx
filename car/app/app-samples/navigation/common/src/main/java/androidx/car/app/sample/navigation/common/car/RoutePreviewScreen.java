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
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.navigation.common.R;
import androidx.core.graphics.drawable.IconCompat;

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

        CarIcon actionIcon = new CarIcon.Builder(IconCompat.createWithResource(
                getCarContext(), R.drawable.baseline_assistant_navigation_24)).build();
        Action navigateAction = new Action.Builder()
                .setIcon(actionIcon)
                .setOnClickListener(this::onNavigate)
                .build();

        mRouteRows = new ArrayList<>();
        SpannableString firstRoute = new SpannableString("   \u00b7 Shortest route");
        firstRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1, 0);
        SpannableString secondRoute = new SpannableString("   \u00b7 Less busy");
        secondRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(24)), 0, 1, 0);
        SpannableString thirdRoute = new SpannableString("   \u00b7 HOV friendly");
        thirdRoute.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867)), 0, 1, 0);

        mRouteRows.add(new Row.Builder().setTitle(firstRoute)
                .setOnClickListener(() -> onRouteSelected(0))
                .addText("Via NE 8th Street")
                .addAction(navigateAction)
                .build());
        mRouteRows.add(new Row.Builder().setTitle(secondRoute)
                .setOnClickListener(() -> onRouteSelected(1))
                .addText("Via NE 1st Ave")
                .addAction(navigateAction).build());
        mRouteRows.add(new Row.Builder().setTitle(thirdRoute)
                .setOnClickListener(() -> onRouteSelected(2))
                .addText("Via NE 4th Street")
                .addAction(navigateAction).build());
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Log.i(TAG, "In RoutePreviewScreen.onGetTemplate()");
        onRouteSelected(0);

        ItemList.Builder listBuilder = new ItemList.Builder();

        for (Row row : mRouteRows) {
            listBuilder.addItem(row);
        }

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .setTitle(getCarContext().getString(R.string.route_preview))
                .build();

        return new MapWithContentTemplate.Builder()
                .setContentTemplate(new ListTemplate.Builder()
                        .setHeader(header)
                        .setSingleList(listBuilder.build())
                        .build())
                .setActionStrip(new ActionStrip.Builder().addAction(mSettingsAction).build())
                .build();
    }

    private void onRouteSelected(int index) {
        mLastSelectedIndex = index;
        mSurfaceRenderer.updateMarkerVisibility(
                /* showMarkers=*/ true,
                /* numMarkers=*/ mRouteRows.size(),
                /* activeMarker=*/ mLastSelectedIndex);
    }

    private void onNavigate() {
        setResult(mLastSelectedIndex);
        finish();
    }
}
