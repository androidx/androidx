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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.tabtemplates;

import static androidx.car.app.model.Action.APP_ICON;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Tab;
import androidx.car.app.model.TabContents;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a screen that demonstrates usage of the full screen {@link TabTemplate} with loading
 * state.
 */
public final class TabTemplateLoadingDemoScreen extends Screen {
    private static final String LOADING_ID = "Loading";
    private static final String SEARCH_ID = "Search";

    private final Map<String, Tab> mTabs;
    private TabTemplate.Builder mTabTemplateBuilder;
    private String mActiveContentId;

    public TabTemplateLoadingDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mTabs = new HashMap<>();
    }

    @Override
    public @NonNull Template onGetTemplate() {
        mTabTemplateBuilder = new TabTemplate.Builder(new TabTemplate.TabCallback() {
            @Override
            public void onTabSelected(@NonNull String tabContentId) {
                mActiveContentId = tabContentId;
                invalidate();
            }
        })
                .setHeaderAction(APP_ICON);

        mTabs.clear();

        Tab loadingTab =
                new Tab.Builder()
                        .setTitle(getCarContext().getString(R.string.tab_title_loading))
                        .setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                                R.drawable.ic_explore_white_24dp)).build())
                        .setContentId(LOADING_ID)
                        .build();
        mTabTemplateBuilder.addTab(loadingTab);

        Tab otherTab =
                new Tab.Builder()
                        .setTitle(getCarContext().getString(R.string.tab_title_search))
                        .setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                                R.drawable.ic_face_24px)).build())
                        .setContentId(SEARCH_ID)
                        .build();
        mTabTemplateBuilder.addTab(otherTab);

        if (mActiveContentId == null) {
            mActiveContentId = LOADING_ID;
        }

        Template contentTemplate;
        switch (mActiveContentId) {
            case LOADING_ID:
                contentTemplate = new ListTemplate.Builder().setLoading(true).build();
                break;
            case SEARCH_ID:
                contentTemplate = createSearchTemplate();
                break;
            default:
                throw new IllegalStateException("What happened?!");
        }

        return mTabTemplateBuilder.setTabContents(
                new TabContents.Builder(contentTemplate).build()).setActiveTabContentId(
                mActiveContentId).build();
    }

    private Template createSearchTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 1; i <= 6; ++i) {
            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle(
                                    getCarContext().getString(R.string.back_caps_action_title) + " "
                                            + i)
                            .addText("Tap to go back to previous screen")
                            .setOnClickListener(() -> getScreenManager().pop())
                            .build());
        }
        SearchTemplate searchTemplate = new SearchTemplate.Builder(
                new SearchTemplate.SearchCallback() {
                    @Override
                    public void onSearchSubmitted(@NonNull String searchText) {
                        CarToast.makeText(getCarContext(), searchText,
                                CarToast.LENGTH_SHORT).show();
                    }
                })
                .setItemList(listBuilder.build())
                .setShowKeyboardByDefault(true)
                .build();
        return searchTemplate;
    }

}
