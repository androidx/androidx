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

package androidx.car.app.sample.showcase.common.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.MapTemplateWithListDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.MapTemplateWithPaneDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.MapTemplateWithToggleDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigationMapOnlyScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigationNotificationsDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigationTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.PlaceListNavigationTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.PlaceListTemplateBrowseDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutePreviewDemoScreen;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/** A screen showing a list of navigation demos */
public final class NavigationDemosScreen extends Screen {
    private static final int MAX_PAGES = 2;

    private final int mPage;

    public NavigationDemosScreen(@NonNull CarContext carContext) {
        this(carContext, /* page= */ 0);
    }

    public NavigationDemosScreen(@NonNull CarContext carContext, int page) {
        super(carContext);
        mPage = page;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        int listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_LIST);

        List<Row> screenList = new ArrayList<>();

        screenList.add(createRow(buildCarIcon(R.drawable.ic_explore_white_24dp),
                getCarContext().getString(R.string.nav_template_demos_title),
                new NavigationTemplateDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(
                        R.string.place_list_nav_template_demo_title),
                new PlaceListNavigationTemplateDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(
                        R.string.route_preview_template_demo_title),
                new RoutePreviewDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(
                        R.string.notification_template_demo_title),
                new NavigationNotificationsDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(R.string.nav_map_template_demo_title),
                new NavigationMapOnlyScreen(getCarContext())));

        screenList.add(createRow(
                getCarContext().getString(R.string.place_list_template_demo_title),
                new PlaceListTemplateBrowseDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(R.string.map_template_list_demo_title),
                new MapTemplateWithListDemoScreen(getCarContext())));

        screenList.add(createRow(getCarContext().getString(R.string.map_template_pane_demo_title),
                new MapTemplateWithPaneDemoScreen(getCarContext())));

        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            screenList.add(
                    createRow(getCarContext().getString(R.string.map_template_toggle_demo_title),
                        new MapTemplateWithToggleDemoScreen(getCarContext())));
        }

        // If the screenArray size is under the limit, we will show all of them on the first page.
        // Otherwise we will show them in multiple pages.
        if (screenList.size() <= listLimit) {
            for (int i = 0; i < screenList.size(); i++) {
                listBuilder.addItem(screenList.get(i));
            }
        } else {
            int currentItemStartIndex = mPage * listLimit;
            int currentItemEndIndex = Math.min(currentItemStartIndex + listLimit,
                    screenList.size());
            for (int i = currentItemStartIndex; i < currentItemEndIndex; i++) {
                listBuilder.addItem(screenList.get(i));
            }
        }

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.nav_demos_title))
                .setHeaderAction(Action.BACK);

        // If the current page does not cover the last item, we will show a More button
        if ((mPage + 1) * listLimit < screenList.size() && mPage + 1 < MAX_PAGES) {
            builder.setActionStrip(new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.more_action_title))
                            .setOnClickListener(() -> {
                                getScreenManager().push(
                                        new NavigationDemosScreen(getCarContext(), mPage + 1));
                            })
                            .build())
                    .build());
        }

        return builder.build();
    }

    private CarIcon buildCarIcon(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }

    private Row createRow(String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> getScreenManager().push(screen))
                .build();
    }

    private Row createRow(CarIcon image, String title, Screen screen) {
        return new Row.Builder()
                .setImage(image)
                .setTitle(title)
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }
}
