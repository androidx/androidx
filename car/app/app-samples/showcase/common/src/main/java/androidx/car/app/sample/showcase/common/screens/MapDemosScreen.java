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

package androidx.car.app.sample.showcase.common.screens;

import static androidx.car.app.model.Action.BACK;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.mapdemos.MapWithContentDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.PlaceListNavigationTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.PlaceListTemplateBrowseDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.RoutePreviewDemoScreen;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A screen demonstrating different template layouts. */
public final class MapDemosScreen extends Screen {

    public MapDemosScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        List<Row> screenList = new ArrayList<>();
        screenList.add(buildBrowsableRow(new MapWithContentDemoScreen(getCarContext()),
                R.string.map_with_content_demo_title));
        screenList.add(buildRowForTemplate(new PlaceListNavigationTemplateDemoScreen(
                getCarContext()),
                R.string.place_list_nav_template_demo_title));
        screenList.add(buildRowForTemplate(new RoutePreviewDemoScreen(getCarContext()),
                R.string.route_preview_template_demo_title));
        screenList.add(buildRowForTemplate(new PlaceListTemplateBrowseDemoScreen(getCarContext()),
                R.string.place_list_template_demo_title));

        ItemList.Builder listBuilder = new ItemList.Builder();

        for (int i = 0; i < screenList.size(); i++) {
            listBuilder.addItem(screenList.get(i));
        }

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.map_demos_title))
                        .setStartHeaderAction(BACK)
                        .build())
                .build();
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .build();
    }

    private Row buildBrowsableRow(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }
}
