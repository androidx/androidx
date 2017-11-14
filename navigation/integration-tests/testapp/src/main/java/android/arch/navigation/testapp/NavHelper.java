/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.navigation.testapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.arch.navigation.NavController;
import android.arch.navigation.NavDestination;
import android.arch.navigation.NavOptions;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewParent;

/**
 * Class which hooks up elements typically in the 'chrome' of your application such as global
 * navigation patterns like a navigation drawer or bottom nav bar with your {@link NavController}.
 */
public class NavHelper {

    // No instances. Static utilities only.
    private NavHelper() {
    }

    /**
     * Attempt to navigate to the {@link NavDestination} associated with the given MenuItem. This
     * MenuItem should have been added via one of the helper methods in this class.
     *
     * <p>Importantly, it assumes the {@link MenuItem#getItemId() menu item id} matches a valid
     * {@link NavDestination#getAction(int) action id} or
     * {@link NavDestination#getId() destination id} to be navigated to.</p>
     *
     * @param navController The NavController that hosts the destination.
     * @param item The selected MenuItem.
     * @return True if the {@link NavController} was able to navigate to the destination
     * associated with the given MenuItem.
     */
    public static boolean handleMenuItemSelected(NavController navController,
            @NonNull MenuItem item) {
        try {
            navController.navigate(item.getItemId(), null,
                    new NavOptions.Builder()
                            .setPopUpTo(navController.getGraph().getStartDestination(), false)
                            .setLaunchSingleTop(true)
                            .setEnterAnim(R.anim.fade_in)
                            .setExitAnim(R.anim.fade_out)
                            .setPopEnterAnim(R.anim.fade_in)
                            .setPopExitAnim(R.anim.fade_out)
                            .build());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Handles the Up button by delegating its behavior to the given NavController. This should
     * generally be called from {@link AppCompatActivity#onSupportNavigateUp()}.
     *
     * @param navController The NavController that hosts your content.
     * @return True if the {@link NavController} was able to navigate up.
     */
    public static boolean handleNavigateUp(NavController navController) {
        return handleNavigateUp(navController, null);
    }

    /**
     * Handles the Up button by delegating its behavior to the given NavController. This should
     * generally be called from {@link AppCompatActivity#onSupportNavigateUp()}.
     *
     * @param navController The NavController that hosts your content.
     * @param drawerLayout The DrawerLayout that should be opened if you are on the topmost level
     *                     of the app.
     * @return True if the {@link NavController} was able to navigate up.
     */
    public static boolean handleNavigateUp(NavController navController, DrawerLayout drawerLayout) {
        if (drawerLayout != null && navController.getCurrentDestination().getId()
                == navController.getGraph().getStartDestination()) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        } else {
            return navController.navigateUp();
        }
    }

    /**
     * Sets up the ActionBar returned by {@link AppCompatActivity#getSupportActionBar()} for use
     * with a {@link NavController}.
     *
     * <p>By calling this method, the title in the action bar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The action bar will also display the Up button when you are on a non-root destination.
     * Call {@link #handleNavigateUp(NavController, DrawerLayout)} to handle the Up button.
     *
     * @param navController The NavController that supplies the secondary menu. Navigation actions
     *                      on this NavController will be reflected in the title of the action bar.
     * @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     */
    public static void setupActionBar(final NavController navController,
            final AppCompatActivity activity) {
        setupActionBar(navController, activity, null);
    }

    /**
     * Sets up the ActionBar returned by {@link AppCompatActivity#getSupportActionBar()} for use
     * with a {@link NavController}.
     *
     * <p>By calling this method, the title in the action bar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The action bar will also display the Up button when you are on a non-root destination and
     * the drawer icon when on the root destination, automatically animating between them.
     * Call {@link #handleNavigateUp(NavController, DrawerLayout)} to handle the Up button.
     *
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the action bar.
     * @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param drawerLayout The DrawerLayout that should be toggled from the home button
     *                     when on the root destination
     */
    public static void setupActionBar(final NavController navController,
            final AppCompatActivity activity, final DrawerLayout drawerLayout) {
        navController.addOnNavigatedListener(
                new ActionBarOnNavigatedListener(activity, drawerLayout));
    }

    /**
     * Sets up a {@link NavigationView} for use with a {@link NavController}. This will call
     * {@link #handleMenuItemSelected(NavController, MenuItem)} when a menu item is selected.
     * The selected item in the NavigationView will automatically be updated when the destination
     * changes.
     *
     * @param navController The NavController that supplies the primary and secondary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the NavigationView.
     * @param navigationView The NavigationView that should be kept in sync with changes to the
     *                       NavController.
     */
    public static void setupNavigationView(final NavController navController,
            final NavigationView navigationView) {
        if (navigationView == null) {
            return;
        }
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        boolean handled = handleMenuItemSelected(navController, item);
                        if (handled) {
                            ViewParent parent = navigationView.getParent();
                            if (parent instanceof DrawerLayout) {
                                ((DrawerLayout) parent).closeDrawer(navigationView);
                            }
                        }
                        return handled;
                    }
                });
        navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
            @Override
            public void onNavigated(NavController controller, NavDestination destination) {
                int destinationId = destination.getId();
                Menu menu = navigationView.getMenu();
                for (int h = 0, size = menu.size(); h < size; h++) {
                    MenuItem item = menu.getItem(h);
                    item.setChecked(item.getItemId() == destinationId);
                }
            }
        });
    }

    /**
     * Sets up a {@link BottomNavigationView} for use with a {@link NavController}. This will call
     * {@link #handleMenuItemSelected(NavController, MenuItem)} when a menu item is selected. The
     * selected item in the BottomNavigationView will automatically be updated when the destination
     * changes.
     *
     * @param navController The NavController that supplies the primary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the BottomNavigationView.
     * @param bottomNavigationView The BottomNavigationView that should be kept in sync with
     *                             changes to the NavController.
     */
    public static void setupBottomNavigationView(final NavController navController,
            final BottomNavigationView bottomNavigationView) {
        if (bottomNavigationView == null) {
            return;
        }
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return handleMenuItemSelected(navController, item);
                    }
                });
        navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
            @Override
            public void onNavigated(NavController controller, NavDestination destination) {
                int destinationId = destination.getId();
                Menu menu = bottomNavigationView.getMenu();
                for (int h = 0, size = menu.size(); h < size; h++) {
                    MenuItem item = menu.getItem(h);
                    if (item.getItemId() == destinationId) {
                        item.setChecked(true);
                    }
                }
            }
        });
    }

    /**
     * The OnNavigatedListener specifically for keeping the ActionBar updated. This handles both
     * updating the title and updating the Up Indicator transitioning between the
     */
    private static class ActionBarOnNavigatedListener implements NavController.OnNavigatedListener {
        private final AppCompatActivity mActivity;
        private final DrawerLayout mDrawerLayout;
        private DrawerArrowDrawable mArrowDrawable;
        private ValueAnimator mAnimator;

        ActionBarOnNavigatedListener(
                AppCompatActivity activity, DrawerLayout drawerLayout) {
            mActivity = activity;
            mDrawerLayout = drawerLayout;
        }

        @Override
        public void onNavigated(NavController controller, NavDestination destination) {
            ActionBar actionBar = mActivity.getSupportActionBar();
            CharSequence title = destination.getLabel();
            if (!TextUtils.isEmpty(title)) {
                actionBar.setTitle(title);
            }
            boolean isStartDestination =
                    controller.getGraph().getStartDestination() == destination.getId();
            actionBar.setDisplayHomeAsUpEnabled(mDrawerLayout != null || !isStartDestination);
            setActionBarUpIndicator(mDrawerLayout != null && isStartDestination);
        }

        void setActionBarUpIndicator(boolean showAsDrawerIndicator) {
            ActionBarDrawerToggle.Delegate delegate = mActivity.getDrawerToggleDelegate();
            boolean animate = true;
            if (mArrowDrawable == null) {
                mArrowDrawable = new DrawerArrowDrawable(
                        delegate.getActionBarThemedContext());
                delegate.setActionBarUpIndicator(mArrowDrawable, 0);
                // We're setting the initial state, so skip the animation
                animate = false;
            }
            float endValue = showAsDrawerIndicator ? 0f : 1f;
            if (animate) {
                float startValue = mArrowDrawable.getProgress();
                if (mAnimator != null) {
                    mAnimator.cancel();
                }
                mAnimator = ObjectAnimator.ofFloat(mArrowDrawable, "progress",
                        startValue, endValue);
                mAnimator.start();
            } else {
                mArrowDrawable.setProgress(endValue);
            }
        }
    }
}
