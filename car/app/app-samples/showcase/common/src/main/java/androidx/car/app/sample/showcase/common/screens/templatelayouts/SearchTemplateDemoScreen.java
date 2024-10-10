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

package androidx.car.app.sample.showcase.common.screens.templatelayouts;

import static androidx.car.app.CarToast.LENGTH_LONG;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.SearchTemplate.SearchCallback;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;

import org.jspecify.annotations.NonNull;

/** A screen that demonstrates the search template. */
public class SearchTemplateDemoScreen extends Screen {

    public SearchTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 1; i <= 6; ++i) {
            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle(getCarContext().getString(R.string.title_prefix) + " " + i)
                            .addText(getCarContext().getString(R.string.first_line_text))
                            .addText(getCarContext().getString(R.string.second_line_text))
                            .build());
        }

        SearchCallback searchListener =
                new SearchCallback() {
                    @Override
                    public void onSearchTextChanged(@NonNull String searchText) {
                    }

                    @Override
                    public void onSearchSubmitted(@NonNull String searchText) {
                        CarToast.makeText(
                                        getCarContext(),
                                        "Searched for " + searchText,
                                        LENGTH_LONG)
                                .show();
                    }
                };

        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setTitle(getCarContext().getString(R.string.settings_action_title))
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.settings_toast_msg),
                                                        LENGTH_LONG)
                                                .show())
                                .build())
                .build();

        return new SearchTemplate.Builder(searchListener)
                .setSearchHint(getCarContext().getString(R.string.search_hint))
                .setHeaderAction(Action.BACK)
                .setShowKeyboardByDefault(false)
                .setItemList(listBuilder.build())
                .setActionStrip(actionStrip)
                .build();
    }
}
