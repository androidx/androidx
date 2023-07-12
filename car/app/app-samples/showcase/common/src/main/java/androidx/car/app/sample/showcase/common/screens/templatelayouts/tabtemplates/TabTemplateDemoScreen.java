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
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Tab;
import androidx.car.app.model.TabContents;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a screen that demonstrates usage of the full screen {@link TabTemplate}.
 */
public final class TabTemplateDemoScreen extends Screen {
    private static final int LIST_SIZE = 10;
    private static final int[] TITLE_RES_IDS = new int[]{
            R.string.tab_title_message,
            R.string.tab_title_pane,
            R.string.tab_title_list,
            R.string.tab_title_grid
    };

    private static final int[] ICON_RES_IDS = new int[]{
            R.drawable.ic_explore_white_24dp,
            R.drawable.ic_face_24px,
            R.drawable.ic_place_white_24dp,
            R.drawable.ic_favorite_white_24dp
    };

    private final Map<String, Tab> mTabs;
    private final Map<String, TabContents> mTabContentsMap;
    private TabTemplate.Builder mTabTemplateBuilder;
    private String mActiveContentId;

    public TabTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mTabs = new HashMap<>();
        mTabContentsMap = new HashMap<>();
        mActiveContentId = null;
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
        }).setHeaderAction(APP_ICON);

        mTabContentsMap.clear();
        mTabs.clear();

        for (int i = 0; i < ICON_RES_IDS.length; i++) {
            String contentId = String.valueOf(i);

            Template contentTemplate;
            switch (i) {
                case 0:
                    contentTemplate = createShortMessageTemplate();
                    break;
                case 1:
                    contentTemplate = createPaneTemplate();
                    break;
                case 2:
                    contentTemplate = createListTemplate();
                    break;
                case 3:
                default:
                    contentTemplate = createGridTemplate();
                    break;
            }
            TabContents tabContents = new TabContents.Builder(contentTemplate).build();
            mTabContentsMap.put(contentId, tabContents);

            Tab.Builder tabBuilder = new Tab.Builder()
                    .setTitle(getCarContext().getString(TITLE_RES_IDS[i]))
                    .setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                            ICON_RES_IDS[i])).build())
                    .setContentId(contentId);
            if (TextUtils.isEmpty(mActiveContentId) && i == 0) {
                mActiveContentId = contentId;
                mTabTemplateBuilder.setTabContents(tabContents);
            } else if (TextUtils.equals(mActiveContentId, contentId)) {
                mTabTemplateBuilder.setTabContents(tabContents);
            }

            Tab tab = tabBuilder.build();
            mTabs.put(tab.getContentId(), tab);
            mTabTemplateBuilder.addTab(tab);
        }
        return mTabTemplateBuilder.setActiveTabContentId(mActiveContentId).build();
    }

    private ListTemplate createListTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 0; i < LIST_SIZE; i++) {
            listBuilder.addItem(buildRowForTemplate(String.valueOf(i), true));
        }
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .addAction(createFabBackAction())
                .build();
    }

    private Row buildRowForTemplate(CharSequence title, boolean clickable) {
        Row.Builder rowBuilder = new Row.Builder()
                .setTitle(title);
        if (clickable) {
            rowBuilder.setOnClickListener(() -> CarToast.makeText(getCarContext(), title,
                    CarToast.LENGTH_SHORT).show());
        }
        return rowBuilder.build();
    }

    private GridTemplate createGridTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        for (int i = 0; i < LIST_SIZE; i++) {
            listBuilder.addItem(buildGridItemForTemplate(String.valueOf(i)));
        }
        return new GridTemplate.Builder()
                .setSingleList(listBuilder.build())
                .addAction(createFabBackAction())
                .build();
    }

    private GridItem buildGridItemForTemplate(CharSequence title) {
        return new GridItem.Builder()
                .setImage(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                                R.drawable.ic_emoji_food_beverage_white_48dp)).build(),
                        GridItem.IMAGE_TYPE_ICON)
                .setTitle(title)
                .build();
    }

    private MessageTemplate createShortMessageTemplate() {
        Action action = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.back_caps_action_title))
                .setIcon(CarIcon.BACK)
                .setOnClickListener(() -> getScreenManager().pop())
                .build();
        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.msg_template_demo_text))
                .setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                        R.drawable.ic_launcher)).build())
                .addAction(action)
                .build();
    }

    private PaneTemplate createPaneTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder()
                .setImage(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                        R.drawable.ic_launcher)).build());
        for (int i = 0; i < LIST_SIZE; i++) {
            paneBuilder.addRow(buildRowForTemplate(String.valueOf(i), false));
        }
        return new PaneTemplate.Builder(paneBuilder.build())
                .build();
    }

    private Action createFabBackAction() {
        Action action = new Action.Builder()
                .setIcon(CarIcon.BACK)
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(() -> getScreenManager().pop())
                .build();
        return action;
    }

}
