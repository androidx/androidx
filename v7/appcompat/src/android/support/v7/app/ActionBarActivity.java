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

package android.support.v7.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.WindowCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.view.Menu;
import android.support.v7.view.MenuInflater;
import android.support.v7.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for activities that use the support library action bar features.
 */
public class ActionBarActivity extends FragmentActivity implements ActionBar.Callback {
    ActionBarActivityDelegate mImpl;
    private ArrayList<ActionBarFragmentCallbacks> mCreatedMenus;

    /**
     * Support library version of {@link Activity#getActionBar}.
     *
     * <p>Retrieve a reference to this activity's ActionBar.
     *
     * @return The Activity's ActionBar, or null if it does not have one.
     */
    public ActionBar getSupportActionBar() {
        return mImpl.getSupportActionBar();
    }

    /**
     * Support library version of {@link Activity#getMenuInflater}.
     *
     * <p>Returns a {@link MenuInflater} with this context.
     *
     * @return The Activity's menu inflater.
     */
    public MenuInflater getSupportMenuInflater() {
        return mImpl.getSupportMenuInflater();
    }

    @Override
    public void setContentView(int layoutResID) {
        mImpl.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        mImpl.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mImpl.setContentView(view, params);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mImpl = ActionBarActivityDelegate.createDelegate(this);
        super.onCreate(savedInstanceState);
        mImpl.onCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mImpl.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStop() {
        super.onStop();
        mImpl.onStop();
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        mImpl.onPostResume();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        return mImpl.onCreatePanelMenu(featureId, frameworkMenu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu) {
        return mImpl.onPreparePanel(featureId, view, menu);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            return mImpl.onCreatePanelView(featureId);
        } else {
            return super.onCreatePanelView(featureId);
        }
    }

    /**
     * You should override {@link #onCreateSupportOptionsMenu(android.support.v7.view.Menu)} when
     * using the support Action Bar.
     */
    @Override
    public final boolean onCreateOptionsMenu(android.view.Menu menu) {
        return false;
    }

    /**
     * You should override {@link #onPrepareSupportOptionsMenu(android.support.v7.view.Menu)} when
     * using the support Action Bar.
     */
    @Override
    public final boolean onPrepareOptionsMenu(android.view.Menu menu) {
        return false;
    }

    /**
     * You should override {@link #onSupportOptionsItemSelected(android.support.v7.view.MenuItem)}
     * when using the support Action Bar.
     */
    @Override
    public final boolean onOptionsItemSelected(android.view.MenuItem item) {
        return false;
    }

    @Override
    public final boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
        if (mImpl.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        mImpl.setTitle(title);
    }

    /**
     * Enable extended support library window features.
     * <p>
     * This is a convenience for calling
     * {@link android.view.Window#requestFeature getWindow().requestFeature()}.
     * </p>
     *
     * @param featureId The desired feature as defined in
     * {@link android.view.Window} or {@link WindowCompat}.
     * @return Returns true if the requested feature is supported and now enabled.
     *
     * @see android.app.Activity#requestWindowFeature
     * @see android.view.Window#requestFeature
     */
    public boolean supportRequestWindowFeature(int featureId) {
        return mImpl.supportRequestWindowFeature(featureId);
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        // Only call up to super on HC+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.supportInvalidateOptionsMenu();
        }
        mImpl.supportInvalidateOptionsMenu();
    }

    @Override
    public final void onActionModeFinished(android.view.ActionMode mode) {
        mImpl.onActionModeFinished(mode);
    }

    @Override
    public final void onActionModeStarted(android.view.ActionMode mode) {
        mImpl.onActionModeStarted(mode);
    }

    /**
     * Notifies the Activity that a support action mode has been started.
     * Activity subclasses overriding this method should call the superclass implementation.
     *
     * @param mode The new action mode.
     */
    public void onSupportActionModeStarted(ActionMode mode) {
    }

    /**
     * Notifies the activity that a support action mode has finished.
     * Activity subclasses overriding this method should call the superclass implementation.
     *
     * @param mode The action mode that just finished.
     */
    public void onSupportActionModeFinished(ActionMode mode) {
    }

    /**
     * Support library version of {@link Activity#onPrepareOptionsMenu}.
     *
     * <p>Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     *
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateSupportOptionsMenu().
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onCreateSupportOptionsMenu
     */
    public boolean onPrepareSupportOptionsMenu(android.support.v7.view.Menu menu) {
        return true;
    }

    /**
     * Support library version of {@link Activity#onCreateOptionsMenu}.
     *
     * <p>Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareSupportOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link android.support.v7.view.Menu#CATEGORY_SYSTEM}
     * group so that they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateSupportOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onSupportOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareSupportOptionsMenu
     * @see #onSupportOptionsItemSelected
     */
    public boolean onCreateSupportOptionsMenu(android.support.v7.view.Menu menu) {
        return true;
    }

