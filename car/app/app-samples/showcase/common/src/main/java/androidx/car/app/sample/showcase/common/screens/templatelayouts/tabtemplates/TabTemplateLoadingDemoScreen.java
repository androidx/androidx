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

import android.annotation.SuppressLint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Tab;
import androidx.car.app.model.TabContents;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a screen that demonstrates usage of the full screen {@link TabTemplate} with loading
 * state.
 */
public final class TabTemplateLoadingDemoScreen extends Screen {
    private static final int[] sTitleResIds = new int[]{
            R.string.tab_title_message, R.string.tab_title_search
    };

    private static final int[] sIconResIds = new int[]{
            R.drawable.ic_explore_white_24dp,
            R.drawable.ic_face_24px
    };

    private final Map<String, Tab> mTabs;
    private TabTemplate.Builder mTabTemplateBuilder;
    private String mActiveContentId;

    public TabTemplateLoadingDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mTabs = new HashMap<>();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        mTabTemplateBuilder = new TabTemplate.Builder(new TabTemplate.TabCallback() {
            @SuppressLint("SyntheticAccessor")
            @Override
            public void onTabSelected(@NonNull String tabContentId) {
                mActiveContentId = tabContentId;
                invalidate();
            }
        })
                .setHeaderAction(APP_ICON);

        mTabs.clear();

        for (int i = 0; i < 2; i++) {
            String contentId = String.valueOf(i);

            Tab.Builder tabBuilder = new Tab.Builder()
                    .setTitle(getCarContext().getString(sTitleResIds[i]))
                    .setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                            sIconResIds[i])).build())
                    .setContentId(contentId);
            if (TextUtils.isEmpty(mActiveContentId) && i == 0) {
                mActiveContentId = contentId;
            }

            Tab tab = tabBuilder.build();
            mTabs.put(tab.getContentId(), tab);
            mTabTemplateBuilder.addTab(tab);

            if (TextUtils.equals(mActiveContentId, contentId)) {
                if (i == 0) {
                    mTabTemplateBuilder.setLoading(true);
                } else {
                    mTabTemplateBuilder.setTabContents(createSearchTab());
                }
            }
        }
        return mTabTemplateBuilder.setActiveTabContentId(mActiveContentId).build();
    }

    private TabContents createSearchTab() {
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
        return new TabContents.Builder(searchTemplate).build();
    }

}
