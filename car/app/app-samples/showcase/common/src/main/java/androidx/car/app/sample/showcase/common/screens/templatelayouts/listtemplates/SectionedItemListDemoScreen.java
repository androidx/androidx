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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating lists with sectioned item list */
public class SectionedItemListDemoScreen extends Screen {
    public SectionedItemListDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {

        ItemList.Builder listBuilderOne = new ItemList.Builder();
        listBuilderOne.addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext));

        listBuilderOne.addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext));

        ItemList.Builder listBuilderTwo = new ItemList.Builder();

        listBuilderTwo.addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext));

        return new ListTemplate.Builder()
                .addSectionedList(SectionedItemList.create(listBuilderOne.build(),
                        getCarContext()
                                .getString(R.string.sectioned_item_list_one_title)))
                .addSectionedList(SectionedItemList.create(listBuilderTwo.build(),
                        getCarContext()
                                .getString(R.string.sectioned_item_list_two_title)))
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext()
                                .getString(R.string.sectioned_item_list_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.home_caps_action_title))
                                .setOnClickListener(
                                        () -> getScreenManager().popToRoot())
                                .build())
                        .build())
                .build();
    }

    private Row buildRowForTemplate(int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .build();
    }
}
