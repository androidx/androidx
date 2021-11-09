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

package androidx.car.app.sample.showcase.common.misc;

import static androidx.car.app.model.Action.BACK;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** Creates a screen that shows loading states in a pane. */
public final class LoadingDemoScreen extends Screen implements DefaultLifecycleObserver {
    private static final int LOADING_TIME_MILLIS = 2000;
    private boolean mIsFinishedLoading = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public LoadingDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        // Post a message that finishes loading the template after some time.
        mHandler.postDelayed(
                () -> {
                    mIsFinishedLoading = true;
                    invalidate();
                },
                LOADING_TIME_MILLIS);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();

        if (!mIsFinishedLoading) {
            paneBuilder.setLoading(true);
        } else {
            paneBuilder.addRow(new Row.Builder().setTitle("Loading Complete!").build());
        }

        return new PaneTemplate.Builder(paneBuilder.build())
                .setTitle("Loading Demo")
                .setHeaderAction(BACK)
                .build();
    }
}
