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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.WindowCompat;
import android.support.v7.appcompat.R;
import android.support.v7.internal.view.menu.ListMenuPresenter;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPresenter;
import android.support.v7.internal.view.menu.MenuView;
import android.support.v7.internal.view.menu.MenuWrapperFactory;
import android.support.v7.internal.widget.ActionBarContainer;
import android.support.v7.internal.widget.ActionBarContextView;
import android.support.v7.internal.widget.ActionBarView;
import android.support.v7.internal.widget.ProgressBarICS;
import android.support.v7.view.ActionMode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

class ActionBarActivityDelegateBase extends ActionBarActivityDelegate implements
        MenuPresenter.Callback, MenuBuilder.Callback {
    private static final String TAG = "ActionBarActivityDelegateBase";

    private static final int[] ACTION_BAR_DRAWABLE_TOGGLE_ATTRS = new int[] {
            R.attr.homeAsUpIndicator
    };

    private ActionBarView mActionBarView;
    private ListMenuPresenter mListMenuPresenter;
    private MenuBuilder mMenu;

    private ActionMode mActionMode;

    // true if we have installed a window sub-decor layout.
    private boolean mSubDecorInstalled;

    private CharSequence mTitleToSet;

    // Used to keep track of Progress Bar Window features
    private boolean mFeatureProgress, mFeatureIndeterminateProgress;

    // Used for emulating PanelFeatureState
    private boolean mClosingActionMenu;
    private boolean mPanelIsPrepared;
    private boolean mPanelRefreshContent;
    private Bundle mPanelFrozenActionViewState;

    ActionBarActivityDelegateBase(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        return new ActionBarImplBase(mActivity, mActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // If this is called before sub-decor is installed, ActionBar will not
        // be properly initialized.
        if (mHasActionBar && mSubDecorInstalled) {
            // Note: The action bar will need to access
            // view changes from superclass.
            ActionBarImplBase actionBar = (ActionBarImplBase) getSupportActionBar();
            actionBar.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onStop() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(false);
        }
    }

    @Override
    public void onPostResume() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(true);
        }
    }

    @Override
    public void setContentView(View v) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        mActivity.getLayoutInflater().inflate(resId, contentParent);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v, lp);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.addView(v, lp);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void onContentChanged() {
        // Ignore all calls to this method as we call onSupportContentChanged manually above
    }

    final void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            if (mHasActionBar) {
                if (mOverlayActionBar) {
                    mActivity.superSetContentView(R.layout.abc_action_bar_decor_overlay);
                } else {
                    mActivity.superSetContentView(R.layout.abc_action_bar_decor);
                }
                mActionBarView = (ActionBarView) mActivity.findViewById(R.id.action_bar);
                mActionBarView.setWindowCallback(mActivity);

                /**
                 * Progress Bars
                 */
                if (mFeatureProgress) {
                    mActionBarView.initProgress();
                }
                if (mFeatureIndeterminateProgress) {
                    mActionBarView.initIndeterminateProgress();
                }

                /**
                 * Split Action Bar
                 */
                boolean splitWhenNarrow = UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW
                        .equals(getUiOptionsFromMetadata());
                boolean splitActionBar;

                if (splitWhenNarrow) {
                    splitActionBar = mActivity.getResources()
                            .getBoolean(R.bool.abc_split_action_bar_is_narrow);
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
            } else {
                mActivity.superSetContentView(R.layout.abc_simple_decor);
            }

            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            View content = mActivity.findViewById(android.R.id.content);
            content.setId(View.NO_ID);
            View abcContent = mActivity.findViewById(R.id.action_bar_activity_content);
            abcContent.setId(android.R.id.content);

            // A title was set before we've install the decor so set it now.
            if (mTitleToSet != null) {
                mActionBarView.setWindowTitle(mTitleToSet);
                mTitleToSet = null;
            }

            applyFixedSizeWindow();

            mSubDecorInstalled = true;

            // Post supportInvalidateOptionsMenu() so that the menu is invalidated post-onCreate()
            mActivity.getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    supportInvalidateOptionsMenu();
                }
            });
        }
    }

    private void applyFixedSizeWindow() {
        TypedArray a = mActivity.obtainStyledAttributes(R.styleable.ActionBarWindow);

        TypedValue mFixedWidthMajor = null;
        TypedValue mFixedWidthMinor = null;
        TypedValue mFixedHeightMajor = null;
        TypedValue mFixedHeightMinor = null;

        if (a.hasValue(R.styleable.ActionBarWindow_windowFixedWidthMajor)) {
            if (mFixedWidthMajor == null) mFixedWidthMajor = new TypedValue();
            a.getValue(R.styleable.ActionBarWindow_windowFixedWidthMajor, mFixedWidthMajor);
        }
        if (a.hasValue(R.styleable.ActionBarWindow_windowFixedWidthMinor)) {
            if (mFixedWidthMinor == null) mFixedWidthMinor = new TypedValue();
            a.getValue(R.styleable.ActionBarWindow_windowFixedWidthMinor, mFixedWidthMinor);
        }
        if (a.hasValue(R.styleable.ActionBarWindow_windowFixedHeightMajor)) {
            if (mFixedHeightMajor == null) mFixedHeightMajor = new TypedValue();
            a.getValue(R.styleable.ActionBarWindow_windowFixedHeightMajor, mFixedHeightMajor);
        }
        if (a.hasValue(R.styleable.ActionBarWindow_windowFixedHeightMinor)) {
            if (mFixedHeightMinor == null) mFixedHeightMinor = new TypedValue();
            a.getValue(R.styleable.ActionBarWindow_windowFixedHeightMinor, mFixedHeightMinor);
        }

        final DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
        final boolean isPortrait = metrics.widthPixels < metrics.heightPixels;
        int w = ViewGroup.LayoutParams.MATCH_PARENT;
        int h = ViewGroup.LayoutParams.MATCH_PARENT;

        final TypedValue tvw = isPortrait ? mFixedWidthMinor : mFixedWidthMajor;
        if (tvw != null && tvw.type != TypedValue.TYPE_NULL) {
            if (tvw.type == TypedValue.TYPE_DIMENSION) {
                w = (int) tvw.getDimension(metrics);
            } else if (tvw.type == TypedValue.TYPE_FRACTION) {
                w = (int) tvw.getFraction(metrics.widthPixels, metrics.widthPixels);
            }
        }

        final TypedValue tvh = isPortrait ? mFixedHeightMajor : mFixedHeightMinor;
        if (tvh != null && tvh.type != TypedValue.TYPE_NULL) {
            if (tvh.type == TypedValue.TYPE_DIMENSION) {
                h = (int) tvh.getDimension(metrics);
            } else if (tvh.type == TypedValue.TYPE_FRACTION) {
                h = (int) tvh.getFraction(metrics.heightPixels, metrics.heightPixels);
            }
        }

        if (w != ViewGroup.LayoutParams.MATCH_PARENT || h != ViewGroup.LayoutParams.MATCH_PARENT) {
            mActivity.getWindow().setLayout(w, h);
        }

        a.recycle();
    }

    @Override
    public boolean supportRequestWindowFeature(int featureId) {
        switch (featureId) {
            case WindowCompat.FEATURE_ACTION_BAR:
                mHasActionBar = true;
                return true;
            case WindowCompat.FEATURE_ACTION_BAR_OVERLAY:
                mOverlayActionBar = true;
                return true;
            case Window.FEATURE_PROGRESS:
                mFeatureProgress = true;
                return true;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                mFeatureIndeterminateProgress = true;
                return true;
            default:
                return mActivity.requestWindowFeature(featureId);
        }
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        if (mActionBarView != null) {
            mActionBarView.setWindowTitle(title);
        } else {
            mTitleToSet = title;
        }
    }

    @Override
    public View onCreatePanelView(int featureId) {
        View createdPanelView = null;

        if (featureId == Window.FEATURE_OPTIONS_PANEL && preparePanel()) {
            createdPanelView = (View) getListMenuView(mActivity, this);
        }

        return createdPanelView;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return mActivity.superOnCreatePanelMenu(featureId, menu);
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return mActivity.superOnPreparePanel(featureId, view, menu);
        }
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            item = MenuWrapperFactory.createMenuItemWrapper(item);
        }
        return mActivity.superOnMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
    }

    @Override
    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(menu, true);
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        if (mClosingActionMenu) {
            return;
        }
        mClosingActionMenu = true;
        mActivity.closeOptionsMenu();
        mActionBarView.dismissPopupMenus();
        mClosingActionMenu = false;
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        return false;
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }

        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);

        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            mActionMode = ab.startActionMode(wrappedCallback);
        }

        if (mActionMode != null) {
            mActivity.onSupportActionModeStarted(mActionMode);
        }
        return mActionMode;
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        if (mMenu != null) {
            Bundle savedActionViewStates = new Bundle();
            mMenu.saveActionViewStates(savedActionViewStates);
            if (savedActionViewStates.size() > 0) {
                mPanelFrozenActionViewState = savedActionViewStates;
            }
            // This will be started again when the panel is prepared.
            mMenu.stopDispatchingItemsChanged();
            mMenu.clear();
        }
        mPanelRefreshContent = true;

        // Prepare the options panel if we have an action bar
        if (mActionBarView != null) {
            mPanelIsPrepared = false;
            preparePanel();
        }
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
                    R.layout.abc_list_menu_item_layout, listPresenterTheme);
            mListMenuPresenter.setCallback(cb);
            mMenu.addMenuPresenter(mListMenuPresenter);
        } else {
            // Make sure we update the ListView
            mListMenuPresenter.updateMenuView(false);
        }

        return mListMenuPresenter.getMenuView(new FrameLayout(context));
    }

    @Override
    public boolean onBackPressed() {
        // Back cancels action modes first.
        if (mActionMode != null) {
            mActionMode.finish();
            return true;
        }

        // Next collapse any expanded action views.
        if (mActionBarView != null && mActionBarView.hasExpandedActionView()) {
            mActionBarView.collapseActionView();
            return true;
        }

        return false;
    }

    @Override
    void setSupportProgressBarVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminate(boolean indeterminate) {
        updateProgressBars(indeterminate ? Window.PROGRESS_INDETERMINATE_ON
                : Window.PROGRESS_INDETERMINATE_OFF);
    }

    @Override
    void setSupportProgress(int progress) {
        updateProgressBars(Window.PROGRESS_START + progress);
    }

    @Override
    int getHomeAsUpIndicatorAttrId() {
        return R.attr.homeAsUpIndicator;
    }

    /**
     * Progress Bar function. Mostly extracted from PhoneWindow.java
     */
    private void updateProgressBars(int value) {
        ProgressBarICS circularProgressBar = getCircularProgressBar();
        ProgressBarICS horizontalProgressBar = getHorizontalProgressBar();

        if (value == Window.PROGRESS_VISIBILITY_ON) {
            if (mFeatureProgress) {
                int level = horizontalProgressBar.getProgress();
                int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ?
                        View.VISIBLE : View.INVISIBLE;
                horizontalProgressBar.setVisibility(visibility);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (value == Window.PROGRESS_VISIBILITY_OFF) {
            if (mFeatureProgress) {
                horizontalProgressBar.setVisibility(View.GONE);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.GONE);
            }
        } else if (value == Window.PROGRESS_INDETERMINATE_ON) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == Window.PROGRESS_INDETERMINATE_OFF) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (Window.PROGRESS_START <= value && value <= Window.PROGRESS_END) {
            // We want to set the progress value before testing for visibility
            // so that when the progress bar becomes visible again, it has the
            // correct level.
            horizontalProgressBar.setProgress(value - Window.PROGRESS_START);

            if (value < Window.PROGRESS_END) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        }
    }

    private void showProgressBars(ProgressBarICS horizontalProgressBar,
            ProgressBarICS spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.INVISIBLE) {
            spinnyProgressBar.setVisibility(View.VISIBLE);
        }
        // Only show the progress bars if the primary progress is not complete
        if (mFeatureProgress && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBars(ProgressBarICS horizontalProgressBar,
            ProgressBarICS spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.VISIBLE) {
            spinnyProgressBar.setVisibility(View.INVISIBLE);
        }
        if (mFeatureProgress && horizontalProgressBar.getVisibility() == View.VISIBLE) {
            horizontalProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private ProgressBarICS getCircularProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) mActionBarView.findViewById(R.id.progress_circular);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    private ProgressBarICS getHorizontalProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) mActionBarView.findViewById(R.id.progress_horizontal);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    private boolean initializePanelMenu() {
        mMenu = new MenuBuilder(getActionBarThemedContext());
        mMenu.setCallback(this);
        return true;
    }

    private boolean preparePanel() {
        // Already prepared (isPrepared will be reset to false later)
        if (mPanelIsPrepared) {
            return true;
        }

        // Init the panel state's menu--return false if init failed
        if (mMenu == null || mPanelRefreshContent) {
            if (mMenu == null) {
                if (!initializePanelMenu() || (mMenu == null)) {
                    return false;
                }
            }

            if (mActionBarView != null) {
                mActionBarView.setMenu(mMenu, this);
            }

            // Creating the panel menu will involve a lot of manipulation;
            // don't dispatch change events to presenters until we're done.
            mMenu.stopDispatchingItemsChanged();

            // Call callback, and return if it doesn't want to display menu.
            if (!mActivity.superOnCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, mMenu)) {
                // Ditch the menu created above
                mMenu = null;

                if (mActionBarView != null) {
                    // Don't show it in the action bar either
                    mActionBarView.setMenu(null, this);
                }

                return false;
            }

            mPanelRefreshContent = false;
        }

        // Preparing the panel menu can involve a lot of manipulation;
        // don't dispatch change events to presenters until we're done.
        mMenu.stopDispatchingItemsChanged();

        // Restore action view state before we prepare. This gives apps
        // an opportunity to override frozen/restored state in onPrepare.
        if (mPanelFrozenActionViewState != null) {
            mMenu.restoreActionViewStates(mPanelFrozenActionViewState);
            mPanelFrozenActionViewState = null;
        }

        // Callback and return if the callback does not want to show the menu
        if (!mActivity.superOnPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, mMenu)) {
            if (mActionBarView != null) {
                // The app didn't want to show the menu for now but it still exists.
                // Clear it out of the action bar.
                mActionBarView.setMenu(null, this);
            }
            mMenu.startDispatchingItemsChanged();
            return false;
        }

        mMenu.startDispatchingItemsChanged();

        // Set other state
        mPanelIsPrepared = true;

        return true;
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mActivity.onSupportActionModeFinished(mode);
            mActionMode = null;
        }
    }

}
