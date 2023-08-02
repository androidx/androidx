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
import androidx.car.app.sample.showcase.common.R;

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
                createRow(getCarContext().getString(R.string.pane_template_demo_title),
                        new PaneTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.list_template_demo_title),
                        new ListTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.place_list_template_demo_title),
                        new PlaceListTemplateBrowseDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.search_template_demo_title),
                        new SearchTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.msg_template_demo_title),
                        new MessageTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.grid_template_demo_title),
                        new GridTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.sign_in_template_demo_title),
                        new SignInTemplateDemoScreen(getCarContext())),
                createRow(getCarContext().getString(R.string.long_msg_template_demo_title),
                        new LongMessageTemplateDemoScreen(getCarContext()))
        };
        // If the screenArray size is under the limit, we will show all of them on the first page.
        // Otherwise we will show them in multiple pages.
        if (screenArray.length <= mItemLimit) {
            for (int i = 0; i < screenArray.length; i++) {
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
                .setTitle(getCarContext().getString(R.string.misc_templates_demos_title))
                .setHeaderAction(BACK);
        // If the current page does not cover the last item, we will show a More button
        if ((mPage + 1) * mItemLimit < screenArray.length && mPage + 1 < MAX_PAGES) {
            builder.setActionStrip(new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.more_action_title))
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