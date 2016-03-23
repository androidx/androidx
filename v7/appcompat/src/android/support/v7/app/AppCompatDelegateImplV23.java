/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.app;

import android.app.UiModeManager;
import android.content.Context;
import android.view.ActionMode;
import android.view.Window;

class AppCompatDelegateImplV23 extends AppCompatDelegateImplV14 {

    private final UiModeManager mUiModeManager;

    AppCompatDelegateImplV23(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);

        mUiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
    }

    @Override
    Window.Callback wrapWindowCallback(Window.Callback callback) {
        // Override the window callback so that we can intercept onWindowStartingActionMode(type)
        // calls
        return new AppCompatWindowCallbackV23(callback);
    }

    @ApplyableNightMode
    @Override
    int mapNightMode(@NightMode final int mode) {
        if (mode == MODE_NIGHT_AUTO
                && mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_AUTO) {
            // If we're set to AUTO and the system's auto night mode is already enabled,
            // we'll just let the system handle it by returning FOLLOW_SYSTEM
            return MODE_NIGHT_FOLLOW_SYSTEM;
        }
        return super.mapNightMode(mode);
    }

    class AppCompatWindowCallbackV23 extends AppCompatWindowCallbackV14 {
        AppCompatWindowCallbackV23(Window.Callback callback) {
            super(callback);
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            if (isHandleNativeActionModesEnabled()) {
                switch (type) {
                    case ActionMode.TYPE_PRIMARY:
                        // We only take over if the type is TYPE_PRIMARY
                        return startAsSupportActionMode(callback);
                }
            }
            // Else, let the call fall through to the wrapped callback
            return super.onWindowStartingActionMode(callback, type);
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            // No-op on API 23+
            return null;
        }
    }
}
