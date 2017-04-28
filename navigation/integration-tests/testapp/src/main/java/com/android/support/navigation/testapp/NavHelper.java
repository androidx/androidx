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

package com.android.support.navigation.testapp;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.navigation.app.nav.NavController;
import android.support.navigation.app.nav.NavDestination;
import android.support.navigation.app.nav.NavGraph;
import android.support.navigation.app.nav.NavOptions;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class which hooks up elements typically in the 'chrome' of your application such as global
 * navigation patterns like a navigation drawer or bottom nav bar with your {@link NavController}.
 */
public class NavHelper {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef(flag = true,
            value = {NavDestination.NAV_TYPE_SECONDARY, NavDestination.NAV_TYPE_PRIMARY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MenuNavTypes {}

    // No instances. Static utilities only.
    private NavHelper() {
    }

    /**
     * Adds the {@link NavGraph#iterator() direct child destinations} of the given NavGraph
     * to the given Menu.
     * @param navGraph The NavGraph to {@link NavGraph#iterator() iterate} through, adding each
     *                 child destination.
     * @param menu The menu to add the children destinations to.
     * @param type The navigation type or types to filter the destinations. This must be one or
     *             more of {@link NavDestination#NAV_TYPE_PRIMARY} and
     *             {@link NavDestination#NAV_TYPE_SECONDARY}.
     */
    public static void addChildDestinationsToMenu(NavGraph navGraph, Menu menu,
            @MenuNavTypes int type) {
        for (NavDestination destination : navGraph) {
            if ((type & destination.getNavType()) != 0) {
                NavHelper.addDestinationToMenu(destination, menu);
            }
        }
    }

    /**
     * Adds a destination to the given Menu with the {@link NavDestination#getId() id} of the
     * destination, the {@link NavDestination#getLabel() label} as the title, and the icon set via
     * {@link NavDestination#setIcon(int)} or {@link NavDestination#setIcon(Drawable)}.
     *
     * <p>The {@link NavDestination#getNavType() navigation type} is used
     * as the groupId for the MenuItem. For {@link NavDestination#NAV_TYPE_SECONDARY secondary}
     * destinations, the order will be set to {@link Menu#CATEGORY_SECONDARY}.
     *
     * @param destination The NavDestination to add to the Menu.
     * @param menu The menu to add this destination to.
     */
    public static void addDestinationToMenu(NavDestination destination, Menu menu) {
        int order = destination.getNavType() == NavDestination.NAV_TYPE_SECONDARY
                ? Menu.CATEGORY_SECONDARY
                : Menu.NONE;
        addDestinationToMenu(destination, menu, destination.getNavType(), order);
    }

    /**
     * Adds a destination to the given Menu with the {@link NavDestination#getId() id} of the
     * destination, the {@link NavDestination#getLabel() label} as the title, and the icon set via
     * {@link NavDestination#setIcon(int)} or {@link NavDestination#setIcon(Drawable)}.
     *
     * @param destination The NavDestination to add to the Menu.
     * @param menu The menu to add this destination to.
     * @param groupId The {@link MenuItem#getGroupId() group identifier} of the added MenuItem
     * @param order The {@link MenuItem#getOrder() category and order within the category} of the
     *              added MenuItem
     */
    public static void addDestinationToMenu(NavDestination destination, Menu menu,
            int groupId, int order) {
        MenuItem item = menu.add(groupId, destination.getId(), order,
                destination.getLabel());
        if (destination.getIconDrawable() != null) {
            item.setIcon(destination.getIconDrawable());
        } else {
            item.setIcon(destination.getIconResourceId());
        }
    }

    /**
     * Attempt to navigate to the {@link NavDestination} associated with the given MenuItem. This
     * MenuItem should have been added via one of the helper methods in this class.
     *
     * <p>Importantly, it assumes the {@link MenuItem#getItemId() menu item id} matches the
     * {@link NavDestination#getId() destination id} to be navigated to.</p>
     *
     * @param navController The NavController that hosts the destination.
     * @param item The selected MenuItem.
     * @return True if the {@link NavController} was able to navigate to the destination
     * associated with the given MenuItem.
     */
    public static boolean handleMenuItemSelected(NavController navController,
            @NonNull MenuItem item) {
        return handleMenuItemSelected(navController, item, null);
    }

    /**
     * Attempt to navigate to the {@link NavDestination} associated with the given MenuItem. This
     * MenuItem should have been added via one of the helper methods in this class.
     *
     * <p>Importantly, it assumes the {@link MenuItem#getItemId() menu item id} matches the
     * {@link NavDestination#getId() destination id} to be navigated to.</p>
     *
     * @param navController The NavController that hosts the destination.
     * @param item The selected MenuItem.
     * @param drawerLayout The DrawerLayout that should be opened on home button presses if you
     *                     are on the topmost level of the app.
     * @return True if the {@link NavController} was able to navigate to the destination
     * associated with the given MenuItem.
     */
    public static boolean handleMenuItemSelected(NavController navController,
            @NonNull MenuItem item, DrawerLayout drawerLayout) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout != null && navController.getCurrentDestination().getId()
                    == navController.getGraph().getStartDestination()) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                navController.navigateUp();
            }
            return true;
        }
        try {
            navController.navigateTo(item.getItemId(), null,
                    new NavOptions.Builder()
                            .setPopUpTo(navController.getGraph().getStartDestination(), false)
                            .setLaunchSingleTop(true)
                            .setEnterAnim(R.anim.fade_in)
                            .setExitAnim(R.anim.fade_out)
                            .build());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
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
     * Call {@link #handleMenuItemSelected(NavController, MenuItem)}
     * to handle the Up button.
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
     * Call {@link #handleMenuItemSelected(NavController, MenuItem, DrawerLayout)}
     * to handle the Up button.
     *
     * @param navController The NavController that supplies the secondary menu. Navigation actions
     *                      on this NavController will be reflected in the title of the action bar.
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
     * Sets up a {@link NavigationView} for use with a {@link NavController}. This will add all
     * {@link NavDestination#NAV_TYPE_PRIMARY primary} and
     * {@link NavDestination#NAV_TYPE_SECONDARY secondary} destinations from the given NavController
     * to the NavigationView. The selected item in the NavigationView will automatically be
     * updated when the destination changes.
     *
     * @param navController The NavController that supplies the primary and secondary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the NavigationView.
     * @param navigationView The NavigationView that should be kept in sync with changes to the
     *                       NavController.
     */
    public static void setupNavigationView(final NavController navController,
            final NavigationView navigationView) {
        setupNavigationView(navController, navigationView, null);
    }

    /**
     * Sets up a {@link NavigationView} for use with a {@link NavController}. This will add all
     * {@link NavDestination#NAV_TYPE_PRIMARY primary} and
     * {@link NavDestination#NAV_TYPE_SECONDARY secondary} destinations from the given NavController
     * to the NavigationView. The selected item in the NavigationView will automatically be
     * updated when the destination changes.
     *
     * @param navController The NavController that supplies the primary and secondary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the NavigationView.
     * @param navigationView The NavigationView that should be kept in sync with changes to the
     *                       NavController.
     * @param listener The listener that should be called for other MenuItems in the NavigationView.
     */
    public static void setupNavigationView(final NavController navController,
            final NavigationView navigationView,
            final NavigationView.OnNavigationItemSelectedListener listener) {
        if (navigationView == null) {
            return;
        }
        addChildDestinationsToMenu(navController.getGraph(), navigationView.getMenu(),
                NavDestination.NAV_TYPE_PRIMARY | NavDestination.NAV_TYPE_SECONDARY);
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
                        return handled
                                || (listener != null && listener.onNavigationItemSelected(item));
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
     * Sets up a {@link BottomNavigationView} for use with a {@link NavController}. This will add
     * all {@link NavDestination#NAV_TYPE_PRIMARY primary} destinations from the given NavController
     * to the BottomNavigationView. The selected item in the BottomNavigationView will
     * automatically be updated when the destination changes.
     *
     * @param navController The NavController that supplies the primary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the BottomNavigationView.
     * @param bottomNavigationView The BottomNavigationView that should be kept in sync with
     *                             changes to the NavController.
     */
    public static void setupBottomNavigationView(final NavController navController,
            final BottomNavigationView bottomNavigationView) {
        setupBottomNavigationView(navController, bottomNavigationView, null);
    }

    /**
     * Sets up a {@link BottomNavigationView} for use with a {@link NavController}. This will add
     * all {@link NavDestination#NAV_TYPE_PRIMARY primary} destinations from the given NavController
     * to the BottomNavigationView. The selected item in the BottomNavigationView will
     * automatically be updated when the destination changes.
     *
     * @param navController The NavController that supplies the primary menu.
     *                      Navigation actions on this NavController will be reflected in the
     *                      selected item in the BottomNavigationView.
     * @param bottomNavigationView The BottomNavigationView that should be kept in sync with
     *                             changes to the NavController.
     * @param listener The listener that should be called for other MenuItems in the
     *                 BottomNavigationView.
     */
    public static void setupBottomNavigationView(final NavController navController,
            final BottomNavigationView bottomNavigationView,
            final BottomNavigationView.OnNavigationItemSelectedListener listener) {
        if (bottomNavigationView == null) {
            return;
        }
        addChildDestinationsToMenu(navController.getGraph(), bottomNavigationView.getMenu(),
                NavDestination.NAV_TYPE_PRIMARY);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return handleMenuItemSelected(navController, item)
                                || (listener != null && listener.onNavigationItemSelected(item));
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
