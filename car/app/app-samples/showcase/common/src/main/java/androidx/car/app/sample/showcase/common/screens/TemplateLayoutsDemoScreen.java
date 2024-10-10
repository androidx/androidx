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
import androidx.car.app.sample.showcase.common.screens.templatelayouts.TabTemplateLayoutsDemoScreen;
import androidx.car.app.versioning.CarAppApiLevels;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A screen demonstrating different template layouts. */
public final class TemplateLayoutsDemoScreen extends Screen {
    private static final int MAX_PAGES = 2;

    private int mPage;

    public TemplateLayoutsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mPage = 0;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        List<Row> screenList = new ArrayList<>();
        screenList.add(buildRowForTemplate(new ListTemplateDemoScreen(getCarContext()),
                R.string.list_template_demo_title));
        screenList.add(buildRowForTemplate(new GridTemplateMenuDemoScreen(getCarContext()),
                R.string.grid_template_menu_demo_title));
        screenList.add(buildRowForTemplate(new MessageTemplateDemoScreen(getCarContext()),
                R.string.msg_template_demo_title));
        screenList.add(buildRowForTemplate(new PaneTemplateDemoScreen(getCarContext()),
                R.string.pane_template_demo_title));
        screenList.add(buildRowForTemplate(new SearchTemplateDemoScreen(getCarContext()),
                R.string.search_template_demo_title));
        screenList.add(buildRowForTemplate(new SignInTemplateDemoScreen(getCarContext()),
                R.string.sign_in_template_demo_title));
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            screenList.add(buildRowForTemplate(new TabTemplateLayoutsDemoScreen(getCarContext()),
                    R.string.tab_template_layouts_demo_title));
        }

        int listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_LIST);

        ItemList.Builder listBuilder = new ItemList.Builder();
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

        Header.Builder headerBuilder = new Header.Builder()
                .setStartHeaderAction(BACK)
                .setTitle(getCarContext().getString(R.string.template_layouts_demo_title));

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build());

        // If the current page does not cover the last item, we will show a More button
        if ((mPage + 1) * listLimit < screenList.size() && mPage + 1 < MAX_PAGES) {
            headerBuilder.addEndHeaderAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.more_action_title))
                            .setOnClickListener(() -> {
                                mPage++;
                                invalidate();
                            })
                            .build());
        }
        return builder.setHeader(headerBuilder.build()).build();
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }
}
