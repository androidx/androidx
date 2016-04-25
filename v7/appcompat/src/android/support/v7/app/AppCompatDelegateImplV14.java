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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.view.SupportActionModeWrapper;
import android.util.Log;
import android.view.ActionMode;
import android.view.Window;

class AppCompatDelegateImplV14 extends AppCompatDelegateImplV11 {

    private static final String KEY_LOCAL_NIGHT_MODE = "appcompat:local_night_mode";

    private static TwilightManager sTwilightManager;

    @NightMode
    private int mLocalNightMode = MODE_NIGHT_UNSPECIFIED;
    private boolean mApplyDayNightCalled;

    private boolean mHandleNativeActionModes = true; // defaults to true

    AppCompatDelegateImplV14(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && mLocalNightMode == MODE_NIGHT_UNSPECIFIED) {
            // If we have a icicle and we haven't had a local night mode set yet, try and read
            // it from the icicle
            mLocalNightMode = savedInstanceState.getInt(KEY_LOCAL_NIGHT_MODE,
                    MODE_NIGHT_UNSPECIFIED);
        }
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
    public boolean applyDayNight() {
        mApplyDayNightCalled = true;

        final int modeToApply = mapNightMode(mLocalNightMode == MODE_NIGHT_UNSPECIFIED
                ? getDefaultNightMode()
                : mLocalNightMode);

        if (modeToApply != MODE_NIGHT_FOLLOW_SYSTEM) {
            // If we're not following the system, we many need to update the configuration
            return updateConfigurationForNightMode(modeToApply);
        }
        return false;
    }

    @Override
    public void setLocalNightMode(@NightMode final int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                if (mLocalNightMode != mode) {
                    mLocalNightMode = mode;
                    if (mApplyDayNightCalled) {
                        // If we've already applied day night, re-apply since we won't be
                        // called again
                        applyDayNight();
                    }
                }
                break;
            default:
                Log.d(TAG, "setLocalNightMode() called with an unknown mode");
                break;
        }
    }

    @ApplyableNightMode
    int mapNightMode(@NightMode final int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
                return getTwilightManager().isNight() ? MODE_NIGHT_YES : MODE_NIGHT_NO;
            case MODE_NIGHT_UNSPECIFIED:
                // If we don't have a mode specified, just let the system handle it
                return MODE_NIGHT_FOLLOW_SYSTEM;
            default:
                return mode;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mLocalNightMode != MODE_NIGHT_UNSPECIFIED) {
            // If we have a local night mode set, save it
            outState.putInt(KEY_LOCAL_NIGHT_MODE, mLocalNightMode);
        }
    }

    /**
     * Updates the {@link Resources} configuration {@code uiMode} with the
     * chosen {@code UI_MODE_NIGHT} value.
     */
    private boolean updateConfigurationForNightMode(@ApplyableNightMode final int mode) {
        final Resources res = mContext.getResources();
        final Configuration conf = res.getConfiguration();
        final int currentNightMode = conf.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        final int newNightMode = (mode == MODE_NIGHT_YES)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;

        if (currentNightMode != newNightMode) {
            final Configuration newConf = new Configuration(conf);
            newConf.uiMode = (newConf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | newNightMode;
            res.updateConfiguration(newConf, null);
            return true;
        }
        return false;
    }

    private TwilightManager getTwilightManager() {
        if (sTwilightManager == null) {
            sTwilightManager = new TwilightManager(mContext.getApplicationContext());
        }
        return sTwilightManager;
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
