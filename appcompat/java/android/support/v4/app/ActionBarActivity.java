/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.app;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.appcompat.R;
import android.support.appcompat.view.SupportMenuInflater;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuInflater;
import android.support.v4.view.MenuItem;
import android.support.appcompat.view.menu.ExpandedMenuView;
import android.support.appcompat.view.menu.ListMenuPresenter;
import android.support.appcompat.view.menu.MenuBuilder;
import android.support.appcompat.view.menu.MenuPresenter;
import android.support.appcompat.view.menu.MenuWrapper;
import android.support.appcompat.widget.ActionBarView;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class ActionBarActivity extends FragmentActivity implements ActionBar.Callback {

    private static final int FEATURE_ACTION_BAR = 8;
    private static final int FEATURE_ACTION_BAR_OVERLAY = 9;

    interface ActionBarActivityImpl {
        void onCreate(ActionBarActivity activity, Bundle savedInstanceState);
        void onPostCreate(Bundle savedInstanceState);
        void onConfigurationChanged(Configuration newConfig);
        void setContentView(ActionBarActivity activity, View v);
        void setContentView(ActionBarActivity activity, int resId);
        void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        ActionBar createActionBar(ActionBarActivity activity);
        void requestWindowFeature(ActionBarActivity activity, int feature);
        ActionBar getSupportActionBar();
        void setTitle(CharSequence title);
        void supportInvalidateOptionsMenu();

        // Methods used to create and respond to options menu
        View onCreatePanelView(int featureId);
        boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu);
        boolean onMenuItemSelected(int featureId, MenuItem item);
        boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem);
        boolean onPreparePanel(int featureId, View view, android.view.Menu frameworkMenu);
    }

    static class ActionBarActivityImplBase implements ActionBarActivityImpl, MenuPresenter.Callback {
        private ActionBarActivity mActivity;
        private ActionBar mActionBar;
        private ActionBarView mActionBarView;
        private ListMenuPresenter mListMenuPresenter;
        private ExpandedMenuView mMenuPanel;
        private MenuBuilder mMenu;

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
            mActivity = activity;
        }

        @Override
        public void onPostCreate(Bundle savedInstanceState) {
            // After the Activity has been created and the content views added, we need to make sure
            // that we've inflated the app's menu, so that Action Items can be rendered.
            if (mActivity.mSubDecorInstalled) {
                if (dispatchCreateSupportOptionsMenu()) {
                    dispatchPrepareSupportOptionsMenu();
                }
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            // If this is called before sub-decor is installed, ActionBar will not
            // be properly initialized.
            if (mActivity.mHasActionBar && mActivity.mSubDecorInstalled) {
                // Note: The action bar will need to access
                // view changes from superclass.
                ActionBarImplCompat actionBar =
                        (ActionBarImplCompat) mActivity.getSupportActionBar();
                actionBar.onConfigurationChanged(newConfig);
            }
        }

        protected void ensureSubDecor(ActionBarActivity activity) {
            if (activity.mHasActionBar && !activity.mSubDecorInstalled) {
                if (activity.mOverlayActionBar) {
                    activity.superSetContentView(R.layout.action_bar_decor_overlay);
                } else {
                    activity.superSetContentView(R.layout.action_bar_decor);
                }
                mActionBarView = (ActionBarView) activity.findViewById(R.id.action_bar);
                mActionBarView.setMenu(mMenu, this);

                initActionBar();

                activity.mSubDecorInstalled = true;
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v);
            } else {
                activity.superSetContentView(v);
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, int resId) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                final LayoutInflater inflater = activity.getLayoutInflater();
                inflater.inflate(resId, contentParent);
            } else {
                activity.superSetContentView(resId);
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v, lp);
            } else {
                activity.superSetContentView(v, lp);
            }
        }

        @Override
        public void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.addView(v, lp);
            } else {
                activity.superSetContentView(v, lp);
            }
        }

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplCompat(activity, activity);
        }

        @Override
        public void requestWindowFeature(ActionBarActivity activity, int feature) {
            if (feature == FEATURE_ACTION_BAR) {
                activity.mHasActionBar = true;
            } else if (feature == FEATURE_ACTION_BAR_OVERLAY) {
                activity.mOverlayActionBar = true;
            }
        }

        @Override
        public ActionBar getSupportActionBar() {
            return mActionBar;
        }

        private void initActionBar() {
            if (mActionBar == null && (mActivity.mHasActionBar || mActivity.mOverlayActionBar)) {
                mActionBar = createActionBar(mActivity);
            }
        }

        @Override
        public View onCreatePanelView(int featureId) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL) {
                boolean show = true;

                // Only dispatch onCreateSupportOptionsMenu if we haven't already
                if (mMenu == null) {
                    show = dispatchCreateSupportOptionsMenu();
                }

                if (show) {
                    show = dispatchPrepareSupportOptionsMenu();

                    if (show) {
                        if (mListMenuPresenter == null) {
                            TypedArray a = mActivity.obtainStyledAttributes(R.styleable.Theme);
                            final int listPresenterTheme = a.getResourceId(
                                    R.styleable.Theme_panelMenuListTheme,
                                    R.style.Theme_AppCompat_CompactMenu);
                            a.recycle();

                            mListMenuPresenter = new ListMenuPresenter(
                                    R.layout.list_menu_item_layout, listPresenterTheme);
                            mListMenuPresenter.initForMenu(mActivity, mMenu);
                        }

                        if (mMenuPanel == null) {
                            mMenuPanel = (ExpandedMenuView) mListMenuPresenter.getMenuView(null);
                        }
                        return mMenuPanel;
                    }
                }
            }

            return null;
        }

        public boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
            if (Window.FEATURE_OPTIONS_PANEL != featureId) {
                return mActivity.superOnCreatePanelMenu(featureId, frameworkMenu);
            }

            // Should never get here as FEATURE_OPTIONS_PANEL is handled by onCreatePanelView
            return false;
        }

        public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
            if (Window.FEATURE_OPTIONS_PANEL != featureId) {
                return mActivity.superOnPreparePanelMenu(featureId, view, menu);
            }

            // Should never get here as FEATURE_OPTIONS_PANEL is handled by onCreatePanelView
            return false;
        }

        private boolean dispatchCreateSupportOptionsMenu() {
            initMenu();

            // Allow activity to inflate menu contents
            boolean show = mActivity.onCreateSupportOptionsMenu(mMenu);
            show |= mActivity.mFragments.dispatchCreateSupportOptionsMenu(mMenu,
                    mActivity.getCompatMenuInflater());
            return show;
        }

        private boolean dispatchPrepareSupportOptionsMenu() {
            boolean goforit = mActivity.onPrepareSupportOptionsMenu(mMenu);
            goforit |= mActivity.mFragments.dispatchPrepareSupportOptionsMenu(mMenu);
            return goforit;
        }

        /* Ported code from com.android.internal.policy.impl.PhoneWindow */

        /**
         * Initializes the menu associated with the ActionBar.
         */
        protected void initMenu() {
            Context context = mActivity;

            // If we have an action bar, initialize the menu with a context themed for it.
            if (mActionBar == null && (mActivity.mHasActionBar || mActivity.mOverlayActionBar)) {
                TypedValue outValue = new TypedValue();
                Resources.Theme currentTheme = mActivity.getTheme();
                currentTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
                final int targetThemeRes = outValue.resourceId;

                if (targetThemeRes != 0) {
                    context = new ContextThemeWrapper(context, targetThemeRes);
                }
            }

            mMenu = new MenuBuilder(context);
            mMenu.setCallback(new MenuBuilder.Callback() {
                /**
                 * Called when a menu item is selected.
                 *
                 * @param menu The menu that is the parent of the item
                 * @param item The menu item that is selected
                 * @return whether the menu item selection was handled
                 */
                @Override
                public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                    return mActivity.onSupportMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
                }

                /**
                 * Called when the mode of the menu changes (for example, from icon to expanded).
                 *
                 * @param menu the menu that has changed modes
                 */
                @Override
                public void onMenuModeChange(MenuBuilder menu) {
                    reopenMenu(menu, true);
                }

                private void reopenMenu(MenuBuilder menu, boolean toggleMenuMode) {
                    if (mActionBarView != null && mActionBarView.isOverflowReserved()) {
                        if (!mActionBarView.isOverflowMenuShowing() || !toggleMenuMode) {
                            if (mActionBarView.getVisibility() == View.VISIBLE) {
                                mActionBarView.showOverflowMenu();
                            }
                        } else {
                            mActionBarView.hideOverflowMenu();
                        }
                        return;
                    }

                    menu.close();
                }
            });

            if (mActionBarView != null) {
                mActionBarView.setMenu(mMenu, this);
            }
            if (mListMenuPresenter != null) {
                mListMenuPresenter.initForMenu(mActivity, mMenu);
            }
        }

        @Override
        public boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem) {
            // We don't want to handle framework items here
            return false;
        }

        /**
         * Default implementation of
         * {@link android.view.Window.Callback#onMenuItemSelected}
         * for activities.  This calls through to the new
         * {@link #onSupportOptionsItemSelected} method for the
         * {@link android.view.Window#FEATURE_OPTIONS_PANEL}
         * panel, so that subclasses of
         * Activity don't need to deal with feature codes.
         */
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            switch (featureId) {
                case Window.FEATURE_OPTIONS_PANEL:
                    if (mActivity.onSupportOptionsItemSelected(item)) {
                        return true;
                    }
                    if (mActivity.mFragments.dispatchSupportOptionsItemSelected(item)) {
                        return true;
                    }
                    if (item.getItemId() == R.id.home && mActionBar != null &&
                            (mActionBar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
                        if (mActivity.getParent() == null) {
                            // TODO: Implement "Up" button
                            // return mActivity.onNavigateUp();
                        } else {
                            // TODO: Implement "Up" button
                            // return mParent.onNavigateUpFromChild(this);
                        }
                    }
                    return false;

                default:
                    Log.e("ActionBarActivity", "Compatibility onSupportMenuItemSelected() invoked with" +
                            "invalid data. Only FEATURE_OPTIONS_PANEL is supported.");
                    return false;
            }
        }

        /* MenuPresenter Callbacks */
        /**
         * Called when a menu is closing.
         */
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            mActivity.closeOptionsMenu();
        }

        /**
         * Called when a submenu opens. Useful for notifying the application of menu state so that
         * it does not attempt to hide the action bar while a submenu is open or similar.
         *
         * @param subMenu Submenu currently being opened
         * @return true if the Callback will handle presenting the submenu, false if the presenter
         *         should attempt to do so.
         */
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            return false;
        }

        @Override
        public void setTitle(CharSequence title) {
            if (mActionBar != null) {
                mActionBar.setTitle(title);
            }
        }

        public void supportInvalidateOptionsMenu() {
            if (dispatchCreateSupportOptionsMenu()) {
                dispatchPrepareSupportOptionsMenu();
            }
        }
    }

    static class ActionBarActivityImplHC implements ActionBarActivityImpl {
        private ActionBar mActionBar;
        private ActionBarActivity mActivity;
        private Menu mMenu;

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
            mActivity = activity;

            if (activity.mHasActionBar) {
                // If action bar is requested by inheriting from the appcompat theme,
                // the system will not know about that. So explicitly request for an action bar.
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR);
            }
            if (activity.mOverlayActionBar) {
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR_OVERLAY);
            }
            if (activity.mHasActionBar || activity.mOverlayActionBar) {
                mActionBar = createActionBar(mActivity);
            }
        }

        @Override
        public void onPostCreate(Bundle savedInstanceState) {
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v) {
            activity.superSetContentView(v);
        }

        @Override
        public void setContentView(ActionBarActivity activity, int resId) {
            activity.superSetContentView(resId);
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.superSetContentView(v, lp);
        }

        @Override
        public void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.superAddContentView(v, lp);
        }

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplHC(activity, activity);
        }

        @Override
        public void requestWindowFeature(ActionBarActivity activity, int feature) {
            activity.superRequestWindowFeature(feature);
        }

        @Override
        public ActionBar getSupportActionBar() {
            return mActionBar;
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
                show |= mActivity.mFragments.dispatchCreateSupportOptionsMenu(mMenu,
                        mActivity.getCompatMenuInflater());
                return show;
            } else {
                return mActivity.superOnCreatePanelMenu(featureId, frameworkMenu);
            }
        }

        @Override
        public boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem) {
            switch (featureId) {
                case Window.FEATURE_OPTIONS_PANEL:
                    MenuItem wrappedItem = MenuWrapper.createMenuItemWrapper(frameworkItem);
                    if (mActivity.onSupportOptionsItemSelected(wrappedItem)) {
                        return true;
                    }
                    if (mActivity.mFragments.dispatchSupportOptionsItemSelected(wrappedItem)) {
                        return true;
                    }
                    break;
                default:
                    Log.e("ActionBarActivity", "Compatibility onSupportMenuItemSelected() invoked with" +
                            "invalid data. Only FEATURE_OPTIONS_PANEL is supported.");
                    break;
            }

            return false;
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return false;
        }

        public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL && mMenu != null) {
                boolean goforit = mActivity.onPrepareSupportOptionsMenu(mMenu);
                goforit |= mActivity.mFragments.dispatchPrepareSupportOptionsMenu(mMenu);
                return goforit;
            } else {
                return mActivity.superOnPreparePanelMenu(featureId, view, menu);
            }
        }

        @Override
        public void setTitle(CharSequence title) {
            // Handled by framework
        }

        public void supportInvalidateOptionsMenu() {
        }

    }

    static class ActionBarActivityImplICS extends ActionBarActivityImplHC {

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplICS(activity, activity);
        }

    }

    ActionBarActivityImpl mImpl;

    // true if the compatibility implementation has installed a window sub-decor layout.
    boolean mSubDecorInstalled;

    // true if this activity has an action bar.
    boolean mHasActionBar;

    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 14) {
            mImpl = new ActionBarActivityImplICS();
        } else if (version >= 11) {
            mImpl = new ActionBarActivityImplHC();
        } else {
            mImpl = new ActionBarActivityImplBase();
        }

        TypedArray a = obtainStyledAttributes(R.styleable.ActionBarWindow);
        mHasActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBar, false);
        mOverlayActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBarOverlay,
                false);
        a.recycle();

        mImpl.onCreate(this, savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return mImpl.getSupportActionBar();
    }

    /**
     * Set the activity content from a layout resource.  The resource will be inflated, adding all
     * top-level views to the activity.
     *
     * @param layoutResID Resource ID to be inflated.
     * @see #setContentView(android.view.View)
     * @see #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    @Override
    public void setContentView(int layoutResID) {
        mImpl.setContentView(this, layoutResID);
    }

    /**
     * Set the activity content to an explicit view.  This view is placed directly into the
     * activity's view hierarchy.  It can itself be a complex view hierarchy.  When calling this
     * method, the layout parameters of the specified view are ignored.  Both the width and the
     * height of the view are set by default to {@link ViewGroup.LayoutParams#MATCH_PARENT}. To use
     * your own layout parameters, invoke {@link #setContentView(android.view.View,
     * android.view.ViewGroup.LayoutParams)} instead.
     *
     * @param view The desired content to display.
     * @see #setContentView(int)
     * @see #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    @Override
    public void setContentView(View view) {
        mImpl.setContentView(this, view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mImpl.onConfigurationChanged(newConfig);
    }

    /**
     * Set the activity content to an explicit view.  This view is placed directly into the
     * activity's view hierarchy.  It can itself be a complex view hierarchy.
     *
     * @param view   The desired content to display.
     * @param params Layout parameters for the view.
     * @see #setContentView(android.view.View)
     * @see #setContentView(int)
     */
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mImpl.setContentView(this, view, params);
    }

    void superSetContentView(int resId) {
        super.setContentView(resId);
    }

    void superSetContentView(View v) {
        super.setContentView(v);
    }

    void superSetContentView(View v, ViewGroup.LayoutParams lp) {
        super.setContentView(v, lp);
    }

    void superAddContentView(View v, ViewGroup.LayoutParams lp) {
        super.addContentView(v, lp);
    }

    void superRequestWindowFeature(int feature) {
        super.requestWindowFeature(feature);
    }

    boolean superOnCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        return super.onCreatePanelMenu(featureId, frameworkMenu);
    }

    boolean superOnPreparePanelMenu(int featureId, View view, android.view.Menu menu) {
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
            return mImpl.onCreatePanelView(featureId);
        else {
            return super.onCreatePanelView(featureId);
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        return mImpl.onCreatePanelMenu(featureId, frameworkMenu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mImpl.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
        return mImpl.onPreparePanel(featureId, view, menu);
    }

    /**
     * Default implementation of
     * {@link android.view.Window.Callback#onMenuItemSelected}
     * for activities.  This calls through to the new
     * {@link #onSupportOptionsItemSelected} method for the
     * {@link android.view.Window#FEATURE_OPTIONS_PANEL}
     * panel, so that subclasses of
     * Activity don't need to deal with feature codes.
     */
    public boolean onSupportMenuItemSelected(int featureId,
            MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            return mImpl.onMenuItemSelected(featureId, item);
        }
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL
                && mImpl.onMenuItemSelected(featureId, item)) {
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        mImpl.setTitle(title);
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        // Only call up to super on HC+, mImpl will handle otherwise
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            super.supportInvalidateOptionsMenu();
        }
        mImpl.supportInvalidateOptionsMenu();
    }

    @Override
    MenuInflater createCompatMenuInflater() {
        return new SupportMenuInflater(this);
    }

}
