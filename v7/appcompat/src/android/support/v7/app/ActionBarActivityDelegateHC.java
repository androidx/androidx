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

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.internal.view.ActionModeWrapper;
import android.support.v7.internal.view.menu.MenuWrapper;
import android.support.v7.view.ActionMode;
import android.support.v7.view.Menu;
import android.support.v7.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

class ActionBarActivityDelegateHC extends ActionBarActivityDelegate {

    Menu mMenu;

    ActionBarActivityDelegateHC(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    public ActionBar createSupportActionBar() {
        return new ActionBarImplHC(mActivity, mActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mHasActionBar) {
            // If action bar is requested by inheriting from the appcompat theme,
            // the system will not know about that. So explicitly request for an action bar.
            mActivity.requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);
        }
        if (mOverlayActionBar) {
            mActivity.requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onPostResume() {
    }

    @Override
    public void setContentView(View v) {
        mActivity.superSetContentView(v);
    }

    @Override
    public void setContentView(int resId) {
        mActivity.superSetContentView(resId);
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        mActivity.superSetContentView(v, lp);
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        mActivity.superAddContentView(v, lp);
    }

    @Override
    public boolean supportRequestWindowFeature(int featureId) {
        return mActivity.requestWindowFeature(featureId);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        // Do not create custom options menu on HC+
        return null;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            // This is a boundary where we transition from framework Menu objects to support
            // library Menu objects.
            if (mMenu == null) {
                mMenu = MenuWrapper.createMenuWrapper(frameworkMenu);
            }
            boolean show = mActivity.onCreateSupportOptionsMenu(mMenu);
            // FIXME: Reintroduce support options menu dispatch through facade.
            //show |= mActivity.mFragments.dispatchCreateSupportOptionsMenu(mMenu,
            //        mActivity.getCompatMenuInflater());
            return show;
        } else {
            return mActivity.superOnCreatePanelMenu(featureId, frameworkMenu);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            MenuItem wrappedItem = MenuWrapper.createMenuItemWrapper(frameworkItem);
            if (mActivity.onSupportOptionsItemSelected(wrappedItem)) {
                return true;
            }
            // FIXME: Reintroduce support options menu dispatch through facade.
            //if (mActivity.mFragments.dispatchSupportOptionsItemSelected(wrappedItem)) {
            //    return true;
            //}
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && mMenu != null) {
            boolean goforit = mActivity.onPrepareSupportOptionsMenu(mMenu);
            // FIXME: Reintroduce support options menu dispatch through facade.
            //goforit |= mActivity.mFragments.dispatchPrepareSupportOptionsMenu(mMenu);
            return goforit;
        } else {
            return mActivity.superOnPreparePanelMenu(featureId, view, menu);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        // Handled by framework
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }

        Context context = getActionBarThemedContext();

        ActionModeWrapper.CallbackWrapper wrappedCallback = new ActionModeWrapper.CallbackWrapper(
                context, callback);
        ActionModeWrapper wrappedMode = null;

        android.view.ActionMode frameworkMode = mActivity.startActionMode(wrappedCallback);

        if (frameworkMode != null) {
            wrappedMode = new ActionModeWrapper(context,
                    mActivity.startActionMode(wrappedCallback));
            wrappedCallback.setLastStartedActionMode(wrappedMode);
        }

        return wrappedMode;
    }

    @Override
    public void onActionModeStarted(android.view.ActionMode mode) {
        mActivity.onSupportActionModeStarted(
                new ActionModeWrapper(getActionBarThemedContext(), mode));
    }

    @Override
    void setSupportProgressBarVisibility(boolean visible) {
        mActivity.setProgressBarVisibility(visible);
    }

    @Override
    void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        mActivity.setProgressBarIndeterminateVisibility(visible);
    }

    @Override
    void setSupportProgressBarIndeterminate(boolean indeterminate) {
        mActivity.setProgressBarIndeterminate(indeterminate);
    }

    @Override
    void setSupportProgress(int progress) {
        mActivity.setProgress(progress);
    }

    @Override
    public void onActionModeFinished(android.view.ActionMode mode) {
        mActivity.onSupportActionModeFinished(
                new ActionModeWrapper(getActionBarThemedContext(), mode));
    }

    @Override
    public void supportInvalidateOptionsMenu() {
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

}
