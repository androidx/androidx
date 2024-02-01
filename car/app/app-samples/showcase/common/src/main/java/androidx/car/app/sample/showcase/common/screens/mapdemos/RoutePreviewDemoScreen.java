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

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.CarToast.LENGTH_SHORT;

import android.text.SpannableString;

import androidx.annotation.NonNull;
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
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModels;
import androidx.core.graphics.drawable.IconCompat;

import java.util.concurrent.TimeUnit;

/** Creates a screen using the {@link RoutePreviewNavigationTemplate} */
public final class RoutePreviewDemoScreen extends Screen {
    public RoutePreviewDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    private boolean mIsFavorite;

    private int mItemLimit;

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
                .addText(titleText)
                .addAction(action)
                .build();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Action action = new Action.Builder()
                .setIcon(buildCarIconWithResources(R.drawable.outline_info_24))
                .setOnClickListener(() -> CarToast.makeText(getCarContext(),
                        R.string.secondary_action_toast, LENGTH_LONG).show())
                .build();

        // Adjust the item limit according to the car constrains.
        mItemLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST);

        ItemList.Builder itemListBuilder = new ItemList.Builder()
                .setOnSelectedListener(this::onRouteSelected)
                .setOnItemsVisibilityChangedListener(this::onRoutesVisible);

        for (int i = 0; i < mItemLimit; i++) {
            itemListBuilder.addItem(createRow(i, action));
        }

        // Set text variants for the navigate action text.
        CarText navigateActionText =
                new CarText.Builder(getCarContext().getString(R.string.continue_start_nav))
                        .addVariant(getCarContext().getString(R.string.continue_route))
                        .build();

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

        return new RoutePreviewNavigationTemplate.Builder()
                .setItemList(itemListBuilder.build())
                .setNavigateAction(
                        new Action.Builder()
                                .setTitle(navigateActionText)
                                .setOnClickListener(this::onNavigate)
                                .build())
                .setMapActionStrip(RoutingDemoModels.getMapActionStrip(getCarContext()))
                .setHeader(header)
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

    private void onRoutesVisible(int startIndex, int endIndex) {
        CarToast.makeText(
                        getCarContext(),
                        getCarContext().getString(R.string.visible_routes_toast_msg)
                                + ": [" + startIndex + "," + endIndex + "]",
                        LENGTH_LONG)
                .show();
    }

    private CarIcon buildCarIconWithResources(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }
}
