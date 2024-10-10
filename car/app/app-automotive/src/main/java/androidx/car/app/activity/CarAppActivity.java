/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.car.app.SessionInfo.DISPLAY_TYPE_MAIN;

import static java.lang.System.identityHashCode;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarAppService;
import androidx.car.app.SessionInfo;

import org.jspecify.annotations.Nullable;

/**
 * The class representing a car app activity in the main display.
 *
 * <p>This class is responsible for binding to the host and rendering the content given by its
 * {@link androidx.car.app.CarAppService}.
 *
 * <p>Usage of {@link CarAppActivity} is only required for applications targeting Automotive OS.
 *
 * <h4>Activity Declaration</h4>
 *
 * <p>The app must declare and export this {@link CarAppActivity} in their manifest. In order for
 * it to show up in the car's app launcher. It must declare the {@code launchMode} to be
 * {@code singleTask}, and it must include a {@link Intent#CATEGORY_LAUNCHER} intent filter.
 *
 * For example:
 *
 * <pre>{@code
 * <activity
 *   android:name="androidx.car.app.activity.CarAppActivity"
 *   android:exported="true"
 *   android:launchMode="singleTask"
 *   android:label="@string/your_app_label">
 *
 *   <intent-filter>
 *     <action android:name="android.intent.action.MAIN" />
 *     <category android:name="android.intent.category.LAUNCHER" />
 *   </intent-filter>
 *   <meta-data android:name="distractionOptimized" android:value="true"/>
 * </activity>
 * }</pre>
 *
 * <p>See {@link androidx.car.app.CarAppService} for how to declare your app's
 * {@link CarAppService} in the manifest.
 *
 *
 * <h4>Distraction-optimized Activities</h4>
 *
 * <p>The activity must be the {@code distractionOptimized} meta-data set to {@code true}, in order
 * for it to be displayed while driving. This is the only activity that can have this meta-data
 * set to {@code true}, any other activities marked this way may cause the app to be rejected
 * during app submission.
 */
@SuppressLint({"ForbiddenSuperClass"})
public final class CarAppActivity extends BaseCarAppActivity {

    @SuppressLint({"ActionValue"})
    @VisibleForTesting
    static final String ACTION_RENDER = "android.car.template.host.RendererService";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String identifier;
        if (getIntent().getIdentifier() != null) {
            identifier = getIntent().getIdentifier();
        } else {
            identifier = String.valueOf(identityHashCode(this));
        }

        bindToViewModel(new SessionInfo(DISPLAY_TYPE_MAIN,
                identifier));
    }
}
