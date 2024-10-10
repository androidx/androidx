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

package androidx.car.app.sample.showcase.common.screens.settings;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.sample.showcase.common.screens.settings.LoadingScreen.loadingScreenTemplate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseService;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating selectable lists. */
public final class ParkedVsDrivingDemoScreen extends Screen implements DefaultLifecycleObserver {

    // Adding loading state parameters
    private static final int LOADING_TIME_MILLIS = 1000;
    private boolean mIsFinishedLoading;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mShouldLoadScreens;

    public ParkedVsDrivingDemoScreen(@NonNull CarContext carContext) {
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


        listBuilder.addItem(
                new Row.Builder()
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(() -> onClick(
                                        getCarContext().getString(R.string.parked_toast_msg))))
                        .setTitle(getCarContext().getString(R.string.parked_only_title))
                        .addText(getCarContext().getString(R.string.parked_only_text))
                        .build());

        // Add a few rows with long subtext
        for (int rowIndex = 1; rowIndex < 5; rowIndex++) {
            listBuilder.addItem(
                    buildRowForTemplate(
                            R.string.other_row_title_prefix,
                            rowIndex,
                            R.string.long_line_text));
        }

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.parking_vs_driving_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .build())
                .build();
    }

    private void onClick(String text) {
        CarToast.makeText(getCarContext(), text, LENGTH_LONG).show();
    }

    private Row buildRowForTemplate(int title, int index, int subText) {
        String rowTitle = getCarContext().getString(title) + " " + (index + 1);
        return new Row.Builder()
                .setTitle(rowTitle)
                .addText(getCarContext().getString(subText))
                .build();
    }
}
