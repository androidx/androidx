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

import static androidx.car.app.model.Action.BACK;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/** An assortment of demos for different templates. */
public final class MiscTemplateDemosScreen extends Screen {
    private static final int MAX_PAGES = 2;

    private final int mPage;

    public MiscTemplateDemosScreen(@NonNull CarContext carContext) {
        this(carContext, 0);
    }

    public MiscTemplateDemosScreen(@NonNull CarContext carContext, int page) {
        super(carContext);
        mPage = page;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        switch (mPage) {
            case 0:
                listBuilder.addItem(createRow("Pane Template Demo",
                        new PaneTemplateDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("List Template Demo",
                        new ListTemplateDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("Place List Template Demo",
                        new PlaceListTemplateBrowseDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("Search Template Demo",
                        new SearchTemplateDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("Message Template Demo",
                        new MessageTemplateDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("Grid Template Demo",
                        new GridTemplateDemoScreen(getCarContext())));
                break;
            case 1:
                listBuilder.addItem(createRow("Sign In Template Demo",
                        new SignInTemplateDemoScreen(getCarContext())));
                listBuilder.addItem(createRow("Long Message Template Demo",
                        new LongMessageTemplateDemoScreen(getCarContext())));
                break;
        }

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Misc Templates Demos")
                .setHeaderAction(BACK);

        if (mPage + 1 < MAX_PAGES) {
            builder.setActionStrip(new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle("More")
                            .setOnClickListener(() -> {
                                getScreenManager().push(
                                        new MiscTemplateDemosScreen(getCarContext(), mPage + 1));
                            })
                            .build())
                    .build());
        }

        return builder.build();
    }

    private Row createRow(String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> getScreenManager().push(screen))
                .build();
    }
}
