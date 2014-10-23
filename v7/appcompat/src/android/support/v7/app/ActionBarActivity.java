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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.WindowCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Base class for activities that use the <a
 * href="{@docRoot}tools/extras/support-library.html">support library</a> action bar features.
 *
 * <p>You can add an {@link ActionBar} to your activity when running on API level 7 or higher
 * by extending this class for your activity and setting the activity theme to
 * {@link android.support.v7.appcompat.R.style#Theme_AppCompat Theme.AppCompat} or a similar theme.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 *
 * <p>For information about how to use the action bar, including how to add action items, navigation
 * modes and more, read the <a href="{@docRoot}guide/topics/ui/actionbar.html">Action
 * Bar</a> API guide.</p>
 * </div>
 */
public class ActionBarActivity extends FragmentActivity implements ActionBar.Callback,
        TaskStackBuilder.SupportParentable, ActionBarDrawerToggle.DelegateProvider,
        android.support.v7.app.ActionBarDrawerToggle.TmpDelegateProvider {

    private ActionBarActivityDelegate mDelegate;

    /**
     * Support library version of {@link Activity#getActionBar}.
     *
     * <p>Retrieve a reference to this activity's ActionBar.
     *
     * @return The Activity's ActionBar, or null if it does not have one.
     */
    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    /**
     * Set a {@link android.widget.Toolbar Toolbar} to act as the {@link ActionBar} for this
     * Activity window.
     *
     * <p>When set to a non-null value the {@link #getActionBar()} method will return
     * an {@link ActionBar} object that can be used to control the given toolbar as if it were
     * a traditional window decor action bar. The toolbar's menu will be populated with the
     * Activity's options menu and the navigation button will be wired through the standard
     * {@link android.R.id#home home} menu select action.</p>
     *
     * <p>In order to use a Toolbar within the Activity's window content the application
     * must not request the window feature {@link Window#FEATURE_ACTION_BAR FEATURE_ACTION_BAR}.</p>
     *
     * @param toolbar Toolbar to set as the Activity's action bar
     */
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDelegate().onCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    public View onCreatePanelView(int featureId) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            return getDelegate().onCreatePanelView(featureId);
        } else {
            return super.onCreatePanelView(featureId);
        }
    }

    @Override
    public final boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }

        final ActionBar ab = getSupportActionBar();
        if (item.getItemId() == android.R.id.home && ab != null &&
                (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            return onSupportNavigateUp();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().destroy();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().onTitleChanged(title);
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
        return getDelegate().supportRequestWindowFeature(featureId);
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        getDelegate().supportInvalidateOptionsMenu();
    }

    /**
     * @hide
     */
    public void invalidateOptionsMenu() {
        getDelegate().supportInvalidateOptionsMenu();
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

    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        return getDelegate().startSupportActionMode(callback);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return getDelegate().onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return getDelegate().onPreparePanel(featureId, view, menu);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        getDelegate().onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return getDelegate().onMenuOpened(featureId, menu);
    }

    /**
     * @hide
     */
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        return getDelegate().onPrepareOptionsPanel(view, menu);
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

    boolean superOnPreparePanel(int featureId, View view, android.view.Menu menu) {
        return super.onPreparePanel(featureId, view, menu);
    }

    boolean superOnPrepareOptionsPanel(View view, Menu menu) {
        return super.onPrepareOptionsPanel(view, menu);
    }

    void superOnPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

    boolean superOnMenuOpened(int featureId, Menu menu) {
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onBackPressed() {
        if (!getDelegate().onBackPressed()) {
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
        getDelegate().setSupportProgressBarVisibility(visible);
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
        getDelegate().setSupportProgressBarIndeterminateVisibility(visible);
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
        getDelegate().setSupportProgressBarIndeterminate(indeterminate);
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
        getDelegate().setSupportProgress(progress);
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

    /**
     * This method is called whenever the user chooses to navigate Up within your application's
     * activity hierarchy from the action bar.
     *
     * <p>If a parent was specified in the manifest for this activity or an activity-alias to it,
     * default Up navigation will be handled automatically. See
     * {@link #getSupportParentActivityIntent()} for how to specify the parent. If any activity
     * along the parent chain requires extra Intent arguments, the Activity subclass
     * should override the method {@link #onPrepareSupportNavigateUpTaskStack(TaskStackBuilder)}
     * to supply those arguments.</p>
     *
     * <p>See <a href="{@docRoot}guide/topics/fundamentals/tasks-and-back-stack.html">Tasks and
     * Back Stack</a> from the developer guide and
     * <a href="{@docRoot}design/patterns/navigation.html">Navigation</a> from the design guide
     * for more information about navigating within your app.</p>
     *
     * <p>See the {@link TaskStackBuilder} class and the Activity methods
     * {@link #getSupportParentActivityIntent()}, {@link #supportShouldUpRecreateTask(Intent)}, and
     * {@link #supportNavigateUpTo(Intent)} for help implementing custom Up navigation.</p>
     *
     * @return true if Up navigation completed successfully and this Activity was finished,
     *         false otherwise.
     */
    public boolean onSupportNavigateUp() {
        Intent upIntent = getSupportParentActivityIntent();

        if (upIntent != null) {
            if (supportShouldUpRecreateTask(upIntent)) {
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
                supportNavigateUpTo(upIntent);
            }
            return true;
        }
        return false;
    }

    /**
     * Obtain an {@link Intent} that will launch an explicit target activity
     * specified by sourceActivity's {@link NavUtils#PARENT_ACTIVITY} &lt;meta-data&gt;
     * element in the application's manifest. If the device is running
     * Jellybean or newer, the android:parentActivityName attribute will be preferred
     * if it is present.
     *
     * @return a new Intent targeting the defined parent activity of sourceActivity
     */
    public Intent getSupportParentActivityIntent() {
        return NavUtils.getParentActivityIntent(this);
    }

    /**
     * Returns true if sourceActivity should recreate the task when navigating 'up'
     * by using targetIntent.
     *
     * <p>If this method returns false the app can trivially call
     * {@link #supportNavigateUpTo(Intent)} using the same parameters to correctly perform
     * up navigation. If this method returns false, the app should synthesize a new task stack
     * by using {@link TaskStackBuilder} or another similar mechanism to perform up navigation.</p>
     *
     * @param targetIntent An intent representing the target destination for up navigation
     * @return true if navigating up should recreate a new task stack, false if the same task
     *         should be used for the destination
     */
    public boolean supportShouldUpRecreateTask(Intent targetIntent) {
        return NavUtils.shouldUpRecreateTask(this, targetIntent);
    }

    /**
     * Navigate from sourceActivity to the activity specified by upIntent, finishing sourceActivity
     * in the process. upIntent will have the flag {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} set
     * by this method, along with any others required for proper up navigation as outlined
     * in the Android Design Guide.
     *
     * <p>This method should be used when performing up navigation from within the same task
     * as the destination. If up navigation should cross tasks in some cases, see
     * {@link #supportShouldUpRecreateTask(Intent)}.</p>
     *
     * @param upIntent An intent representing the target destination for up navigation
     */
    public void supportNavigateUpTo(Intent upIntent) {
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public final ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return getDelegate().getDrawerToggleDelegate();
    }

    @Nullable
    @Override
    /**
     * Temporary method until ActionBarDrawerToggle transition from v4 to v7 is complete.
     */
    public android.support.v7.app.ActionBarDrawerToggle.Delegate getV7DrawerToggleDelegate() {
        return getDelegate().getV7DrawerToggleDelegate();
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return getDelegate().onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First let the Activity try and handle it (for back, etc)
        if (super.onKeyDown(keyCode, event)) {
            return true;
        }
        return getDelegate().onKeyDown(keyCode, event);
    }

    /**
     * Use {@link #onSupportContentChanged()} instead.
     */
    public final void onContentChanged() {
        getDelegate().onContentChanged();
    }

    /**
     * This hook is called whenever the content view of the screen changes.
     * @see android.app.Activity#onContentChanged()
     */
    public void onSupportContentChanged() {
    }

    @Override
    public View onCreateView(String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        // Allow super (FragmentActivity) to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }
        // If we reach here super didn't create a View, so let our delegate attempt it
        return getDelegate().createView(name, attrs);
    }

    private ActionBarActivityDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = ActionBarActivityDelegate.createDelegate(this);
        }
        return mDelegate;
    }
}
