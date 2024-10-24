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

package androidx.car.app.sample.showcase.common.screens.templatelayouts;

import static androidx.car.app.model.Action.BACK;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.ContentProviderIconsDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.EmptyListDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.RadioButtonListDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.SecondaryActionsAndDecorationDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.SectionedItemListDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.TextAndIconsDemosScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates.ToggleButtonListDemoScreen;
import androidx.car.app.versioning.CarAppApiLevels;

import org.jspecify.annotations.NonNull;

/**
 * Creates a screen that demonstrates usage of the full screen {@link ListTemplate} to display a
 * full-screen list.
 */
public final class ListTemplateDemoScreen extends Screen {

    public ListTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(buildRowForTemplate(new RadioButtonListDemoScreen(getCarContext()),
                R.string.radio_button_list_demo_title));
        listBuilder.addItem(buildRowForTemplate(new ToggleButtonListDemoScreen(getCarContext()),
                R.string.toggle_button_demo_title));
        listBuilder.addItem(buildRowForTemplate(new TextAndIconsDemosScreen(getCarContext()),
                R.string.text_icons_demo_title));
        listBuilder.addItem(buildRowForTemplate(new ContentProviderIconsDemoScreen(getCarContext()),
                R.string.content_provider_icons_demo_title));
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            listBuilder.addItem(buildRowForTemplate(
                    new SecondaryActionsAndDecorationDemoScreen(getCarContext()),
                    R.string.secondary_actions_decoration_button_demo_title));
        }
        listBuilder.addItem(buildRowForTemplate(
                new SectionedItemListDemoScreen(getCarContext()),
                R.string.sectioned_item_list_demo_title));

        // ========================================================================
        // WARNING: 6 demos have been added above, which is the max list size for some users/devs.
        // Demos added below may be truncated from the list in certain regions.
        // ========================================================================

        listBuilder.addItem(buildRowForTemplate(
                new EmptyListDemoScreen(getCarContext()),
                R.string.empty_list_demo_title));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.list_template_demo_title))
                        .setStartHeaderAction(BACK)
                        .build())
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
