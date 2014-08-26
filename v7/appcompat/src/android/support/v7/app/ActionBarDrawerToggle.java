/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.appcompat.R;

/**
 * This class provides a handy way to tie together the functionality of
 * {@link android.support.v4.widget.DrawerLayout} and the framework <code>ActionBar</code> to
 * implement the recommended design for navigation drawers.
 *
 * <p>To use <code>ActionBarDrawerToggle</code>, create one in your Activity and call through
 * to the following methods corresponding to your Activity callbacks:</p>
 *
 * <ul>
 * <li>{@link android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
 * onConfigurationChanged}
 * <li>{@link android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
 * onOptionsItemSelected}</li>
 * </ul>
 *
 * <p>Call {@link #syncState()} from your <code>Activity</code>'s
 * {@link android.app.Activity#onPostCreate(android.os.Bundle) onPostCreate} to synchronize the
 * indicator with the state of the linked DrawerLayout after <code>onRestoreInstanceState</code>
 * has occurred.</p>
 *
 * <p><code>ActionBarDrawerToggle</code> can be used directly as a
 * {@link android.support.v4.widget.DrawerLayout.DrawerListener}, or if you are already providing
 * your own listener, call through to each of the listener methods from your own.</p>
 */
public class ActionBarDrawerToggle implements DrawerLayout.DrawerListener {

    /**
     * Allows an implementing Activity to return an {@link ActionBarDrawerToggle.Delegate} to use
     * with ActionBarDrawerToggle.
     */
    public interface DelegateProvider {

        /**
         * @return Delegate to use for ActionBarDrawableToggles, or null if the Activity
         * does not wish to override the default behavior.
         */
        @Nullable
        Delegate getDrawerToggleDelegate();
    }

    public interface Delegate {

        /**
         * Set the Action Bar's up indicator drawable and content description.
         *
         * @param upDrawable     - Drawable to set as up indicator
         * @param contentDescRes - Content description to set
         */
        void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes);

