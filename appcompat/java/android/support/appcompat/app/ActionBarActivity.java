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

package android.support.appcompat.app;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.appcompat.R;
import android.support.appcompat.view.Menu;
import android.support.appcompat.view.MenuInflater;
import android.support.appcompat.view.menu.ExpandedMenuView;
import android.support.appcompat.view.menu.ListMenuPresenter;
import android.support.appcompat.view.menu.MenuBuilder;
import android.support.appcompat.view.menu.MenuPresenter;
import android.support.appcompat.view.menu.MenuView;
import android.support.appcompat.widget.ActionBarView;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class ActionBarActivity extends FragmentActivity implements ActionBar.Callback {

    private static final int FEATURE_ACTION_BAR = 8;
    private static final int FEATURE_ACTION_BAR_OVERLAY = 9;

    private MenuInflater mMenuInflater;

    interface ActionBarActivityImpl {
        void onCreate(ActionBarActivity activity, Bundle savedInstanceState);
        void onConfigurationChanged(Configuration newConfig);
        void setContentView(ActionBarActivity activity, View v);
        void setContentView(ActionBarActivity activity, int resId);
        void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        ActionBar createActionBar(ActionBarActivity activity);
        void requestWindowFeature(ActionBarActivity activity, int feature);
        ActionBar getSupportActionBar();

        // Methods used to create options menu
        View onCreatePanelView(int featureId);
        boolean onCreatePanelMenu(int featureId, Menu menu);
    }

    static class ActionBarActivityImplBase implements ActionBarActivityImpl, MenuPresenter.Callback {
        private ActionBarActivity mActivity;
        private ActionBar mActionBar;
        private ListMenuPresenter mListMenuPresenter;
        private ExpandedMenuView mMenuPanel;
        private MenuBuilder mMenu;

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
            mActivity = activity;
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
                // Before inflating an ActionBar, we need to make sure that we've inflated
                // the app's menu, so that Action Items can be rendered.
                callOnCreateSupportOptionsMenu();

                if (activity.mOverlayActionBar) {
                    activity.superSetContentView(R.layout.action_bar_decor_overlay);
                } else {
                    activity.superSetContentView(R.layout.action_bar_decor);
                }

                ActionBarView actionBarView = (ActionBarView) activity.findViewById(R.id.action_bar);
                actionBarView.setMenu(mMenu, this);
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
            initActionBar();
            return mActionBar;
        }

        private void initActionBar() {
            if (mActionBar == null && (mActivity.mHasActionBar || mActivity.mOverlayActionBar)) {
                mActionBar = IMPL.createActionBar(mActivity);
            }
        }

        @Override
        public View onCreatePanelView(int featureId) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL) {
                if (mMenu == null) {
                    onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, null);
                }

                // Allow activity to modify menu state (show/hide items, etc.)
                mActivity.onPrepareSupportOptionsMenu(mMenu);

                if (mListMenuPresenter == null) {
                    mListMenuPresenter = new ListMenuPresenter(
                            mActivity, R.layout.list_menu_item_layout);
                    mListMenuPresenter.initForMenu(mActivity, mMenu);
                }

                if (mMenuPanel == null) {
                    mMenuPanel = (ExpandedMenuView) mListMenuPresenter.getMenuView(null);
                }
                return mMenuPanel;
            }

            return null;
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL) {
                return callOnCreateSupportOptionsMenu();
            }
            return false;
        }

        private boolean callOnCreateSupportOptionsMenu() {
            // Discard menu object from system, since it doesn't support action items.
            // Construct new replacement menu and pass it forward instead.
            if (mMenu == null) {
                mMenu = new MenuBuilder(mActivity);
            }

            // Allow activity to inflate menu contents
            return mActivity.onCreateSupportOptionsMenu(mMenu);
        }

        /* MenuPresenter Callbacks */
        /**
         * Called when a menu is closing.
         */
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            return;
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

    }

    static class ActionBarActivityImplHC implements ActionBarActivityImpl {
        private ActionBarImplHC mActionBar;

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
            if (activity.mHasActionBar) {
                // If action bar is requested by inheriting from the appcompat theme,
                // the system will not know about that. So explicitly request for an action bar.
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR);
            }
            if (activity.mOverlayActionBar) {
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR_OVERLAY);
            }
            if (activity.mHasActionBar || activity.mOverlayActionBar) {
                mActionBar = new ActionBarImplHC(activity, activity);
            }
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
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            // Do not create custom options menu on HC+
            return false;
        }
    }

    static class ActionBarActivityImplICS extends ActionBarActivityImplHC {

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplICS(activity, activity);
        }
    }

    static final ActionBarActivityImpl IMPL;

    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 14) {
            IMPL = new ActionBarActivityImplICS();
        } else if (version >= 11) {
            IMPL = new ActionBarActivityImplHC();
        } else {
            IMPL = new ActionBarActivityImplBase();
        }
    }

    // true if the compatibility implementation has installed a window sub-decor layout.
    boolean mSubDecorInstalled;

    // true if this activity has an action bar.
    boolean mHasActionBar;

    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TypedArray a = obtainStyledAttributes(R.styleable.ActionBarWindow);
        mHasActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBar, false);
        mOverlayActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBarOverlay,
                false);
        a.recycle();

        IMPL.onCreate(this, savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return IMPL.getSupportActionBar();
    }

    public MenuInflater getSupportMenuInflater() {
        if (mMenuInflater != null) {
            return mMenuInflater;
        } else {
            mMenuInflater = new MenuInflater(this);
            return mMenuInflater;
        }
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
        IMPL.setContentView(this, layoutResID);
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
        IMPL.setContentView(this, view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        IMPL.onConfigurationChanged(newConfig);
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
        IMPL.setContentView(this, view, params);
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

    @Override
    public View onCreatePanelView(int featureId) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
            return IMPL.onCreatePanelView(featureId);
        else
            return super.onCreatePanelView(featureId);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
            // This is a boundary where we transition from framework Menu objects to support library
            // Menu objects.
            return IMPL.onCreatePanelMenu(featureId, null);
        else
            return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
            return onPrepareOptionsMenu(menu);
        return super.onPreparePanel(featureId, view, menu);
    }

    /**
     * Support library version of onPrepareOptionsMenu, used for legacy
     * devices (API Level 10 or earlier).
     *
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     *
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareOptionsMenu
     * @see #onCreateSupportOptionsMenu
     */
    public boolean onPrepareSupportOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Support library version of onCreateOptionsMenu, used for legacy
     * devices (API Level 10 or earlier).
     *
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onCreateOptionsMenu
     * @see #onPrepareSupportOptionsMenu
     * @see #onOptionsItemSelected
     */
    public boolean onCreateSupportOptionsMenu(android.support.appcompat.view.Menu menu) {
        return false;
    }

}
