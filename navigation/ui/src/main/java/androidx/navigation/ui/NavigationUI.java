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

package androidx.navigation.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;

/**
 * Class which hooks up elements typically in the 'chrome' of your application such as global
 * navigation patterns like a navigation drawer or bottom nav bar with your {@link NavController}.
 */
public class NavigationUI {

    // No instances. Static utilities only.
    private NavigationUI() {
    }

    /**
     * Attempt to navigate to the {@link NavDestination} associated with the given MenuItem. This
     * MenuItem should have been added via one of the helper methods in this class.
     *
     * <p>Importantly, it assumes the {@link MenuItem#getItemId() menu item id} matches a valid
     * {@link NavDestination#getAction(int) action id} or
     * {@link NavDestination#getId() destination id} to be navigated to.</p>
     *
     * @param item The selected MenuItem.
     * @param navController The NavController that hosts the destination.
     * @return True if the {@link NavController} was able to navigate to the destination
     * associated with the given MenuItem.
     */
    public static boolean onNavDestinationSelected(@NonNull MenuItem item,
            @NonNull NavController navController) {
        return onNavDestinationSelected(item, navController, false);
    }

    private static boolean onNavDestinationSelected(@NonNull MenuItem item,
            @NonNull NavController navController, boolean popUp) {
        NavOptions.Builder builder = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.nav_default_enter_anim)
                .setExitAnim(R.anim.nav_default_exit_anim)
                .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                .setPopExitAnim(R.anim.nav_default_pop_exit_anim);
        if (popUp) {
            builder.setPopUpTo(findStartDestination(navController.getGraph()).getId(), false);
        }
        NavOptions options = builder.build();
        try {
            //TODO provide proper API instead of using Exceptions as Control-Flow.
            navController.navigate(item.getItemId(), null, options);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Handles the Up button by delegating its behavior to the given NavController. This should
     * generally be called from {@link AppCompatActivity#onSupportNavigateUp()}.
     * <p>If you do not have a {@link DrawerLayout}, you should call
     * {@link NavController#navigateUp()} directly.
     *
     * @param drawerLayout The DrawerLayout that should be opened if you are on the topmost level
     *                     of the app.
     * @param navController The NavController that hosts your content.
     * @return True if the {@link NavController} was able to navigate up.
     */
    public static boolean navigateUp(@Nullable DrawerLayout drawerLayout,
            @NonNull NavController navController) {
        if (drawerLayout != null && navController.getCurrentDestination()
                == findStartDestination(navController.getGraph())) {
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
     * Call {@link #navigateUp(DrawerLayout, NavController)} to handle the Up button.
     *
     * @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param navController The NavController that supplies the secondary menu. Navigation actions
 *                      on this NavController will be reflected in the title of the action bar.
     */
    public static void setupActionBarWithNavController(@NonNull AppCompatActivity activity,
            @NonNull NavController navController) {
        setupActionBarWithNavController(activity, navController, null);
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
     * Call {@link #navigateUp(DrawerLayout, NavController)} to handle the Up button.
     *  @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the action bar.
     * @param drawerLayout The DrawerLayout that should be toggled from the home button
     */
    public static void setupActionBarWithNavController(@NonNull AppCompatActivity activity,
            @NonNull NavController navController,
            @Nullable DrawerLayout drawerLayout) {
        navController.addOnNavigatedListener(
                new ActionBarOnNavigatedListener(activity, drawerLayout));
    }

    /**
     * Sets up a {@link NavigationView} for use with a {@link NavController}. This will call
     * {@link #onNavDestinationSelected(MenuItem, NavController)} when a menu item is selected.
     * The selected item in the NavigationView will automatically be updated when the destination
     * changes.
     *
     * @param navigationView The NavigationView that should be kept in sync with changes to the
     *                       NavController.
     * @param navController The NavController that supplies the primary and secondary menu.
 *                      Navigation actions on this NavController will be reflected in the
 *                      selected item in the NavigationView.
     */
    public static void setupWithNavController(@NonNull final NavigationView navigationView,
            @NonNull final NavController navController) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        boolean handled = onNavDestinationSelected(item, navController, true);
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
            public void onNavigated(@NonNull NavController controller,
                    @NonNull NavDestination destination) {
                Menu menu = navigationView.getMenu();
                for (int h = 0, size = menu.size(); h < size; h++) {
                    MenuItem item = menu.getItem(h);
                    item.setChecked(matchDestination(destination, item.getItemId()));
                }
            }
        });
    }

    /**
     * Sets up a {@link BottomNavigationView} for use with a {@link NavController}. This will call
     * {@link #onNavDestinationSelected(MenuItem, NavController)} when a menu item is selected. The
     * selected item in the BottomNavigationView will automatically be updated when the destination
     * changes.
     *
     * @param bottomNavigationView The BottomNavigationView that should be kept in sync with
     *                             changes to the NavController.
     * @param navController The NavController that supplies the primary menu.
 *                      Navigation actions on this NavController will be reflected in the
 *                      selected item in the BottomNavigationView.
     */
    public static void setupWithNavController(
            @NonNull final BottomNavigationView bottomNavigationView,
            @NonNull final NavController navController) {
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return onNavDestinationSelected(item, navController, true);
                    }
                });
        navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
            @Override
            public void onNavigated(@NonNull NavController controller,
                    @NonNull NavDestination destination) {
                Menu menu = bottomNavigationView.getMenu();
                for (int h = 0, size = menu.size(); h < size; h++) {
                    MenuItem item = menu.getItem(h);
                    if (matchDestination(destination, item.getItemId())) {
                        item.setChecked(true);
                    }
                }
            }
        });
    }

    /**
     * Determines whether the given <code>destId</code> matches the NavDestination. This handles
     * both the default case (the destination's id matches the given id) and the nested case where
     * the given id is a parent/grandparent/etc of the destination.
     */
    private static boolean matchDestination(@NonNull NavDestination destination,
            @IdRes int destId) {
        NavDestination currentDestination = destination;
        while (currentDestination.getId() != destId && currentDestination.getParent() != null) {
            currentDestination = currentDestination.getParent();
        }
        return currentDestination.getId() == destId;
    }

    /**
     * Finds the actual start destination of the graph, handling cases where the graph's starting
     * destination is itself a NavGraph.
     */
    private static NavDestination findStartDestination(@NonNull NavGraph graph) {
        NavDestination startDestination = graph;
        while (startDestination instanceof NavGraph) {
            NavGraph parent = (NavGraph) startDestination;
            startDestination = parent.findNode(parent.getStartDestination());
        }
        return startDestination;
    }

    /**
     * The OnNavigatedListener specifically for keeping the ActionBar updated. This handles both
     * updating the title and updating the Up Indicator transitioning between the
     */
    private static class ActionBarOnNavigatedListener implements NavController.OnNavigatedListener {
        private final AppCompatActivity mActivity;
        @Nullable
        private final DrawerLayout mDrawerLayout;
        private DrawerArrowDrawable mArrowDrawable;
        private ValueAnimator mAnimator;

        ActionBarOnNavigatedListener(
                @NonNull AppCompatActivity activity, @Nullable DrawerLayout drawerLayout) {
            mActivity = activity;
            mDrawerLayout = drawerLayout;
        }

        @Override
        public void onNavigated(@NonNull NavController controller,
                @NonNull NavDestination destination) {
            ActionBar actionBar = mActivity.getSupportActionBar();
            CharSequence title = destination.getLabel();
            if (!TextUtils.isEmpty(title)) {
                actionBar.setTitle(title);
            }
            boolean isStartDestination = findStartDestination(controller.getGraph()) == destination;
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