        /**
         * Set the Action Bar's up indicator content description.
         *
         * @param contentDescRes - Content description to set
         */
        void setActionBarDescription(@StringRes int contentDescRes);
    }

    private final Delegate mActivityImpl;
    private final DrawerLayout mDrawerLayout;

    private DrawerToggle mSlider;
    private final int mOpenDrawerContentDescRes;
    private final int mCloseDrawerContentDescRes;

    /**
     * Construct a new ActionBarDrawerToggle.
     *
     * <p>The given {@link Activity} will be linked to the specified {@link DrawerLayout} and
     * its Actionbar's Up button will be set to a custom drawable.
     * <p>This drawable shows a Hamburger icon when drawer is closed and an arrow when drawer
     * is open. It animates between these two states as the drawer opens.</p>
     *
     * <p>String resources must be provided to describe the open/close drawer actions for
     * accessibility services.</p>
     *
     * @param activity                  The Activity hosting the drawer. Should have an ActionBar.
     * @param drawerLayout              The DrawerLayout to link to the given Activity's ActionBar
     * @param openDrawerContentDescRes  A String resource to describe the "open drawer" action
     *                                  for accessibility
     * @param closeDrawerContentDescRes A String resource to describe the "close drawer" action
     *                                  for accessibility
     */
    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
            @StringRes int openDrawerContentDescRes,
            @StringRes int closeDrawerContentDescRes) {
        this(activity, null, drawerLayout, new DrawerArrowDrawableToggle(activity)
                , openDrawerContentDescRes, closeDrawerContentDescRes);
    }

    /**
     * Construct a new ActionBarDrawerToggle with a Toolbar.
     * <p>
     * The given {@link Activity} will be linked to the specified {@link DrawerLayout} and
     * the Toolbar's navigation icon will be set to a custom drawable. Using this constructor
     * will set Toolbar's navigation click listener to toggle the drawer when it is clicked.
     * <p>
     * This drawable shows a Hamburger icon when drawer is closed and an arrow when drawer
     * is open. It animates between these two states as the drawer opens.
     * <p>
     * String resources must be provided to describe the open/close drawer actions for
     * accessibility services.
     * <p>
     * Please use {@link #ActionBarDrawerToggle(Activity, DrawerLayout, int, int)} if you are
     * setting the Toolbar as the ActionBar of your activity.
     *
     * @param activity                  The Activity hosting the drawer.
     * @param toolbar                   The toolbar to use if you have an independent Toolbar.
     * @param drawerLayout              The DrawerLayout to link to the given Activity's ActionBar
     * @param openDrawerContentDescRes  A String resource to describe the "open drawer" action
     *                                  for accessibility
     * @param closeDrawerContentDescRes A String resource to describe the "close drawer" action
     *                                  for accessibility
     */
    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
            Toolbar toolbar, @StringRes int openDrawerContentDescRes,
            @StringRes int closeDrawerContentDescRes) {
        this(activity, toolbar, drawerLayout, new DrawerArrowDrawableToggle(activity)
                , openDrawerContentDescRes, closeDrawerContentDescRes);
    }

    /**
     * In the future, we can make this constructor public if we want to let developers customize
     * the
     * animation.
     */
    <T extends Drawable & DrawerToggle> ActionBarDrawerToggle(Activity activity, Toolbar toolbar,
            DrawerLayout drawerLayout, T slider,
            @StringRes int openDrawerContentDescRes,
            @StringRes int closeDrawerContentDescRes) {
        if (toolbar != null) {
            mActivityImpl = new ToolbarCompatDelegate(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggle();
                }
            });
        } else if (activity instanceof DelegateProvider) { // Allow the Activity to provide an impl
            mActivityImpl = ((DelegateProvider) activity).getDrawerToggleDelegate();
        } else if (activity instanceof ActionBarActivity) {
            mActivityImpl = new AppCompatDelegate((ActionBarActivity) activity);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mActivityImpl = new HoneycombDelegate(activity);
        } else {
            mActivityImpl = new DummyDelegate();
        }

        mDrawerLayout = drawerLayout;
        mOpenDrawerContentDescRes = openDrawerContentDescRes;
        mCloseDrawerContentDescRes = closeDrawerContentDescRes;
        mSlider = slider;
    }

    /**
     * Synchronize the state of the drawer indicator/affordance with the linked DrawerLayout.
     *
     * <p>This should be called from your <code>Activity</code>'s
     * {@link Activity#onPostCreate(android.os.Bundle) onPostCreate} method to synchronize after
     * the DrawerLayout's instance state has been restored, and any other time when the state
     * may have diverged in such a way that the ActionBarDrawerToggle was not notified.
     * (For example, if you stop forwarding appropriate drawer events for a period of time.)</p>
     */
    public void syncState() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mSlider.setPosition(1);
        } else {
            mSlider.setPosition(0);
        }

        setActionBarUpIndicator((Drawable) mSlider,
                mDrawerLayout.isDrawerOpen(GravityCompat.START) ?
                        mCloseDrawerContentDescRes : mOpenDrawerContentDescRes);
    }

    /**
     * This method should always be called by your <code>Activity</code>'s
     * {@link Activity#onConfigurationChanged(android.content.res.Configuration)
     * onConfigurationChanged}
     * method.
     *
     * @param newConfig The new configuration
     */
    public void onConfigurationChanged(Configuration newConfig) {
        syncState();
    }

    /**
     * This method should be called by your <code>Activity</code>'s
     * {@link Activity#onOptionsItemSelected(android.view.MenuItem) onOptionsItemSelected} method.
     * If it returns true, your <code>onOptionsItemSelected</code> method should return true and
     * skip further processing.
     *
     * @param item the MenuItem instance representing the selected menu item
     * @return true if the event was handled and further processing should not occur
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            toggle();
            return true;
        }
        return false;
    }

    private void toggle() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use your
     * ActionBarDrawerToggle instance directly as your DrawerLayout's listener, you should call
     * through to this method from your own listener object.
     *
     * @param drawerView  The child view that was moved
     * @param slideOffset The new offset of this drawer within its range, from 0-1
     */
    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        mSlider.setPosition(Math.min(1f, Math.max(0, slideOffset)));
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use your
     * ActionBarDrawerToggle instance directly as your DrawerLayout's listener, you should call
     * through to this method from your own listener object.
     *
     * @param drawerView Drawer view that is now open
     */
    @Override
    public void onDrawerOpened(View drawerView) {
        mSlider.setPosition(1);
        setActionBarDescription(mCloseDrawerContentDescRes);
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use your
     * ActionBarDrawerToggle instance directly as your DrawerLayout's listener, you should call
     * through to this method from your own listener object.
     *
     * @param drawerView Drawer view that is now closed
     */
    @Override
    public void onDrawerClosed(View drawerView) {
        mSlider.setPosition(0);
        setActionBarDescription(mOpenDrawerContentDescRes);
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use your
     * ActionBarDrawerToggle instance directly as your DrawerLayout's listener, you should call
     * through to this method from your own listener object.
     *
     * @param newState The new drawer motion state
     */
    @Override
    public void onDrawerStateChanged(int newState) {
    }

    void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
        mActivityImpl.setActionBarUpIndicator(upDrawable, contentDescRes);
    }

    void setActionBarDescription(int contentDescRes) {
        mActivityImpl.setActionBarDescription(contentDescRes);
    }


    static class DrawerArrowDrawableToggle extends DrawerArrowDrawable
            implements DrawerToggle {

        private final Activity mActivity;

        public DrawerArrowDrawableToggle(Activity activity) {
            super(activity);
            mActivity = activity;
        }

        public void setPosition(float position) {
            if (position == 1f) {
                setVerticalMirror(true);
            } else if (position == 0f) {
                setVerticalMirror(false);
            }
            super.setProgress(position);
        }

        @Override
        boolean isLayoutRtl() {
            return ViewCompat.getLayoutDirection(mActivity.getWindow().getDecorView())
                    == ViewCompat.LAYOUT_DIRECTION_RTL;
        }

        public float getPosition() {
            return super.getProgress();
        }
    }

    /**
     * Interface for toggle drawables. Can be public in the future
     */
    static interface DrawerToggle {

        public void setPosition(float position);

        public float getPosition();
    }

    /**
     * ActionBarDrawerToggle delegate for Activities which extend ActionBarActivity.
     */
    static class AppCompatDelegate implements Delegate {

        final ActionBarActivity mActivity;

        AppCompatDelegate(ActionBarActivity activity) {
            mActivity = activity;
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes) {
            final ActionBar supportActionBar = mActivity.getSupportActionBar();
            supportActionBar.setHomeAsUpIndicator(upDrawable);
            supportActionBar.setHomeActionContentDescription(contentDescRes);
        }

        @Override
        public void setActionBarDescription(@StringRes int contentDescRes) {
            mActivity.getSupportActionBar().setHomeActionContentDescription(contentDescRes);
        }
    }

    /**
     * ActionbarDrawerToggle delegate for Activities which don't extend ActionBarActivity but can
     * use
     * Framework's ActionBar.
     */
    static class HoneycombDelegate implements Delegate {

        final Activity mActivity;

        HoneycombDelegate(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes) {
            final android.app.ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(upDrawable);
                actionBar.setHomeActionContentDescription(contentDescRes);
            }
        }

        @Override
        public void setActionBarDescription(@StringRes int contentDescRes) {
            final android.app.ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                actionBar.setHomeActionContentDescription(contentDescRes);
            }
        }
    }

    static class ToolbarCompatDelegate implements Delegate {
        final Toolbar mToolbar;

        ToolbarCompatDelegate(Toolbar toolbar) {
            mToolbar = toolbar;
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes) {
            mToolbar.setNavigationIcon(upDrawable);
            mToolbar.setNavigationContentDescription(contentDescRes);
        }

        @Override
        public void setActionBarDescription(@StringRes int contentDescRes) {
            mToolbar.setNavigationContentDescription(contentDescRes);
        }
    }

    /**
     * Delegate for activities without an actionbar.
     */
    static class DummyDelegate implements Delegate {

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes) {

        }

        @Override
        public void setActionBarDescription(@StringRes int contentDescRes) {

        }
    }
}
