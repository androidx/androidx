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

package androidx.appcompat.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.SupportActionModeWrapper;
import androidx.appcompat.view.WindowCallbackWrapper;
import androidx.appcompat.view.menu.MenuBuilder;

class AppCompatDelegateImplV14 extends AppCompatDelegateImplV9 {

    private static final String KEY_LOCAL_NIGHT_MODE = "appcompat:local_night_mode";

    @NightMode
    private int mLocalNightMode = MODE_NIGHT_UNSPECIFIED;
    private boolean mApplyDayNightCalled;

    private boolean mHandleNativeActionModes = true; // defaults to true

    private AutoNightModeManager mAutoNightModeManager;

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
    public boolean hasWindowFeature(int featureId) {
        return super.hasWindowFeature(featureId) || mWindow.hasFeature(featureId);
    }

    @Override
    View callActivityOnCreateView(View parent, String name, Context context, AttributeSet attrs) {
        // On Honeycomb+, Activity's private inflater factory will handle calling its
        // onCreateView(...)
        return null;
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
        boolean applied = false;

        @NightMode final int nightMode = getNightMode();
        @ApplyableNightMode final int modeToApply = mapNightMode(nightMode);
        if (modeToApply != MODE_NIGHT_FOLLOW_SYSTEM) {
            applied = updateForNightMode(modeToApply);
        }

        if (nightMode == MODE_NIGHT_AUTO) {
            // If we're already been started, we may need to setup auto mode again
            ensureAutoNightModeManager();
            mAutoNightModeManager.setup();
        }

        mApplyDayNightCalled = true;
        return applied;
    }

