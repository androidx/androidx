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

import static androidx.car.app.model.Action.BACK;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent.MapTemplateWithToggleDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent.MapWithGridTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent.MapWithListTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent.MapWithMessageTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent.MapWithPaneTemplateDemoScreen;
import androidx.car.app.versioning.CarAppApiLevels;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A screen demonstrating different template layouts. */
public final class MapWithContentDemoScreen extends Screen {
    public MapWithContentDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        List<Row> screenList = new ArrayList<>();
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_7) {
            screenList.add(buildRowForTemplate(new MapWithMessageTemplateDemoScreen(
                    getCarContext()),
                    R.string.map_with_message_demo_title));
            screenList.add(buildRowForTemplate(new MapWithGridTemplateDemoScreen(getCarContext()),
                    R.string.map_with_grid_demo_title));
        }

        screenList.add(buildRowForTemplate(new MapWithListTemplateDemoScreen(getCarContext()),
                R.string.map_template_list_demo_title));
        screenList.add(buildRowForTemplate(new MapWithPaneTemplateDemoScreen(getCarContext()),
                R.string.map_template_pane_demo_title));

        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            screenList.add(buildRowForTemplate(new MapTemplateWithToggleDemoScreen(getCarContext()),
                    R.string.map_template_toggle_demo_title));
        }

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
}
