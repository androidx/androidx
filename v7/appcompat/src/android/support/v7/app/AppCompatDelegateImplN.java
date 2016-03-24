/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.Window;

import java.util.List;

class AppCompatDelegateImplN extends AppCompatDelegateImplV23 {

    AppCompatDelegateImplN(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    @Override
    Window.Callback wrapWindowCallback(Window.Callback callback) {
        return new AppCompatWindowCallbackN(callback);
    }

    class AppCompatWindowCallbackN extends AppCompatWindowCallbackV23 {
        AppCompatWindowCallbackN(Window.Callback callback) {
            super(callback);
        }

        @Override
        public void onProvideKeyboardShortcuts(
                List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {
            final PanelFeatureState panel = getPanelState(Window.FEATURE_OPTIONS_PANEL, true);
            if (panel != null && panel.menu != null) {
                // The menu provided is one created by PhoneWindow which we don't actually use.
                // Instead we'll pass through our own...
                super.onProvideKeyboardShortcuts(data, panel.menu, deviceId);
            } else {
                // If we don't have a menu, jump pass through the original instead
                super.onProvideKeyboardShortcuts(data, menu, deviceId);
            }
        }
    }
}
