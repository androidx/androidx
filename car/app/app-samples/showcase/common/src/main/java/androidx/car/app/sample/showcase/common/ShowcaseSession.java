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

package androidx.car.app.sample.showcase.common;

import static androidx.car.app.sample.showcase.common.ShowcaseService.INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.sample.showcase.common.misc.PreSeedingFlowScreen;
import androidx.car.app.sample.showcase.common.misc.ResultDemoScreen;
import androidx.car.app.sample.showcase.common.navigation.NavigationNotificationsDemoScreen;
import androidx.car.app.sample.showcase.common.navigation.SurfaceRenderer;
import androidx.car.app.sample.showcase.common.navigation.routing.NavigatingDemoScreen;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/** Session class for the Showcase sample app. */
public class ShowcaseSession extends Session implements DefaultLifecycleObserver {
    static final String URI_SCHEME = "samples";
    static final String URI_HOST = "showcase";

    @Nullable
    private SurfaceRenderer mRenderer;

    @NonNull
    @Override
    public Screen onCreateScreen(@NonNull Intent intent) {
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(this);
        mRenderer = new SurfaceRenderer(getCarContext(), lifecycle);

        if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
            // Handle the navigation Intent by pushing first the "home" screen onto the stack, then
            // returning the screen that we want to show a template for.
            // Doing this allows the app to go back to the previous screen when the user clicks on a
            // back action.
            getCarContext()
                    .getCarService(ScreenManager.class)
                    .push(new StartScreen(getCarContext(), this));
            return new NavigatingDemoScreen(getCarContext());
        }

        if (getCarContext().getCallingComponent() != null) {
            // Similarly, if the application has been called "for result", we push a "home"
            // screen onto the stack and return the results demo screen.
            getCarContext()
                    .getCarService(ScreenManager.class)
                    .push(new StartScreen(getCarContext(), this));
            return new ResultDemoScreen(getCarContext());
        }

        // For demo purposes this uses a shared preference setting to store whether we should
        // pre-seed the screen back stack. This allows the app to have a way to go back to the
        // home/start screen making the home/start screen the 0th position.
        // For a real application, it would probably check if it has all the needed system
        // permissions, and if any are missing, it would pre-seed the start screen and return a
        // screen that will send the user to the phone to grant the needed permissions.
        boolean shouldPreSeedBackStack =
                getCarContext()
                        .getSharedPreferences(ShowcaseService.SHARED_PREF_KEY, Context.MODE_PRIVATE)
                        .getBoolean(ShowcaseService.PRE_SEED_KEY, false);
        if (shouldPreSeedBackStack) {
            // Reset so that we don't require it next time
            getCarContext()
                    .getSharedPreferences(ShowcaseService.SHARED_PREF_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(ShowcaseService.PRE_SEED_KEY, false)
                    .apply();

            getCarContext()
                    .getCarService(ScreenManager.class)
                    .push(new StartScreen(getCarContext(), this));
            return new PreSeedingFlowScreen(getCarContext());
        }
        return new StartScreen(getCarContext(), this);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.i("SHOWCASE", "onDestroy");
    }

    @Override
    public void onNewIntent(@NonNull Intent intent) {
        // Process various deeplink intents.

        ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);

        if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
            // If the Intent is to navigate, and we aren't already, push the navigation screen.
            if (screenManager.getTop() instanceof NavigatingDemoScreen) {
                return;
            }
            screenManager.push(new NavigatingDemoScreen(getCarContext()));
            return;
        }

        if (getCarContext().getCallingComponent() != null) {
            // Remove any other instances of the results screen.
            screenManager.popToRoot();
            screenManager.push(new ResultDemoScreen(getCarContext()));
            return;
        }

        Uri uri = intent.getData();
        if (uri != null
                && URI_SCHEME.equals(uri.getScheme())
                && URI_HOST.equals(uri.getSchemeSpecificPart())) {

            Screen top = screenManager.getTop();
            // No-op
            if (INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP.equals(uri.getFragment())) {
                if (!(top instanceof NavigationNotificationsDemoScreen)) {
                    screenManager.push(new NavigationNotificationsDemoScreen(getCarContext()));
                }
            }
        }
    }

    @Override
    public void onCarConfigurationChanged(@NonNull Configuration configuration) {
        if (mRenderer != null) {
            mRenderer.onCarConfigurationChanged();
        }
    }

    /** Tells the session whether to update the renderer to show car hardware information. */
    public void setCarHardwareSurfaceRendererEnabledState(boolean isEnabled) {
        mRenderer.setCarHardwareSurfaceRendererEnabledState(isEnabled);
    }
}