    @Override
    public void onStart() {
        // This will apply day/night if the time has changed, it will also call through to
        // setupAutoNightModeIfNeeded()
        applyDayNight();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Make sure we clean up any receivers setup for AUTO mode
        if (mAutoNightModeManager != null) {
            mAutoNightModeManager.cleanup();
        }
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
                Log.i(TAG, "setLocalNightMode() called with an unknown mode");
                break;
        }
    }

    @ApplyableNightMode
    int mapNightMode(@NightMode final int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
                ensureAutoNightModeManager();
                return mAutoNightModeManager.getApplyableNightMode();
            case MODE_NIGHT_UNSPECIFIED:
                // If we don't have a mode specified, just let the system handle it
                return MODE_NIGHT_FOLLOW_SYSTEM;
            default:
                return mode;
        }
    }

    @NightMode
    private int getNightMode() {
        return mLocalNightMode != MODE_NIGHT_UNSPECIFIED ? mLocalNightMode : getDefaultNightMode();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mLocalNightMode != MODE_NIGHT_UNSPECIFIED) {
            // If we have a local night mode set, save it
            outState.putInt(KEY_LOCAL_NIGHT_MODE, mLocalNightMode);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure we clean up any receivers setup for AUTO mode
        if (mAutoNightModeManager != null) {
            mAutoNightModeManager.cleanup();
        }
    }

    /**
     * Updates the {@link Resources} configuration {@code uiMode} with the
     * chosen {@code UI_MODE_NIGHT} value.
     */
    private boolean updateForNightMode(@ApplyableNightMode final int mode) {
        final Resources res = mContext.getResources();
        final Configuration conf = res.getConfiguration();
        final int currentNightMode = conf.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        final int newNightMode = (mode == MODE_NIGHT_YES)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;

        if (currentNightMode != newNightMode) {
            if (shouldRecreateOnNightModeChange()) {
                if (DEBUG) {
                    Log.d(TAG, "applyNightMode() | Night mode changed, recreating Activity");
                }
                // If we've already been created, we need to recreate the Activity for the
                // mode to be applied
                final Activity activity = (Activity) mContext;
                activity.recreate();
            } else {
                if (DEBUG) {
                    Log.d(TAG, "applyNightMode() | Night mode changed, updating configuration");
                }
                final Configuration config = new Configuration(conf);
                final DisplayMetrics metrics = res.getDisplayMetrics();

                // Update the UI Mode to reflect the new night mode
                config.uiMode = newNightMode | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
                res.updateConfiguration(config, metrics);

                // We may need to flush the Resources' drawable cache due to framework bugs.
                if (!(Build.VERSION.SDK_INT >= 26)) {
                    ResourcesFlusher.flush(res);
                }
            }
            return true;
        } else {
            if (DEBUG) {
                Log.d(TAG, "applyNightMode() | Skipping. Night mode has not changed: " + mode);
            }
        }
        return false;
    }

    private void ensureAutoNightModeManager() {
        if (mAutoNightModeManager == null) {
            mAutoNightModeManager = new AutoNightModeManager(TwilightManager.getInstance(mContext));
        }
    }

    @VisibleForTesting
    final AutoNightModeManager getAutoNightModeManager() {
        ensureAutoNightModeManager();
        return mAutoNightModeManager;
    }

    private boolean shouldRecreateOnNightModeChange() {
        if (mApplyDayNightCalled && mContext instanceof Activity) {
            // If we've already applyDayNight() (via setTheme), we need to check if the
            // Activity has configChanges set to handle uiMode changes
            final PackageManager pm = mContext.getPackageManager();
            try {
                final ActivityInfo info = pm.getActivityInfo(
                        new ComponentName(mContext, mContext.getClass()), 0);
                // We should return true (to recreate) if configChanges does not want to
                // handle uiMode
                return (info.configChanges & ActivityInfo.CONFIG_UI_MODE) == 0;
            } catch (PackageManager.NameNotFoundException e) {
                // This shouldn't happen but let's not crash because of it, we'll just log and
                // return true (since most apps will do that anyway)
                Log.d(TAG, "Exception while getting ActivityInfo", e);
                return true;
            }
        }
        return false;
    }

    class AppCompatWindowCallbackV14 extends WindowCallbackWrapper {
        AppCompatWindowCallbackV14(Window.Callback callback) {
            super(callback);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return AppCompatDelegateImplV14.this.dispatchKeyEvent(event)
                    || super.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return super.dispatchKeyShortcutEvent(event)
                    || AppCompatDelegateImplV14.this.onKeyShortcut(event.getKeyCode(), event);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL && !(menu instanceof MenuBuilder)) {
                // If this is an options menu but it's not an AppCompat menu, we eat the event
                // and return false
                return false;
            }
            return super.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public void onContentChanged() {
            // We purposely do not propagate this call as this is called when we install
            // our sub-decor rather than the user's content
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            final MenuBuilder mb = menu instanceof MenuBuilder ? (MenuBuilder) menu : null;

            if (featureId == Window.FEATURE_OPTIONS_PANEL && mb == null) {
                // If this is an options menu but it's not an AppCompat menu, we eat the event
                // and return false
                return false;
            }

            // On ICS and below devices, onPreparePanel calls menu.hasVisibleItems() to determine
            // if a panel is prepared. This interferes with any initially invisible items, which
            // are later made visible. We workaround it by making hasVisibleItems() always
            // return true during the onPreparePanel call.
            if (mb != null) {
                mb.setOverrideVisibleItems(true);
            }

            final boolean handled = super.onPreparePanel(featureId, view, menu);

            if (mb != null) {
                mb.setOverrideVisibleItems(false);
            }

            return handled;
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            super.onMenuOpened(featureId, menu);
            AppCompatDelegateImplV14.this.onMenuOpened(featureId);
            return true;
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            super.onPanelClosed(featureId, menu);
            AppCompatDelegateImplV14.this.onPanelClosed(featureId);
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
            final androidx.appcompat.view.ActionMode supportActionMode
                    = startSupportActionMode(callbackWrapper);

            if (supportActionMode != null) {
                // If we received a support action mode, wrap and return it
                return callbackWrapper.getActionModeWrapper(supportActionMode);
            }
            return null;
        }
    }

    @VisibleForTesting
    final class AutoNightModeManager {
        private TwilightManager mTwilightManager;
        private boolean mIsNight;

        private BroadcastReceiver mAutoTimeChangeReceiver;
        private IntentFilter mAutoTimeChangeReceiverFilter;

        AutoNightModeManager(@NonNull TwilightManager twilightManager) {
            mTwilightManager = twilightManager;
            mIsNight = twilightManager.isNight();
        }

        @ApplyableNightMode
        final int getApplyableNightMode() {
            mIsNight = mTwilightManager.isNight();
            return mIsNight ? MODE_NIGHT_YES : MODE_NIGHT_NO;
        }

        final void dispatchTimeChanged() {
            final boolean isNight = mTwilightManager.isNight();
            if (isNight != mIsNight) {
                mIsNight = isNight;
                applyDayNight();
            }
        }

        final void setup() {
            cleanup();

            // If we're set to AUTO, we register a receiver to be notified on time changes. The
            // system only sends the tick out every minute, but that's enough fidelity for our use
            // case
            if (mAutoTimeChangeReceiver == null) {
                mAutoTimeChangeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (DEBUG) {
                            Log.d("AutoTimeChangeReceiver", "onReceive | Intent: " + intent);
                        }
                        dispatchTimeChanged();
                    }
                };
            }
            if (mAutoTimeChangeReceiverFilter == null) {
                mAutoTimeChangeReceiverFilter = new IntentFilter();
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIME_CHANGED);
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIME_TICK);
            }
            mContext.registerReceiver(mAutoTimeChangeReceiver, mAutoTimeChangeReceiverFilter);
        }

        final void cleanup() {
            if (mAutoTimeChangeReceiver != null) {
                mContext.unregisterReceiver(mAutoTimeChangeReceiver);
                mAutoTimeChangeReceiver = null;
            }
        }
    }
}
