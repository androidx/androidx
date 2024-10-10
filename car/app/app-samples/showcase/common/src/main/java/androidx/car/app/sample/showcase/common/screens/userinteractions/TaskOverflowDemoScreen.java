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

package androidx.car.app.sample.showcase.common.screens.userinteractions;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
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

import org.jspecify.annotations.NonNull;

/** A screen demonstrating Task Overflow for the different templates */
public class TaskOverflowDemoScreen extends Screen {

    public TaskOverflowDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    private Row createRow(int index) {
        switch (index) {
            case 0:
                return new Row.Builder()
                        .setTitle("    ")
                        .addText(getCarContext().getString(R.string.application_overflow_subtitle1))
                        .addText(getCarContext().getString(R.string.application_overflow_subtitle2))

                        .build();
            case 1:
                return buildRowForTemplate(new ListTemplateDemoScreen(getCarContext()),
                        R.string.list_template_demo_title);
            case 2:
                return buildRowForTemplate(new GridTemplateMenuDemoScreen(getCarContext()),
                        R.string.grid_template_menu_demo_title);
            case 3:
                return buildRowForTemplate(new MessageTemplateDemoScreen(getCarContext()),
                        R.string.msg_template_demo_title);
            case 4:
                return buildRowForTemplate(new PaneTemplateDemoScreen(getCarContext()),
                        R.string.pane_template_demo_title);
            case 5:
                return buildRowForTemplate(new SearchTemplateDemoScreen(getCarContext()),
                        R.string.search_template_demo_title);
            case 6:
                return buildRowForTemplate(new SignInTemplateDemoScreen(getCarContext()),
                        R.string.sign_in_template_demo_title);
            default:
                return new Row.Builder()
                        .setTitle(
                                getCarContext().getString(R.string.other_row_title_prefix) + (index
                                        + 1))
                        .addText(getCarContext().getString(R.string.other_row_text))
                        .build();
        }
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }

    @Override
    public @NonNull Template onGetTemplate() {
        int listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_LIST);
        if (listLimit >= 6) {
            listLimit = 6;
        }

        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 0; i < listLimit; i++) {
            listBuilder.addItem(createRow(i));
        }
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.application_overflow_title))
                        .setStartHeaderAction(Action.BACK)
                        .build())
                .build();
    }
}
