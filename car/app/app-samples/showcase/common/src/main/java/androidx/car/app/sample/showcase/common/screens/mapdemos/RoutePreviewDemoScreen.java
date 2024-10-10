/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.CarToast.LENGTH_SHORT;

import android.text.SpannableString;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.TimeUnit;

/** Creates a screen using the new {@link MapWithContentTemplate} */
public final class RoutePreviewDemoScreen extends Screen {
    private boolean mIsFavorite;

    private int mItemLimit;
    private final RoutingDemoModelFactory mRoutingDemoModelFactory;

    public RoutePreviewDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mRoutingDemoModelFactory = new RoutingDemoModelFactory(carContext);
    }

    private CarText createRouteText(int index) {
        switch (index) {
            case 0:
                // Set text variants for the first route.
                SpannableString shortRouteLongText = new SpannableString(
                        "   \u00b7 ---------------- " + getCarContext().getString(
                                R.string.short_route)
                                + " -------------------");
                shortRouteLongText.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1,
                        0);
                SpannableString firstRouteShortText = new SpannableString(
                        "   \u00b7 " + getCarContext().getString(R.string.short_route));
                firstRouteShortText.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1,
                        0);
                return new CarText.Builder(shortRouteLongText)
                        .addVariant(firstRouteShortText)
                        .build();
            case 1:
                SpannableString lessBusyRouteText =
                        new SpannableString(
                                "   \u00b7 " + getCarContext().getString(R.string.less_busy));
                lessBusyRouteText.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(24)), 0, 1,
                        0);
                return new CarText.Builder(lessBusyRouteText).build();
            case 2:
                SpannableString hovRouteText =
                        new SpannableString(
                                "   \u00b7 " + getCarContext().getString(R.string.hov_friendly));
                hovRouteText.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867)), 0, 1, 0);
                return new CarText.Builder(hovRouteText).build();
            default:
                SpannableString routeText =
                        new SpannableString(
                                "   \u00b7 " + getCarContext().getString(R.string.long_route));
                routeText.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867L + index)),
                        0, 1, 0);
                return new CarText.Builder(routeText).build();
        }
    }

    private Row createRow(int index, Action action) {
        CarText route = createRouteText(index);
        String titleText = "Via NE " + (index + 4) + "th Street";

        return new Row.Builder()
                .setTitle(route)
                .setOnClickListener(() -> onRouteSelected(index))
                .addText(titleText)
                .addAction(action)
                .build();
    }

    @Override
    public @NonNull Template onGetTemplate() {
        // Adjust the item limit according to the car constrains.
        mItemLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST);

        CarIcon navigateActionIcon = new CarIcon.Builder(IconCompat.createWithResource(
                getCarContext(), R.drawable.ic_place_white_24dp)).build();
        Action navigateAction = new Action.Builder()
                .setIcon(navigateActionIcon)
                .setOnClickListener(this::onNavigate)
                .build();

        ItemList.Builder itemListBuilder = new ItemList.Builder();

        for (int i = 0; i < mItemLimit; i++) {
            itemListBuilder.addItem(createRow(i, navigateAction));
        }

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                mIsFavorite
                                                        ? R.drawable.ic_favorite_filled_white_24dp
                                                        : R.drawable.ic_favorite_white_24dp))
                                        .build())
                        .setOnClickListener(() -> {
                            mIsFavorite = !mIsFavorite;
                            CarToast.makeText(
                                            getCarContext(),
                                            mIsFavorite
                                                    ? getCarContext()
                                                    .getString(R.string.favorite_toast_msg)
                                                    : getCarContext().getString(
                                                            R.string.not_favorite_toast_msg),
                                            LENGTH_SHORT)
                                    .show();
                            invalidate();
                        })
                        .build())
                .addEndHeaderAction(new Action.Builder()
                        .setOnClickListener(() -> finish())
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_close_white_24dp))
                                        .build())
                        .build())
                .setTitle(getCarContext().getString(R.string.route_preview_template_demo_title))
                .build();

        // RoutePreview Template is deprecated. Demo using new MapWithContent Template
        return new MapWithContentTemplate.Builder()
                .setContentTemplate(new ListTemplate.Builder()
                        .setHeader(header)
                        .setSingleList(itemListBuilder.build())
                        .build())
                .setMapController(new MapController.Builder().setMapActionStrip(
                        mRoutingDemoModelFactory.getMapActionStrip()).build())
                .build();
    }

    private void onNavigate() {
        CarToast.makeText(getCarContext(),
                getCarContext().getString(R.string.nav_requested_toast_msg),
                LENGTH_LONG).show();
    }

    private void onRouteSelected(int index) {
        CarToast.makeText(getCarContext(),
                getCarContext().getString(R.string.selected_route_toast_msg) + ": " + index,
                LENGTH_LONG).show();
    }
}
