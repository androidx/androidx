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

import android.support.v7.appcompat.R;
import android.support.v7.internal.widget.NativeActionModeAwareLayout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

class ActionBarImplHC extends ActionBarImplBase
        implements NativeActionModeAwareLayout.OnActionModeForChildListener {

    final NativeActionModeAwareLayout mNativeActionModeAwareLayout;
    private ActionMode mCurActionMode;

    public ActionBarImplHC(ActionBarActivity activity, Callback callback) {
        super(activity, callback);

        // NativeActionModeAwareLayout is used to notify us whena native Action Mode is started
        mNativeActionModeAwareLayout = (NativeActionModeAwareLayout) activity
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

    @Override
    public void show() {
        super.show();
        if (mCurActionMode != null) {
            mCurActionMode.finish();
        }
    }

    @Override
    public void hide() {
        super.hide();
        if (mCurActionMode != null) {
            mCurActionMode.finish();
        }
    }

    @Override
    boolean isShowHideAnimationEnabled() {
        // Only allow animation if we're not currently showing an action mode
        return mCurActionMode == null && super.isShowHideAnimationEnabled();
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
                showForActionMode();
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
            hideForActionMode();
            // Remove any reference to the mode
            mCurActionMode = null;
        }
    }
}