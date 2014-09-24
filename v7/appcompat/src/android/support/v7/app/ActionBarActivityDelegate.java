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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.appcompat.R;
import android.support.v7.internal.app.WindowCallback;
import android.support.v7.internal.view.SupportMenuInflater;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

abstract class ActionBarActivityDelegate {

    static final String METADATA_UI_OPTIONS = "android.support.UI_OPTIONS";

    private static final String TAG = "ActionBarActivityDelegate";

    static ActionBarActivityDelegate createDelegate(ActionBarActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new ActionBarActivityDelegateHC(activity);
        } else {
            return new ActionBarActivityDelegateBase(activity);
        }
    }

    final ActionBarActivity mActivity;

    private ActionBar mActionBar;
    private MenuInflater mMenuInflater;

    // true if this activity has an action bar.
    boolean mHasActionBar;
    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;
    // true if this any action modes should overlay the activity content
    boolean mOverlayActionMode;
    // true if this activity is floating (e.g. Dialog)
    boolean mIsFloating;
    // The default window callback
    final WindowCallback mDefaultWindowCallback = new WindowCallback() {
        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem menuItem) {
            return mActivity.onMenuItemSelected(featureId, menuItem);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return mActivity.superOnCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View menuView, Menu menu) {
            return mActivity.superOnPreparePanel(featureId, menuView, menu);
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            mActivity.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return mActivity.onMenuOpened(featureId, menu);
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            return startSupportActionModeFromWindow(callback);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            return null;
        }
    };
    // The fake window callback we're currently using
    private WindowCallback mWindowCallback;
    private boolean mIsDestroyed;

    ActionBarActivityDelegate(ActionBarActivity activity) {
        mActivity = activity;
        mWindowCallback = mDefaultWindowCallback;
    }

    abstract ActionBar createSupportActionBar();

    final ActionBar getSupportActionBar() {
        // The Action Bar should be lazily created as hasActionBar
        // could change after onCreate
        if (mHasActionBar) {
            if (mActionBar == null) {
                mActionBar = createSupportActionBar();
            }
        }
        return mActionBar;
    }

    protected final void setSupportActionBar(ActionBar actionBar) {
        mActionBar = actionBar;
    }

    abstract void setSupportActionBar(Toolbar toolbar);

    MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            mMenuInflater = new SupportMenuInflater(getActionBarThemedContext());
        }
        return mMenuInflater;
    }

    void onCreate(Bundle savedInstanceState) {
        TypedArray a = mActivity.obtainStyledAttributes(R.styleable.Theme);

        if (!a.hasValue(R.styleable.Theme_windowActionBar)) {
            a.recycle();
            throw new IllegalStateException(
                    "You need to use a Theme.AppCompat theme (or descendant) with this activity.");
        }

        mHasActionBar = a.getBoolean(R.styleable.Theme_windowActionBar, false);
        mOverlayActionBar = a.getBoolean(R.styleable.Theme_windowActionBarOverlay, false);
        mOverlayActionMode = a.getBoolean(R.styleable.Theme_windowActionModeOverlay, false);
        mIsFloating = a.getBoolean(R.styleable.Theme_android_windowIsFloating, false);
        a.recycle();
    }

    abstract void onConfigurationChanged(Configuration newConfig);

    abstract void onStop();

    abstract void onPostResume();

    abstract void setContentView(View v);

    abstract void setContentView(int resId);

    abstract void setContentView(View v, ViewGroup.LayoutParams lp);

    abstract void addContentView(View v, ViewGroup.LayoutParams lp);

    abstract void onTitleChanged(CharSequence title);

    abstract void supportInvalidateOptionsMenu();

    abstract boolean supportRequestWindowFeature(int featureId);

    // Methods used to create and respond to options menu
    abstract View onCreatePanelView(int featureId);

    abstract boolean onPreparePanel(int featureId, View view, Menu menu);

    abstract void onPanelClosed(int featureId, Menu menu);

    abstract boolean onMenuOpened(int featureId, Menu menu);

    boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // Call straight through to onPrepareOptionsMenu, bypassing super.onPreparePanel().
            // This is because Activity.onPreparePanel() on <v4.1 calls menu.hasVisibleItems(),
            // which interferes with the initially invisible items.
            return mActivity.onPrepareOptionsMenu(menu);
        }
        return mActivity.superOnPrepareOptionsPanel(view, menu);
    }

    abstract boolean onCreatePanelMenu(int featureId, Menu menu);

    abstract boolean onBackPressed();

    abstract ActionMode startSupportActionMode(ActionMode.Callback callback);

    abstract void setSupportProgressBarVisibility(boolean visible);

    abstract void setSupportProgressBarIndeterminateVisibility(boolean visible);

    abstract void setSupportProgressBarIndeterminate(boolean indeterminate);

    abstract void setSupportProgress(int progress);

    boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    abstract boolean onKeyShortcut(int keyCode, KeyEvent event);

    final ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    final android.support.v7.app.ActionBarDrawerToggle.Delegate getV7DrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    abstract int getHomeAsUpIndicatorAttrId();

    abstract void onContentChanged();

    final String getUiOptionsFromMetadata() {
        try {
            PackageManager pm = mActivity.getPackageManager();
            ActivityInfo info = pm.getActivityInfo(mActivity.getComponentName(),
                    PackageManager.GET_META_DATA);

            String uiOptions = null;
            if (info.metaData != null) {
                uiOptions = info.metaData.getString(METADATA_UI_OPTIONS);
            }
            return uiOptions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getUiOptionsFromMetadata: Activity '" + mActivity.getClass()
                    .getSimpleName() + "' not in manifest");
            return null;
        }
    }

    protected final Context getActionBarThemedContext() {
        Context context = null;

        // If we have an action bar, let it return a themed context
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            context = ab.getThemedContext();
        }

        if (context == null) {
            context = mActivity;
        }
        return context;
    }

    abstract View createView(String name, @NonNull AttributeSet attrs);


    private class ActionBarDrawableToggleImpl implements
            android.support.v7.app.ActionBarDrawerToggle.Delegate,
            ActionBarDrawerToggle.Delegate {
        @Override
        public Drawable getThemeUpIndicator() {
            final TypedArray a = ActionBarActivityDelegate.this.getActionBarThemedContext()
                    .obtainStyledAttributes(new int[]{ getHomeAsUpIndicatorAttrId() });
            final Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        @Override
        public Context getActionBarThemedContext() {
            return ActionBarActivityDelegate.this.getActionBarThemedContext();
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setHomeAsUpIndicator(upDrawable);
                ab.setHomeActionContentDescription(contentDescRes);
            }
        }

        @Override
        public void setActionBarDescription(int contentDescRes) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setHomeActionContentDescription(contentDescRes);
            }
        }
    }

    abstract ActionMode startSupportActionModeFromWindow(ActionMode.Callback callback);

    final void setWindowCallback(WindowCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback can not be null");
        }
        mWindowCallback = callback;
    }

    final WindowCallback getWindowCallback() {
        return mWindowCallback;
    }

    final void destroy() {
        mIsDestroyed = true;
    }

    final boolean isDestroyed() {
        return mIsDestroyed;
    }
}
