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
    private final int mItemLimit;

    public MiscTemplateDemosScreen(@NonNull CarContext carContext, int page, int limit) {
        super(carContext);
        mPage = page;
        mItemLimit = limit;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        Row[] screenArray = new Row[]{
                createRow("Pane Template Demo",
                        new PaneTemplateDemoScreen(getCarContext())),
                createRow("List Template Demo",
                        new ListTemplateDemoScreen(getCarContext())),
                createRow("Place List Template Demo",
                        new PlaceListTemplateBrowseDemoScreen(getCarContext())),
                createRow("Search Template Demo",
                        new SearchTemplateDemoScreen(getCarContext())),
                createRow("Message Template Demo",
                        new MessageTemplateDemoScreen(getCarContext())),
                createRow("Grid Template Demo",
                        new GridTemplateDemoScreen(getCarContext())),
                createRow("Sign In Template Demo",
                        new SignInTemplateDemoScreen(getCarContext())),
                createRow("Long Message Template Demo",
                        new LongMessageTemplateDemoScreen(getCarContext()))
        };
        // If the screenArray size is under the limit, we will show all of them on the first page.
        // Otherwise we will show them in multiple pages.
        if (screenArray.length <= mItemLimit) {
            for (int i = 0; i < mItemLimit; i++) {
                listBuilder.addItem(screenArray[i]);
            }
        } else {
            int currentItemStartIndex = mPage * mItemLimit;
            int currentItemEndIndex = Math.min(currentItemStartIndex + mItemLimit,
                    screenArray.length);
            for (int i = currentItemStartIndex; i < currentItemEndIndex; i++) {
                listBuilder.addItem(screenArray[i]);
            }
        }
        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Misc Templates Demos")
                .setHeaderAction(BACK);
        // If the current page does not cover the last item, we will show a More button
        if ((mPage + 1) * mItemLimit < screenArray.length && mPage + 1 < MAX_PAGES) {
            builder.setActionStrip(new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle("More")
                            .setOnClickListener(() -> getScreenManager().push(
                                    new MiscTemplateDemosScreen(getCarContext(), mPage + 1,
                                            mItemLimit)))
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