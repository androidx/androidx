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

package androidx.car.app.sample.showcase.common.templates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.CarToast.LENGTH_SHORT;

import androidx.annotation.NonNull;
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

/** A screen that demonstrates the search template. */
public class SearchTemplateDemoScreen extends Screen {

    public SearchTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 1; i <= 6; ++i) {
            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle("Title " + i)
                            .addText("First line of text")
                            .addText("Second line of text")
                            .build());
        }

        SearchCallback searchListener =
                new SearchCallback() {
                    @Override
                    public void onSearchTextChanged(@NonNull String searchText) {
                        CarToast.makeText(
                                getCarContext(),
                                "Search changed: " + searchText,
                                LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onSearchSubmitted(@NonNull String searchText) {
                        CarToast.makeText(
                                getCarContext(),
                                "Search submitted: " + searchText,
                                LENGTH_LONG)
                                .show();
                    }
                };

        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setTitle("Settings")
                                .setOnClickListener(
                                        () ->
                                                CarToast.makeText(
                                                        getCarContext(),
                                                        "Clicked Settings",
                                                        LENGTH_LONG)
                                                        .show())
                                .build())
                .build();

        return new SearchTemplate.Builder(searchListener)
                .setSearchHint("Search here")
                .setHeaderAction(Action.BACK)
                .setShowKeyboardByDefault(false)
                .setItemList(listBuilder.build())
                .setActionStrip(actionStrip)
                .build();
    }
}
