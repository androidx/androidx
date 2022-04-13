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

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.navigation.routing.NavigationTemplateDemoScreen;
import androidx.core.graphics.drawable.IconCompat;

/** A screen showing a list of navigation demos */
public final class NavigationDemosScreen extends Screen {
    public NavigationDemosScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_explore_white_24dp))
                                        .build(),
                                Row.IMAGE_TYPE_ICON)
                        .setTitle(getCarContext().getString(R.string.nav_template_demos_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new NavigationTemplateDemoScreen(
                                                                getCarContext())))
                        .setBrowsable(true)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(
                                R.string.place_list_nav_template_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new PlaceListNavigationTemplateDemoScreen(
                                                                getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(
                                R.string.route_preview_template_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new RoutePreviewDemoScreen(getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(
                                R.string.notification_template_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new NavigationNotificationsDemoScreen(
                                                                getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.nav_map_template_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new NavigationMapOnlyScreen(getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.map_template_pane_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new MapTemplateDemoScreen(getCarContext())))
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.nav_demos_title))
                .setHeaderAction(Action.BACK)
                .build();
    }
}
