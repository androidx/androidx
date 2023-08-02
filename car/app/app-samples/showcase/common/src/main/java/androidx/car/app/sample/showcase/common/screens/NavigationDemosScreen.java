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
import androidx.car.app.ScreenManager;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Row;
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
import androidx.car.app.sample.showcase.common.screens.paging.PagedListTemplate;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/** A screen showing a list of navigation demos */
public final class NavigationDemosScreen extends PagedListTemplate.RowList {
    private final @NonNull CarContext mCarContext;

    private NavigationDemosScreen(@NonNull CarContext carContext) {
        this.mCarContext = carContext;
    }

    /** Creates a screen with navigation demos */
    @NonNull
    public static Screen createScreen(@NonNull CarContext carContext) {
        return new PagedListTemplate(new NavigationDemosScreen(carContext), carContext);
    }

    @NonNull
    @Override
    protected List<Row> getRows(@NonNull ScreenManager screenManager) {
        List<Row> screenList = new ArrayList<>();

        screenList.add(createRow(
                screenManager,
                buildCarIcon(R.drawable.ic_explore_white_24dp),
                mCarContext.getString(R.string.nav_template_demos_title),
                new NavigationTemplateDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.place_list_nav_template_demo_title),
                new PlaceListNavigationTemplateDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.route_preview_template_demo_title),
                new RoutePreviewDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.notification_template_demo_title),
                new NavigationNotificationsDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.nav_map_template_demo_title),
                new NavigationMapOnlyScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.place_list_template_demo_title),
                new PlaceListTemplateBrowseDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.map_template_list_demo_title),
                new MapTemplateWithListDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.map_template_pane_demo_title),
                new MapTemplateWithPaneDemoScreen(mCarContext)
        ));

        if (mCarContext.getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            screenList.add(
                    createRow(
                            screenManager,
                            mCarContext.getString(R.string.map_template_toggle_demo_title),
                            new MapTemplateWithToggleDemoScreen(mCarContext)));
        }

        return screenList;
    }

    @NonNull
    @Override
    protected String getTemplateTitle() {
        return mCarContext.getString(R.string.nav_demos_title);
    }

    private CarIcon buildCarIcon(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        mCarContext,
                        imageId))
                .build();
    }

    private Row createRow(ScreenManager screenManager, String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> screenManager.push(screen))
                .build();
    }

    private Row createRow(ScreenManager screenManager, CarIcon image, String title, Screen screen) {
        return new Row.Builder()
                .setImage(image)
                .setTitle(title)
                .setOnClickListener(() -> screenManager.push(screen))
                .setBrowsable(true)
                .build();
    }
}