    /**
     * Support library version of {@link Activity#onOptionsItemSelected}.
     *
     * <p>This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onPrepareSupportOptionsMenu
     * @see #onCreateSupportOptionsMenu
     */
    public boolean onSupportOptionsItemSelected(android.support.v7.view.MenuItem item) {
        return false;
    }

    /**
     * Support library version of {@link Activity#onMenuItemSelected}.
     *
     * <p>This calls through to the {@link #onSupportOptionsItemSelected}
     * method for the {@link android.view.Window#FEATURE_OPTIONS_PANEL}
     * panel, so that subclasses of Activity don't need to deal with feature codes.
     */
    public boolean onSupportMenuItemSelected(int featureId, MenuItem item) {
        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                if (onSupportOptionsItemSelected(item)) {
                    return true;
                }

                if (dispatchSupportOptionsItemSelectedToFragments(
                        getSupportFragmentManager(), item)) {
                    return true;
                }

                ActionBar ab = getSupportActionBar();
                if (item.getItemId() == android.R.id.home && ab != null &&
                        (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
                    return onSupportNavigateUp();
                }
                return false;

            default:
                return false;
        }
    }

    private boolean dispatchSupportOptionsItemSelectedToFragments(
            FragmentManager fragmentManager, MenuItem item) {
        if (fragmentManager != null) {
            final List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                final int count = fragments.size();
                for (int i = 0; i < count; i++) {
                    final Fragment fragment = fragments.get(i);
                    if (!fragment.isHidden()) {
                        if (fragment.hasOptionsMenu() && fragment.isMenuVisible()
                                && fragment instanceof ActionBarFragmentCallbacks) {
                            final ActionBarFragmentCallbacks callbacks =
                                    (ActionBarFragmentCallbacks)fragment;
                            if (callbacks.onSupportOptionsItemSelected(item)) {
                                return true;
                            }
                        }
                        if (dispatchSupportOptionsItemSelectedToFragments(
                                fragment.getChildFragmentManager(), item)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    boolean dispatchCreateSupportOptionsMenu(Menu menu) {
        boolean show = onCreateSupportOptionsMenu(menu);

        ArrayList<ActionBarFragmentCallbacks> newMenus =
                new ArrayList<ActionBarFragmentCallbacks>();
        show |= dispatchCreateSupportOptionsMenuToFragments(
                getSupportFragmentManager(), menu, newMenus);
        if (newMenus.isEmpty()) {
            newMenus = null;
        }

        if (mCreatedMenus != null) {
            final int count = mCreatedMenus.size();
            for (int i = 0; i < count; i++) {
                ActionBarFragmentCallbacks callbacks = mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(callbacks)) {
                    callbacks.onDestroySupportOptionsMenu();
                }
            }
        }
        mCreatedMenus = newMenus;
        return show;
    }

    private boolean dispatchCreateSupportOptionsMenuToFragments(
            FragmentManager fragmentManager, Menu menu,
            ArrayList<ActionBarFragmentCallbacks> newMenus) {
        boolean show = false;
        if (fragmentManager != null) {
            final List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                final int count = fragments.size();
                for (int i = 0; i < count; i++) {
                    final Fragment fragment = fragments.get(i);
                    if (!fragment.isHidden()) {
                        if (fragment.hasOptionsMenu() && fragment.isMenuVisible()
                                && fragment instanceof ActionBarFragmentCallbacks) {
                            final ActionBarFragmentCallbacks callbacks =
                                    (ActionBarFragmentCallbacks)fragment;
                            show = true;
                            callbacks.onCreateSupportOptionsMenu(menu, getSupportMenuInflater());
                            newMenus.add(callbacks);
                        }
                        show |= dispatchCreateSupportOptionsMenuToFragments(
                                fragment.getChildFragmentManager(), menu, newMenus);
                    }
                }
            }
        }
        return show;
    }

    boolean dispatchPrepareSupportOptionsMenu(Menu menu) {
        boolean show = onPrepareSupportOptionsMenu(menu);
        show |= dispatchPrepareSupportOptionsMenuToFragments(
                getSupportFragmentManager(), menu);
        return show;
    }

    private boolean dispatchPrepareSupportOptionsMenuToFragments(
            FragmentManager fragmentManager, Menu menu) {
        boolean show = false;
        if (fragmentManager != null) {
            final List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                final int count = fragments.size();
                for (int i = 0; i < count; i++) {
                    final Fragment fragment = fragments.get(i);
                    if (!fragment.isHidden()) {
                        if (fragment.hasOptionsMenu() && fragment.isMenuVisible()
                                && fragment instanceof ActionBarFragmentCallbacks) {
                            final ActionBarFragmentCallbacks callbacks =
                                    (ActionBarFragmentCallbacks)fragment;
                            show = true;
                            callbacks.onPrepareSupportOptionsMenu(menu);
                        }
                        show |= dispatchPrepareSupportOptionsMenuToFragments(
                                fragment.getChildFragmentManager(), menu);
                    }
                }
            }
        }
        return show;
    }

    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        return mImpl.startSupportActionMode(callback);
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

    boolean superOnCreatePanelMenu(int featureId, android.view.Menu frameworkMenu) {
        return super.onCreatePanelMenu(featureId, frameworkMenu);
    }

    boolean superOnPreparePanelMenu(int featureId, View view, android.view.Menu menu) {
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public void onBackPressed() {
        if (!mImpl.onBackPressed()) {
            super.onBackPressed();
        }
    }

    /**
     * Support library version of {@link Activity#setProgressBarVisibility(boolean)}
     * <p>
     * Sets the visibility of the progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #supportRequestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public void setSupportProgressBarVisibility(boolean visible) {
        mImpl.setSupportProgressBarVisibility(visible);
    }

    /**
     * Support library version of {@link Activity#setProgressBarIndeterminateVisibility(boolean)}
     * <p>
     * Sets the visibility of the indeterminate progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #supportRequestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        mImpl.setSupportProgressBarIndeterminateVisibility(visible);
    }

    /**
     * Support library version of {@link Activity#setProgressBarIndeterminate(boolean)}
     * <p>
     * Sets whether the horizontal progress bar in the title should be indeterminate (the
     * circular is always indeterminate).
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #supportRequestWindowFeature(int)}.
     *
     * @param indeterminate Whether the horizontal progress bar should be indeterminate.
     */
    public void setSupportProgressBarIndeterminate(boolean indeterminate) {
        mImpl.setSupportProgressBarIndeterminate(indeterminate);
    }

    /**
     * Support library version of {@link Activity#setProgress(int)}.
     * <p>
     * Sets the progress for the progress bars in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #supportRequestWindowFeature(int)}.
     *
     * @param progress The progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive). If 10000 is given, the progress
     *            bar will be completely filled and will fade out.
     */
    public void setSupportProgress(int progress) {
        mImpl.setSupportProgress(progress);
    }

    /**
     * Support version of {@link #onCreateNavigateUpTaskStack(android.app.TaskStackBuilder)}.
     * This method will be called on all platform versions.
     *
     * Define the synthetic task stack that will be generated during Up navigation from
     * a different task.
     *
     * <p>The default implementation of this method adds the parent chain of this activity
     * as specified in the manifest to the supplied {@link TaskStackBuilder}. Applications
     * may choose to override this method to construct the desired task stack in a different
     * way.</p>
     *
     * <p>This method will be invoked by the default implementation of {@link #onNavigateUp()}
     * if {@link #shouldUpRecreateTask(Intent)} returns true when supplied with the intent
     * returned by {@link #getParentActivityIntent()}.</p>
     *
     * <p>Applications that wish to supply extra Intent parameters to the parent stack defined
     * by the manifest should override
     * {@link #onPrepareSupportNavigateUpTaskStack(TaskStackBuilder)}.</p>
     *
     * @param builder An empty TaskStackBuilder - the application should add intents representing
     *                the desired task stack
     */
    public void onCreateSupportNavigateUpTaskStack(TaskStackBuilder builder) {
        builder.addParentStack(this);
    }

    /**
     * Support version of {@link #onPrepareNavigateUpTaskStack(android.app.TaskStackBuilder)}.
     * This method will be called on all platform versions.
     *
     * Prepare the synthetic task stack that will be generated during Up navigation
     * from a different task.
     *
     * <p>This method receives the {@link TaskStackBuilder} with the constructed series of
     * Intents as generated by {@link #onCreateSupportNavigateUpTaskStack(TaskStackBuilder)}.
     * If any extra data should be added to these intents before launching the new task,
     * the application should override this method and add that data here.</p>
     *
     * @param builder A TaskStackBuilder that has been populated with Intents by
     *                onCreateNavigateUpTaskStack.
     */
    public void onPrepareSupportNavigateUpTaskStack(TaskStackBuilder builder) {
    }

    public boolean onSupportNavigateUp() {
        Intent upIntent = NavUtils.getParentActivityIntent(this);

        if (upIntent != null) {
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                TaskStackBuilder b = TaskStackBuilder.create(this);
                onCreateSupportNavigateUpTaskStack(b);
                onPrepareSupportNavigateUpTaskStack(b);
                b.startActivities();

                try {
                    ActivityCompat.finishAffinity(this);
                } catch (IllegalStateException e) {
                    // This can only happen on 4.1+, when we don't have a parent or a result set.
                    // In that case we should just finish().
                    finish();
                }
            } else {
                // This activity is part of the application's task, so simply
                // navigate up to the hierarchical parent activity.
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        }
        return false;
    }

}
