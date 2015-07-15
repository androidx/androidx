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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.internal.view.SupportActionModeWrapper;
import android.view.ActionMode;
import android.view.Window;

class AppCompatDelegateImplV14 extends AppCompatDelegateImplV11 {

    private TwilightManager mTwilightManager;
    private boolean mHandleNativeActionModes = true; // defaults to true

    @NightMode
    private int mNightMode = MODE_NIGHT_AUTO;

    AppCompatDelegateImplV14(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    @Override
    Window.Callback wrapWindowCallback(Window.Callback callback) {
        // Override the window callback so that we can intercept onWindowStartingActionMode()
        // calls
        return new AppCompatWindowCallbackV14(callback);
    }

    @Override
    public void setHandleNativeActionModesEnabled(boolean enabled) {
        mHandleNativeActionModes = enabled;
    }

    @Override
    public boolean isHandleNativeActionModesEnabled() {
        return mHandleNativeActionModes;
    }

    @Override
    public void applyDayNight() {
        if (!isSystemControllingNightMode()) {
            // If the system is not controlling night mode, let's do it ourselves
            switch (mNightMode) {
                case MODE_NIGHT_AUTO:
                    // For auto, we need to check whether it's night or not
                    setDayNightUsingTwilight();
                    break;
                default:
                    // Else, we'll set the value directly
                    setDayNightConfiguration(mNightMode);
                    break;
            }
        }
    }

    @Override
    public void setNightMode(@NightMode int mode) {
        mNightMode = mode;
    }

    /**
     * If possible, updates the Activity's {@link uiMode} to match whether we are at night or not.
     */
    void setDayNightUsingTwilight() {
        // If the system isn't controlling night mode, we'll do it ourselves
        if (getTwilightManager().isNight()) {
            // If we're at 'night', set the night mode
            setDayNightConfiguration(MODE_NIGHT_YES);
        } else {
            // Else, set the day mode
            setDayNightConfiguration(MODE_NIGHT_NO);
        }
    }

    /**
     * Updates the {@link Resources} configuration {@code uiMode} with the
     * chosen {@code UI_MODE_NIGHT} value.
     */
    void setDayNightConfiguration(@NightMode int mode) {
        final Resources res = mContext.getResources();
        final Configuration conf = res.getConfiguration();
        final int currentNightMode = conf.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        int newNightMode = Configuration.UI_MODE_NIGHT_UNDEFINED;
        switch (mode) {
            case MODE_NIGHT_NO:
                newNightMode = Configuration.UI_MODE_NIGHT_NO;
                break;
            case MODE_NIGHT_YES:
                newNightMode = Configuration.UI_MODE_NIGHT_YES;
                break;
        }

        if (currentNightMode != newNightMode) {
            conf.uiMode = (conf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | newNightMode;
            res.updateConfiguration(conf, res.getDisplayMetrics());
        }
    }

    /**
     * Returns true if the system is controlling night mode.
     */
    private boolean isSystemControllingNightMode() {
        final UiModeManager uiModeManager =
                (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);

        if (Build.VERSION.SDK_INT < 23
                && uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR) {
            // Night mode only has an effect with car mode enabled on < API v23
            return false;
        }

        return uiModeManager.getNightMode() != UiModeManager.MODE_NIGHT_NO;
    }

    private TwilightManager getTwilightManager() {
        if (mTwilightManager == null) {
            mTwilightManager = new TwilightManager(mContext);
        }
        return mTwilightManager;
    }

    class AppCompatWindowCallbackV14 extends AppCompatWindowCallbackBase {
        AppCompatWindowCallbackV14(Window.Callback callback) {
            super(callback);
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            // We wrap in a support action mode on v14+ if enabled
            if (isHandleNativeActionModesEnabled()) {
                return startAsSupportActionMode(callback);
            }
            // Else, let the call fall through to the wrapped callback
            return super.onWindowStartingActionMode(callback);
        }

        /**
         * Wrap the framework {@link ActionMode.Callback} in a support action mode and
         * let AppCompat display it.
         */
        final ActionMode startAsSupportActionMode(ActionMode.Callback callback) {
            // Wrap the callback as a v7 ActionMode.Callback
            final SupportActionModeWrapper.CallbackWrapper callbackWrapper
                    = new SupportActionModeWrapper.CallbackWrapper(mContext, callback);

            // Try and start a support action mode using the wrapped callback
            final android.support.v7.view.ActionMode supportActionMode
                    = startSupportActionMode(callbackWrapper);

            if (supportActionMode != null) {
                // If we received a support action mode, wrap and return it
                return callbackWrapper.getActionModeWrapper(supportActionMode);
            }
            return null;
        }
    }
}
