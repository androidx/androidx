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
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseService;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

/** A screen that demonstrates the message template. */
public class LatestFeatures extends Screen implements DefaultLifecycleObserver {

    private static final int LOADING_TIME_MILLIS = 1000;
    private boolean mIsFinishedLoading;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mShouldLoadScreens;

    public LatestFeatures(@NonNull CarContext carContext) {
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
        } else {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.latest_feature_details))
                    .setHeader(new Header.Builder().setTitle(getCarContext()
                            .getString(R.string.latest_feature_title))
                            .setStartHeaderAction(Action.BACK).build())
                    .build();
        }

    }
}
