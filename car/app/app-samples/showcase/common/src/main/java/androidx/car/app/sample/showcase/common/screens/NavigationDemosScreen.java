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

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.model.Row;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.ArrivedDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.JunctionImageDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.LoadingDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigatingDemoScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigationMapOnlyScreen;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.NavigationNotificationsDemoScreen;
import androidx.car.app.sample.showcase.common.screens.paging.PagedListTemplate;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A screen showing a list of navigation demos */
public final class NavigationDemosScreen extends PagedListTemplate.RowList {
    private final @NonNull CarContext mCarContext;

    private NavigationDemosScreen(@NonNull CarContext carContext) {
        this.mCarContext = carContext;
    }

    /** Creates a screen with navigation demos */
    public static @NonNull Screen createScreen(@NonNull CarContext carContext) {
        return new PagedListTemplate(new NavigationDemosScreen(carContext), carContext);
    }

    @Override
    protected @NonNull List<Row> getRows(@NonNull ScreenManager screenManager) {
        List<Row> screenList = new ArrayList<>();

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.loading_demo_title),
                new LoadingDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.arrived_demo_title),
                new ArrivedDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.junction_image_demo_title),
                new JunctionImageDemoScreen(mCarContext)
        ));

        screenList.add(createRow(
                screenManager,
                mCarContext.getString(R.string.navigating_demo_title),
                new NavigatingDemoScreen(mCarContext)
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
        return screenList;
    }

    @Override
    protected @NonNull String getTemplateTitle() {
        return mCarContext.getString(R.string.nav_demos_title);
    }


    private Row createRow(ScreenManager screenManager, String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> screenManager.push(screen))
                .build();
    }
}
