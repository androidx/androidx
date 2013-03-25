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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.appcompat.R;
import android.support.v7.internal.view.menu.ListMenuPresenter;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPresenter;
import android.support.v7.internal.view.menu.MenuView;
import android.support.v7.internal.widget.ActionBarContainer;
import android.support.v7.internal.widget.ActionBarContextView;
import android.support.v7.internal.widget.ActionBarView;
import android.support.v7.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

class ActionBarActivityDelegateCompat extends ActionBarActivityDelegate implements
        MenuPresenter.Callback, MenuBuilder.Callback {

    ActionBarView mActionBarView;
    ListMenuPresenter mListMenuPresenter;
    MenuBuilder mMenu;

    ActionBarActivityDelegateCompat(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    public ActionBar createSupportActionBar() {
        return new ActionBarImplCompat(mActivity, mActivity);
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

    private void ensureSubDecor() {
        if (mActivity.mHasActionBar && !mActivity.mSubDecorInstalled) {
            if (mActivity.mOverlayActionBar) {
                mActivity.superSetContentView(R.layout.action_bar_decor_overlay);
            } else {
                mActivity.superSetContentView(R.layout.action_bar_decor);
            }
            mActionBarView = (ActionBarView) mActivity.findViewById(R.id.action_bar);

            /**
             * Split Action Bar
             */
            boolean splitWhenNarrow = UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW
                    .equals(getUiOptionsFromMetadata());
            boolean splitActionBar;

            if (splitWhenNarrow) {
                splitActionBar = mActivity.getResources()
                        .getBoolean(R.bool.split_action_bar_is_narrow);
            } else {
                TypedArray a = mActivity.obtainStyledAttributes(R.styleable.ActionBarWindow);
                splitActionBar = a
                        .getBoolean(R.styleable.ActionBarWindow_windowSplitActionBar, false);
                a.recycle();
            }

            final ActionBarContainer splitView = (ActionBarContainer) mActivity.findViewById(
                    R.id.split_action_bar);
            if (splitView != null) {
                mActionBarView.setSplitView(splitView);
                mActionBarView.setSplitActionBar(splitActionBar);
                mActionBarView.setSplitWhenNarrow(splitWhenNarrow);

                final ActionBarContextView cab = (ActionBarContextView) mActivity.findViewById(
                        R.id.action_context_bar);
                cab.setSplitView(splitView);
                cab.setSplitActionBar(splitActionBar);
                cab.setSplitWhenNarrow(splitWhenNarrow);
            }

            mActivity.mSubDecorInstalled = true;
        }
    }

    @Override
    public boolean requestWindowFeature(int featureId) {
        switch (featureId) {
            case WindowCompat.FEATURE_ACTION_BAR:
                mActivity.mHasActionBar = true;
                return true;
            case WindowCompat.FEATURE_ACTION_BAR_OVERLAY:
                mActivity.mOverlayActionBar = true;
                return true;
            default:
                return mActivity.requestWindowFeature(featureId);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        ActionBar ab = mActivity.getSupportActionBar();
        if (ab != null) {
            ab.setTitle(title);
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

    @Override
    public boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        if (Window.FEATURE_OPTIONS_PANEL != featureId) {
            return mActivity.superOnCreatePanelMenu(featureId, frameworkMenu);
        }

        // Should never get here as FEATURE_OPTIONS_PANEL is handled by onCreatePanelView
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
        if (Window.FEATURE_OPTIONS_PANEL != featureId) {
            return mActivity.superOnPreparePanelMenu(featureId, view, menu);
        }

        // Should never get here as FEATURE_OPTIONS_PANEL is handled by onCreatePanelView
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem) {
        // We don't want to handle framework items here
        return false;
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        if (mActivity.onSupportOptionsItemSelected(item)) {
            return true;
        }
        // FIXME: Reintroduce support options menu dispatch through facade.
        //if (mActivity.mFragments.dispatchSupportOptionsItemSelected(item)) {
        //    return true;
        //}

        ActionBar ab = mActivity.getSupportActionBar();
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
    }

    @Override
    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(menu, true);
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        mActivity.closeOptionsMenu();
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        return false;
    }

    @Override
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

    private MenuBuilder createMenu() {
        Context context = mActivity;

        // If we have an action bar, initialize the menu with a context themed from it.
        ActionBar ab = mActivity.getSupportActionBar();
        if (ab != null) {
            context = ab.getThemedContext();
        }

        MenuBuilder menu = new MenuBuilder(context);
        menu.setCallback(this);
        return menu;
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
        } else {
            // Make sure we update the ListView
            mListMenuPresenter.updateMenuView(false);
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

        // FIXME: Reintroduce support options menu dispatch through facade.
        //show |= mActivity.mFragments.dispatchCreateSupportOptionsMenu(menu,
        //        mActivity.getCompatMenuInflater());

        return show;
    }

    private boolean dispatchPrepareSupportOptionsMenu(MenuBuilder menu) {
        boolean goforit = mActivity.onPrepareSupportOptionsMenu(menu);
        // FIXME: Reintroduce support options menu dispatch through facade.
        //goforit |= mActivity.mFragments.dispatchPrepareSupportOptionsMenu(menu);
        return goforit;
    }

}
