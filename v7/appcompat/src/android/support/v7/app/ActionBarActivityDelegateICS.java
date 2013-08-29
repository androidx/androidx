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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.WindowCompat;
import android.support.v7.internal.view.ActionModeWrapper;
import android.support.v7.internal.view.menu.MenuWrapperFactory;
import android.support.v7.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

class ActionBarActivityDelegateICS extends ActionBarActivityDelegate {
    Menu mMenu;

    ActionBarActivityDelegateICS(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    public ActionBar createSupportActionBar() {
        return new ActionBarImplICS(mActivity, mActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Set framework uiOptions from the support metadata value
        if (UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW.equals(getUiOptionsFromMetadata())) {
            mActivity.getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW,
                    ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        }

        super.onCreate(savedInstanceState);

        if (mHasActionBar) {
            // If action bar is requested by inheriting from the appcompat theme,
            // the system will not know about that. So explicitly request for an action bar.
            mActivity.requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);
        }
        if (mOverlayActionBar) {
            mActivity.requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        }

        /*
         * This goofy move needs some explanation.
         *
         * The verifier on older platform versions has some interesting side effects if
         * a class defines a method that takes a parameter of a type that doesn't exist.
         * In this case, that type is android.view.ActionMode. Therefore, ActionBarActivity
         * cannot override the onActionModeStarted/Finished methods without causing nastiness
         * when it is loaded on older platform versions.
         *
         * Since these methods are actually part of the window callback and not intrinsic to
         * Activity itself, we can install a little shim with the window instead that knows
         * about the ActionMode class. Note that this means that any new methods added to
         * Window.Callback in the future won't get proxied without updating the support lib,
         * but we shouldn't be adding new methods to public interfaces that way anyway...right? ;)
         */
        final Window w = mActivity.getWindow();
        w.setCallback(createWindowCallbackWrapper(w.getCallback()));
    }

    Window.Callback createWindowCallbackWrapper(Window.Callback cb) {
        return new WindowCallbackWrapper(cb);
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
    public void onContentChanged() {
        // Call straight through to the support version of the method
        mActivity.onSupportContentChanged();
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
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL || featureId == Window.FEATURE_ACTION_BAR) {
            if (mMenu == null) {
                mMenu = MenuWrapperFactory.createMenuWrapper(menu);
            }
            return mActivity.superOnCreatePanelMenu(featureId, mMenu);
        }
        return mActivity.superOnCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL || featureId == Window.FEATURE_ACTION_BAR) {
            return mActivity.superOnPreparePanel(featureId, view, mMenu);
        }
        return mActivity.superOnPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            item = MenuWrapperFactory.createMenuItemWrapper(item);
        }
        return mActivity.superOnMenuItemSelected(featureId, item);
    }

    @Override
    public void onTitleChanged(CharSequence title) {
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
            wrappedMode = new ActionModeWrapper(context, frameworkMode);
            wrappedCallback.setLastStartedActionMode(wrappedMode);
        }

        return wrappedMode;
    }

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

    public void onActionModeFinished(android.view.ActionMode mode) {
        mActivity.onSupportActionModeFinished(
                new ActionModeWrapper(getActionBarThemedContext(), mode));
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        mMenu = null;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        // Return null so that ActionBarDrawableToggle uses it's standard impl
        return null;
    }

    class WindowCallbackWrapper implements Window.Callback {
        final Window.Callback mWrapped;

        public WindowCallbackWrapper(Window.Callback wrapped) {
            mWrapped = wrapped;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return mWrapped.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return mWrapped.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return mWrapped.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return mWrapped.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return mWrapped.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return mWrapped.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            return mWrapped.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return mWrapped.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            return mWrapped.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return mWrapped.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return mWrapped.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            mWrapped.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            mWrapped.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            mWrapped.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            mWrapped.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            mWrapped.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            mWrapped.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return mWrapped.onSearchRequested();
        }

        @Override
        public android.view.ActionMode onWindowStartingActionMode(
                android.view.ActionMode.Callback callback) {
            return mWrapped.onWindowStartingActionMode(callback);
        }

        /*
         * And here are the money methods, the reason why this wrapper exists:
         */

        @Override
        public void onActionModeStarted(android.view.ActionMode mode) {
            mWrapped.onActionModeStarted(mode);
            ActionBarActivityDelegateICS.this.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(android.view.ActionMode mode) {
            mWrapped.onActionModeFinished(mode);
            ActionBarActivityDelegateICS.this.onActionModeFinished(mode);
        }
    }
}
