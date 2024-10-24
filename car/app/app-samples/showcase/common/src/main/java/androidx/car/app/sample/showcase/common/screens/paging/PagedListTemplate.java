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

package androidx.car.app.sample.showcase.common.screens.paging;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A generic screen featuring a paged list template.
 *
 * <p> Paging is useful to avoid truncation in situations where list length is aggressively limited.
 */
public class PagedListTemplate extends Screen {
    private static final int MAX_PAGES = 2;

    private final RowList mRowList;
    private final int mPage;

    public PagedListTemplate(@NonNull RowList rowList, @NonNull CarContext carContext) {
        this(rowList, carContext, /* page= */ 0);
    }

    public PagedListTemplate(@NonNull RowList rowList, @NonNull CarContext carContext, int page) {
        super(carContext);
        mRowList = rowList;
        mPage = page;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        int listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_LIST);

        List<Row> screenList = mRowList.getRows(getScreenManager());

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
                .setStartHeaderAction(Action.BACK)
                .setTitle(mRowList.getTemplateTitle());

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build());

        // If the current page does not cover the last item, we will show a More button
        if ((mPage + 1) * listLimit < screenList.size() && mPage + 1 < MAX_PAGES) {
            headerBuilder.addEndHeaderAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.more_action_title))
                            .setOnClickListener(() -> {
                                getScreenManager().push(
                                        new PagedListTemplate(
                                                mRowList,
                                                getCarContext(),
                                                mPage + 1
                                        )
                                );
                            })
                            .build());
        }

        return builder.setHeader(headerBuilder.build()).build();
    }

    /** A list of rows, used to populate a {@link PagedListTemplate} */
    public abstract static class RowList {
        protected abstract @NonNull List<Row> getRows(@NonNull ScreenManager screenManager);

        protected abstract @NonNull String getTemplateTitle();
    }
}
