/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.GridTemplateMenuDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.ListTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.MessageTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.PaneTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.SearchTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.SignInTemplateDemoScreen;

/** A screen demonstrating different template layouts. */
public final class TemplateLayoutsDemoScreen extends Screen {

    public TemplateLayoutsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(new ListTemplateDemoScreen(getCarContext()),
                R.string.list_template_demo_title));
        listBuilder.addItem(buildRowForTemplate(new GridTemplateMenuDemoScreen(getCarContext()),
                R.string.grid_template_menu_demo_title));
        listBuilder.addItem(buildRowForTemplate(new MessageTemplateDemoScreen(getCarContext()),
                R.string.msg_template_demo_title));
        listBuilder.addItem(buildRowForTemplate(new PaneTemplateDemoScreen(getCarContext()),
                R.string.pane_template_demo_title));
        listBuilder.addItem(buildRowForTemplate(new SearchTemplateDemoScreen(getCarContext()),
                R.string.search_template_demo_title));
        listBuilder.addItem(buildRowForTemplate(new SignInTemplateDemoScreen(getCarContext()),
                R.string.sign_in_template_demo_title));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.template_layouts_demo_title))
                .setHeaderAction(BACK)
                .build();
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }
}
