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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.appcompat.R;
import android.support.appcompat.view.SupportMenuInflater;
import android.support.appcompat.view.menu.MenuView;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuInflater;
import android.support.v4.view.MenuItem;
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
        void onCreate(Bundle savedInstanceState);
        void onPostCreate(Bundle savedInstanceState);
        void onConfigurationChanged(Configuration newConfig);
        void setContentView(View v);
        void setContentView(int resId);
        void setContentView(View v, ViewGroup.LayoutParams lp);
        void addContentView(View v, ViewGroup.LayoutParams lp);
        ActionBar createActionBar();
        void requestWindowFeature(int feature);
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

     abstract static class ActionBarActivityImplBase implements ActionBarActivityImpl {
         final ActionBarActivity mActivity;
         private ActionBar mActionBar;

         ActionBarActivityImplBase(ActionBarActivity activity) {
             mActivity = activity;
         }

         @Override
         public ActionBar getSupportActionBar() {
             // The Action Bar should be lazily loaded as mHasActionBar or mOverlayActionBar could
             // change after onCreate
             if (mActivity.mHasActionBar || mActivity.mOverlayActionBar) {
                 if (mActionBar == null) {
                     mActionBar = createActionBar();
                 }
             } else {
                 // If we're not set to have a Action Bar, null it just in case it's been set
                 mActionBar = null;
             }

             return mActionBar;
         }
    }

    static class ActionBarActivityImplCompat extends ActionBarActivityImplBase implements MenuPresenter.Callback {
        ActionBarView mActionBarView;
        ListMenuPresenter mListMenuPresenter;
        MenuBuilder mMenu;

        ActionBarActivityImplCompat(ActionBarActivity activity) {
            super(activity);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
        }

        @Override
        public void onPostCreate(Bundle savedInstanceState) {
            // After the Activity has been created and the content views added, we need to make sure
            // that we've inflated the app's menu, so that Action Items can be rendered.
            if (mActivity.mSubDecorInstalled) {
                supportInvalidateOptionsMenu();
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

        void ensureSubDecor() {
            if (mActivity.mHasActionBar && !mActivity.mSubDecorInstalled) {
                if (mActivity.mOverlayActionBar) {
                    mActivity.superSetContentView(R.layout.action_bar_decor_overlay);
                } else {
                    mActivity.superSetContentView(R.layout.action_bar_decor);
                }
                mActionBarView = (ActionBarView) mActivity.findViewById(R.id.action_bar);
                mActivity.mSubDecorInstalled = true;
            }
        }

        @Override
        public void setContentView(View v) {
            ensureSubDecor();
            if (mActivity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) mActivity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v);
            } else {
                mActivity.superSetContentView(v);
            }
        }

        @Override
        public void setContentView(int resId) {
            ensureSubDecor();
            if (mActivity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) mActivity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                final LayoutInflater inflater = mActivity.getLayoutInflater();
                inflater.inflate(resId, contentParent);
            } else {
                mActivity.superSetContentView(resId);
            }
        }

        @Override
        public void setContentView(View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor();
            if (mActivity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) mActivity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v, lp);
            } else {
                mActivity.superSetContentView(v, lp);
            }
        }

        @Override
        public void addContentView(View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor();
            if (mActivity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) mActivity.findViewById(R.id.action_bar_activity_content);
                contentParent.addView(v, lp);
            } else {
                mActivity.superSetContentView(v, lp);
            }
        }

        @Override
        public ActionBar createActionBar() {
            return new ActionBarImplCompat(mActivity, mActivity);
        }

        @Override
        public void requestWindowFeature(int feature) {
            if (feature == FEATURE_ACTION_BAR) {
                mActivity.mHasActionBar = true;
            } else if (feature == FEATURE_ACTION_BAR_OVERLAY) {
                mActivity.mOverlayActionBar = true;
            }
        }

        @Override
        public View onCreatePanelView(int featureId) {
            View createdPanelView = null;

            if (featureId == Window.FEATURE_OPTIONS_PANEL) {
                boolean show = true;
                MenuBuilder menu = mMenu;

                if (menu == null) {
                    // We don't have a menu created, so create one
                    menu = createMenu();
                    setMenu(menu);

                    // Make sure we're not dispatching item changes to presenters
                    menu.stopDispatchingItemsChanged();
                    // Dispatch onCreateSupportOptionsMenu
                    show = dispatchCreateSupportOptionsMenu(menu);
                }

                if (show) {
                    // Make sure we're not dispatching item changes to presenters
                    menu.stopDispatchingItemsChanged();
                    // Dispatch onPrepareSupportOptionsMenu
                    show = dispatchPrepareSupportOptionsMenu(menu);
                }

                if (show) {
                    createdPanelView = (View) getListMenuView(mActivity, this);

                    // Allow menu to start dispatching changes to presenters
                    menu.startDispatchingItemsChanged();
                } else {
                    // If the menu isn't being shown, we no longer need it
                    setMenu(null);
                }
            }

            return createdPanelView;
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

        private MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
            if (mMenu == null) {
                return null;
            }

            if (mListMenuPresenter == null) {
                TypedArray a = context.obtainStyledAttributes(R.styleable.Theme);
                final int listPresenterTheme = a.getResourceId(
                        R.styleable.Theme_panelMenuListTheme,
                        R.style.Theme_AppCompat_CompactMenu);
                a.recycle();

                mListMenuPresenter = new ListMenuPresenter(
                        R.layout.list_menu_item_layout, listPresenterTheme);
                mListMenuPresenter.setCallback(cb);
                mMenu.addMenuPresenter(mListMenuPresenter);
            }

            return mListMenuPresenter.getMenuView(null);
        }

        private void setMenu(MenuBuilder menu) {
            if (menu == mMenu) {
                return;
            }

            if (mMenu != null) {
                mMenu.removeMenuPresenter(mListMenuPresenter);
            }
            mMenu = menu;

            if (menu != null && mListMenuPresenter != null) {
                menu.addMenuPresenter(mListMenuPresenter);
            }
            if (mActionBarView != null) {
                mActionBarView.setMenu(menu, this);
            }
        }

        private boolean dispatchCreateSupportOptionsMenu(MenuBuilder menu) {
            // Allow activity to inflate menu contents
            boolean show = mActivity.onCreateSupportOptionsMenu(menu);
            show |= mActivity.mFragments.dispatchCreateSupportOptionsMenu(menu,
                    mActivity.getCompatMenuInflater());

            return show;
        }

        private boolean dispatchPrepareSupportOptionsMenu(MenuBuilder menu) {
            boolean goforit = mActivity.onPrepareSupportOptionsMenu(menu);
            goforit |= mActivity.mFragments.dispatchPrepareSupportOptionsMenu(menu);
            return goforit;
        }

        /* Ported code from com.android.internal.policy.impl.PhoneWindow */

        /**
         * Initializes the menu associated with the ActionBar.
         */
        private MenuBuilder createMenu() {
            Context context = mActivity;

            // If we have an action bar, initialize the menu with a context themed from it.
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                context = ab.getThemedContext();
            }

            MenuBuilder menu = new MenuBuilder(context);
            menu.setCallback(new MenuBuilder.Callback() {
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

            return menu;
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

                    ActionBar ab = getSupportActionBar();
                    if (item.getItemId() == R.id.home && ab != null &&
                            (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
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
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(title);
            }
        }

        public void supportInvalidateOptionsMenu() {
            final MenuBuilder menu = createMenu();

            // No need to use start/stopDispatchingItemsChanged here
            // as there are no presenters attached yet

            if (dispatchCreateSupportOptionsMenu(menu) &&
                    dispatchPrepareSupportOptionsMenu(menu)) {
                setMenu(menu);
            } else {
                setMenu(null);
            }
        }
    }

    static class ActionBarActivityImplHC extends ActionBarActivityImplBase {
        Menu mMenu;

        ActionBarActivityImplHC(ActionBarActivity activity) {
            super(activity);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if (mActivity.mHasActionBar) {
                // If action bar is requested by inheriting from the appcompat theme,
                // the system will not know about that. So explicitly request for an action bar.
                mActivity.superRequestWindowFeature(FEATURE_ACTION_BAR);
            }
            if (mActivity.mOverlayActionBar) {
                mActivity.superRequestWindowFeature(FEATURE_ACTION_BAR_OVERLAY);
            }
        }

        @Override
        public void onPostCreate(Bundle savedInstanceState) {
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
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
        public ActionBar createActionBar() {
            return new ActionBarImplHC(mActivity, mActivity);
        }

        @Override
        public void requestWindowFeature(int feature) {
            mActivity.superRequestWindowFeature(feature);
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

        ActionBarActivityImplICS(ActionBarActivity activity) {
            super(activity);
        }

        @Override
        public ActionBar createActionBar() {
            return new ActionBarImplICS(mActivity, mActivity);
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

        final int version = Build.VERSION.SDK_INT;
        if (version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mImpl = new ActionBarActivityImplICS(this);
        } else if (version >= Build.VERSION_CODES.HONEYCOMB) {
            mImpl = new ActionBarActivityImplHC(this);
        } else {
            mImpl = new ActionBarActivityImplCompat(this);
        }

        TypedArray a = obtainStyledAttributes(R.styleable.ActionBarWindow);
        mHasActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBar, false);
        mOverlayActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBarOverlay, false);
        a.recycle();

        mImpl.onCreate(savedInstanceState);
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
        mImpl.setContentView(layoutResID);
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
        mImpl.setContentView(view);
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
        mImpl.setContentView(view, params);
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
