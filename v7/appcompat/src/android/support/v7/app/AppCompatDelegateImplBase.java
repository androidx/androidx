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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.appcompat.R;
import android.support.v7.internal.view.SupportMenuInflater;
import android.support.v7.internal.view.WindowCallbackWrapper;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.widget.TintTypedArray;
import android.support.v7.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;

abstract class AppCompatDelegateImplBase extends AppCompatDelegate {

    final Context mContext;
    final Window mWindow;
    final Window.Callback mOriginalWindowCallback;
    final Window.Callback mAppCompatWindowCallback;
    final AppCompatCallback mAppCompatCallback;

    ActionBar mActionBar;
    MenuInflater mMenuInflater;

    // true if this activity has an action bar.
    boolean mHasActionBar;
    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;
    // true if this any action modes should overlay the activity content
    boolean mOverlayActionMode;
    // true if this activity is floating (e.g. Dialog)
    boolean mIsFloating;
    // true if this activity has no title
    boolean mWindowNoTitle;

    private CharSequence mTitle;

    private boolean mIsDestroyed;

    AppCompatDelegateImplBase(Context context, Window window, AppCompatCallback callback) {
        mContext = context;
        mWindow = window;
        mAppCompatCallback = callback;

        mOriginalWindowCallback = mWindow.getCallback();
        if (mOriginalWindowCallback instanceof AppCompatWindowCallbackBase) {
            throw new IllegalStateException(
                    "AppCompat has already installed itself into the Window");
        }
        mAppCompatWindowCallback = wrapWindowCallback(mOriginalWindowCallback);
        // Now install the new callback
        mWindow.setCallback(mAppCompatWindowCallback);
    }

    abstract void initWindowDecorActionBar();

    Window.Callback wrapWindowCallback(Window.Callback callback) {
        return new AppCompatWindowCallbackBase(callback);
    }

    @Override
    public ActionBar getSupportActionBar() {
        // The Action Bar should be lazily created as hasActionBar
        // could change after onCreate
        initWindowDecorActionBar();
        return mActionBar;
    }

    final ActionBar peekSupportActionBar() {
        return mActionBar;
    }

    @Override
    public MenuInflater getMenuInflater() {
        // Make sure that action views can get an appropriate theme.
        if (mMenuInflater == null) {
            initWindowDecorActionBar();
            mMenuInflater = new SupportMenuInflater(
                    mActionBar != null ? mActionBar.getThemedContext() : mContext);
        }
        return mMenuInflater;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.Theme);

        if (!a.hasValue(R.styleable.Theme_windowActionBar)) {
            a.recycle();
            throw new IllegalStateException(
                    "You need to use a Theme.AppCompat theme (or descendant) with this activity.");
        }

        if (a.getBoolean(R.styleable.Theme_windowNoTitle, false)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.Theme_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR);
        }
        if (a.getBoolean(R.styleable.Theme_windowActionBarOverlay, false)) {
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR_OVERLAY);
        }
        if (a.getBoolean(R.styleable.Theme_windowActionModeOverlay, false)) {
            requestWindowFeature(FEATURE_ACTION_MODE_OVERLAY);
        }
        mIsFloating = a.getBoolean(R.styleable.Theme_android_windowIsFloating, false);
        a.recycle();
    }

    // Methods used to create and respond to options menu
    abstract void onPanelClosed(int featureId, Menu menu);

    abstract boolean onMenuOpened(int featureId, Menu menu);

    abstract boolean dispatchKeyEvent(KeyEvent event);

    abstract boolean onKeyShortcut(int keyCode, KeyEvent event);

    @Override
    public final ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    final Context getActionBarThemedContext() {
        Context context = null;

        // If we have an action bar, let it return a themed context
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            context = ab.getThemedContext();
        }

        if (context == null) {
            context = mContext;
        }
        return context;
    }

    private class ActionBarDrawableToggleImpl implements ActionBarDrawerToggle.Delegate {
        @Override
        public Drawable getThemeUpIndicator() {
            final TintTypedArray a = TintTypedArray.obtainStyledAttributes(
                    getActionBarThemedContext(), null, new int[]{ R.attr.homeAsUpIndicator });
            final Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        @Override
        public Context getActionBarThemedContext() {
            return AppCompatDelegateImplBase.this.getActionBarThemedContext();
        }

        @Override
        public boolean isNavigationVisible() {
            final ActionBar ab = getSupportActionBar();
            return ab != null && (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0;
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

    @Override
    public final void onDestroy() {
        mIsDestroyed = true;
    }

    @Override
    public void setHandleNativeActionModesEnabled(boolean enabled) {
        // no-op pre-v14
    }

    @Override
    public boolean isHandleNativeActionModesEnabled() {
        // Always false pre-v14
        return false;
    }

    final boolean isDestroyed() {
        return mIsDestroyed;
    }

    final Window.Callback getWindowCallback() {
        return mWindow.getCallback();
    }

    @Override
    public final void setTitle(CharSequence title) {
        mTitle = title;
        onTitleChanged(title);
    }

    abstract void onTitleChanged(CharSequence title);

    final CharSequence getTitle() {
        // If the original window callback is an Activity, we'll use it's title
        if (mOriginalWindowCallback instanceof Activity) {
            return ((Activity) mOriginalWindowCallback).getTitle();
        }
        // Else, we'll return the title we have recorded ourselves
        return mTitle;
    }

    class AppCompatWindowCallbackBase extends WindowCallbackWrapper {
        AppCompatWindowCallbackBase(Window.Callback callback) {
            super(callback);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return AppCompatDelegateImplBase.this.dispatchKeyEvent(event)
                    || super.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return super.dispatchKeyShortcutEvent(event)
                    || AppCompatDelegateImplBase.this.onKeyShortcut(event.getKeyCode(), event);
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
            return super.onMenuOpened(featureId, menu)
                    || AppCompatDelegateImplBase.this.onMenuOpened(featureId, menu);
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            super.onPanelClosed(featureId, menu);
            AppCompatDelegateImplBase.this.onPanelClosed(featureId, menu);
        }
    }
}
