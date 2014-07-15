/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.appcompat.R;
import android.support.v7.internal.app.WindowDecorActionBar;
import android.support.v7.internal.widget.NativeActionModeAwareLayout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class ActionBarActivityDelegateHC extends ActionBarActivityDelegateBase
        implements NativeActionModeAwareLayout.OnActionModeForChildListener {

    private NativeActionModeAwareLayout mNativeActionModeAwareLayout;
    private ActionMode mCurActionMode;

    ActionBarActivityDelegateHC(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    void onSubDecorInstalled() {
        // NativeActionModeAwareLayout is used to notify us whena native Action Mode is started
        mNativeActionModeAwareLayout = (NativeActionModeAwareLayout) mActivity
                .findViewById(R.id.action_bar_root);

        // Can be null when using FEATURE_ACTION_BAR_OVERLAY
        if (mNativeActionModeAwareLayout != null) {
            mNativeActionModeAwareLayout.setActionModeForChildListener(this);
        }
    }

    // From NativeActionModeAwareLayout.OnActionModeForChildListener
    @Override
    public ActionMode.Callback onActionModeForChild(ActionMode.Callback callback) {
        return new CallbackWrapper(callback);
    }

    private class CallbackWrapper implements ActionMode.Callback {
        private final ActionMode.Callback mWrappedCallback;

        CallbackWrapper(ActionMode.Callback callback) {
            mWrappedCallback = callback;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            final boolean wrappedResult = mWrappedCallback.onCreateActionMode(mode, menu);
            if (wrappedResult) {
                // Keep reference to action mode
                mCurActionMode = mode;

                // Make sure that the compat Action Bar is shown
                if (getSupportActionBar() instanceof WindowDecorActionBar) {
                    ((WindowDecorActionBar) getSupportActionBar()).animateToMode(true);
                }
            }
            return wrappedResult;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrappedCallback.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrappedCallback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWrappedCallback.onDestroyActionMode(mode);

            // We previously shown the Action Bar for positioning purposes, now hide it again
            if (getSupportActionBar() instanceof WindowDecorActionBar) {
                ((WindowDecorActionBar) getSupportActionBar()).animateToMode(false);
            }
            // Remove any reference to the mode
            mCurActionMode = null;
        }
    }
}
