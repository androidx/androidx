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

package androidx.car.app.sample.navigation.common.car;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.SearchTemplate.SearchCallback;
import androidx.car.app.model.Template;
import androidx.car.app.sample.navigation.common.model.DemoScripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Screen for showing entering a search and showing initial results. */
public final class SearchScreen extends Screen {

    @NonNull
    private final Action mSettingsAction;

    @NonNull
    private final SurfaceRenderer mSurfaceRenderer;

    private ItemList mItemList = withNoResults(new ItemList.Builder()).build();
    final List<String> mTitles = new ArrayList<>();

    @Nullable
    private String mSearchText;
    private final List<String> mFakeTitles =
            new ArrayList<>(Arrays.asList("Starbucks", "Shell", "Costco", "Aldi", "Safeway"));

    public SearchScreen(
            @NonNull CarContext carContext,
            @NonNull Action settingsAction,
            @NonNull SurfaceRenderer surfaceRenderer) {
        super(carContext);
        mSettingsAction = settingsAction;
        mSurfaceRenderer = surfaceRenderer;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new SearchTemplate.Builder(
                new SearchCallback() {
                    @Override
                    public void onSearchTextChanged(@NonNull String searchText) {
                        doSearch(searchText);
                    }

                    @Override
                    public void onSearchSubmitted(@NonNull String searchTerm) {
                        // When the user presses the search key use the top item in the list
                        // as the
                        // result and simulate as if the user had pressed that.
                        if (mTitles.size() > 0) {
                            onClickSearch(mTitles.get(0));
                        }
                    }
                })
                .setHeaderAction(Action.BACK)
                .setShowKeyboardByDefault(false)
                .setItemList(mItemList)
                .setInitialSearchText(mSearchText == null ? "" : mSearchText)
                .build();
    }

    void doSearch(String searchText) {
        mSearchText = searchText;
        mTitles.clear();
        ItemList.Builder builder = new ItemList.Builder();
        if (searchText.isEmpty()) {
            withNoResults(builder);
        } else {
            // Create some fake data entries.
            for (String title : mFakeTitles) {
                mTitles.add(title);
                builder.addItem(
                        new Row.Builder()
                                .setTitle(title)
                                .setOnClickListener(() -> onClickSearch(title))
                                .build());
            }
        }
        mItemList = builder.build();
        invalidate();
        return;
    }

    void onClickSearch(@NonNull String searchText) {
        getScreenManager()
                .pushForResult(
                        new RoutePreviewScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
                        this::onRouteSelected);
    }

    private static ItemList.Builder withNoResults(ItemList.Builder builder) {
        return builder.setNoItemsMessage("No Results");
    }

    private void onRouteSelected(@Nullable Object previewResult) {
        int previewIndex = previewResult == null ? -1 : (int) previewResult;
        if (previewIndex < 0) {
            return;
        }
        // Start the same demo instructions. More will be added in the future.
        setResult(DemoScripts.getNavigateHome(getCarContext()));
        finish();
    }
}
