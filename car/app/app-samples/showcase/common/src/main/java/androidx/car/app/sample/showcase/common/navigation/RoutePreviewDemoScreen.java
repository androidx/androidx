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

package androidx.car.app.sample.showcase.common.navigation;

import static androidx.car.app.CarToast.LENGTH_LONG;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;

import java.util.concurrent.TimeUnit;

/** Creates a screen using the {@link RoutePreviewNavigationTemplate} */
public final class RoutePreviewDemoScreen extends Screen {
    public RoutePreviewDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        // Set text variants for the first route.
        SpannableString firstRouteLongText = new SpannableString(
                "   \u00b7 ---------------- Short" + "  " + "route " + "-------------------");
        firstRouteLongText.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1, 0);
        SpannableString firstRouteShortText = new SpannableString("   \u00b7 Short Route");
        firstRouteShortText.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1, 0);
        CarText firstRoute = new CarText.Builder(firstRouteLongText)
                .addVariant(firstRouteShortText)
                .build();

        SpannableString secondRoute = new SpannableString("   \u00b7 Less busy");
        secondRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(24)), 0, 1, 0);
        SpannableString thirdRoute = new SpannableString("   \u00b7 HOV friendly");
        thirdRoute.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867)), 0, 1, 0);

        // Set text variants for the navigate action text.
        CarText navigateActionText =
                new CarText.Builder("Continue to start navigation").addVariant("Continue to "
                        + "route").build();

        return new RoutePreviewNavigationTemplate.Builder()
                .setItemList(
                        new ItemList.Builder()
                                .setOnSelectedListener(this::onRouteSelected)
                                .addItem(
                                        new Row.Builder()
                                                .setTitle(firstRoute)
                                                .addText("Via NE 8th Street")
                                                .build())
                                .addItem(
                                        new Row.Builder()
                                                .setTitle(secondRoute)
                                                .addText("Via NE 1st Ave")
                                                .build())
                                .addItem(
                                        new Row.Builder()
                                                .setTitle(thirdRoute)
                                                .addText("Via NE 4th Street")
                                                .build())
                                .setOnItemsVisibilityChangedListener(this::onRoutesVisible)
                                .build())
                .setNavigateAction(
                        new Action.Builder()
                                .setTitle(navigateActionText)
                                .setOnClickListener(this::onNavigate)
                                .build())
                .setTitle("Routes")
                .setHeaderAction(Action.BACK)
                .build();
    }

    private void onNavigate() {
        CarToast.makeText(getCarContext(), "Navigation Requested", LENGTH_LONG * 2).show();
    }

    private void onRouteSelected(int index) {
        CarToast.makeText(getCarContext(), "Selected route: " + index, LENGTH_LONG).show();
    }

    private void onRoutesVisible(int startIndex, int endIndex) {
        CarToast.makeText(
                getCarContext(),
                "Visible routes: [" + startIndex + "," + endIndex + "]",
                LENGTH_LONG)
                .show();
    }
}
