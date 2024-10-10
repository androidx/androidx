/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.settings;

import static androidx.car.app.sample.showcase.common.screens.settings.LoadingScreen.loadingScreenTemplate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseService;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

/**
 * A {@link Screen} that shows examples on how to query for various content limits via the
 * {@lnk ConstraintManager} API.
 */
public class ContentLimitsDemoScreen extends Screen implements DefaultLifecycleObserver {

    // Loading State parameters
    private static final int LOADING_TIME_MILLIS = 1000;
    private boolean mIsFinishedLoading;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mShouldLoadScreens;

    public ContentLimitsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        mShouldLoadScreens =
                getCarContext()
                        .getSharedPreferences(ShowcaseService.SHARED_PREF_KEY, Context.MODE_PRIVATE)
                        .getBoolean(ShowcaseService.LOADING_KEY, false);
        if (mShouldLoadScreens) {
            // Post a message that finishes loading the template after some time.
            mHandler.postDelayed(
                    () -> {
                        mIsFinishedLoading = true;
                        invalidate();
                    },
                    LOADING_TIME_MILLIS);
        }
    }

    @Override
    public @NonNull Template onGetTemplate() {

        if (!mIsFinishedLoading && mShouldLoadScreens) {
            return loadingScreenTemplate(getCarContext());
        }

        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(R.string.list_limit,
                ConstraintManager.CONTENT_LIMIT_TYPE_LIST));

        listBuilder.addItem(buildRowForTemplate(R.string.grid_limit,
                ConstraintManager.CONTENT_LIMIT_TYPE_GRID));

        listBuilder.addItem(buildRowForTemplate(R.string.pane_limit,
                ConstraintManager.CONTENT_LIMIT_TYPE_PANE));

        listBuilder.addItem(buildRowForTemplate(R.string.place_list_limit,
                ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST));

        listBuilder.addItem(buildRowForTemplate(R.string.route_list_limit,
                ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.content_limits))
                        .setStartHeaderAction(Action.BACK)
                        .build())
                .build();
    }

    private Row buildRowForTemplate(int title, int contentLimitType) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .addText(Integer.toString(getCarContext()
                        .getCarService(ConstraintManager.class)
                        .getContentLimit(contentLimitType)))
                .build();
    }
}
