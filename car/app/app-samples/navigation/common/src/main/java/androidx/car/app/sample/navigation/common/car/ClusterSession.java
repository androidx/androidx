/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.sample.navigation.common.R;
import androidx.car.app.sample.navigation.common.model.Instruction;
import androidx.car.app.sample.navigation.common.nav.NavigationService;
import androidx.core.graphics.drawable.IconCompat;

import java.util.List;

/** Session class for the Navigation sample app. */
class ClusterSession extends Session implements NavigationScreen.Listener {
    static final String TAG = ClusterSession.class.getSimpleName();

    @Nullable
    NavigationScreen mNavigationScreen;

    @Nullable
    SurfaceRenderer mNavigationCarSurface;

    // A reference to the navigation service used to get location updates and routing.
    @Nullable
    NavigationService mService;

    @NonNull
    Action mSettingsAction;

    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        Log.i(TAG, "In onCreateScreen()");

        mSettingsAction =
                new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(), R.drawable.ic_settings))
                                        .build())
                        .setOnClickListener(
                                () -> {
                                    getCarContext()
                                            .getCarService(ScreenManager.class)
                                            .push(new SettingsScreen(getCarContext()));
                                })
                        .build();

        mNavigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
        mNavigationScreen =
                new NavigationScreen(getCarContext(), mSettingsAction, this, mNavigationCarSurface);

        String action = intent.getAction();
        if (action != null && CarContext.ACTION_NAVIGATE.equals(action)) {
            CarToast.makeText(
                    getCarContext(),
                    "Navigation intent: " + intent.getDataString(),
                    CarToast.LENGTH_LONG)
                    .show();
        }

        return mNavigationScreen;
    }

    @Override
    public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
        mNavigationCarSurface.onCarConfigurationChanged();
    }

    @Override
    public void executeScript(@NonNull List<Instruction> instructions) {
        if (mService != null) {
            mService.executeInstructions(instructions);
        }
    }

    @Override
    public void stopNavigation() {
        if (mService != null) {
            mService.stopNavigation();
        }
    }
}
