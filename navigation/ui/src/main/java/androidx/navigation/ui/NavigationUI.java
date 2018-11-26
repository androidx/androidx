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

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Class which hooks up elements typically in the 'chrome' of your application such as global
 * navigation patterns like a navigation drawer or bottom nav bar with your {@link NavController}.
 */
public final class NavigationUI {

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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean onNavDestinationSelected(@NonNull MenuItem item,
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
     * @deprecated Use {@link #navigateUp(NavController, DrawerLayout)} or
     * {@link #navigateUp(NavController, AppBarConfiguration)}.
     */
    @Deprecated
    public static boolean navigateUp(@Nullable DrawerLayout drawerLayout,
            @NonNull NavController navController) {
        return navigateUp(navController, new AppBarConfiguration.Builder(navController.getGraph())
                .setDrawerLayout(drawerLayout)
                .build());
    }

    /**
     * Handles the Up button by delegating its behavior to the given NavController. This should
     * generally be called from {@link AppCompatActivity#onSupportNavigateUp()}.
     * <p>If you do not have a {@link DrawerLayout}, you should call
     * {@link NavController#navigateUp()} directly.
     *
     * @param navController The NavController that hosts your content.
     * @param drawerLayout The DrawerLayout that should be opened if you are on the topmost level
     *                     of the app.
     * @return True if the {@link NavController} was able to navigate up.
     */
    public static boolean navigateUp(@NonNull NavController navController,
            @Nullable DrawerLayout drawerLayout) {
        return navigateUp(navController, new AppBarConfiguration.Builder(navController.getGraph())
                .setDrawerLayout(drawerLayout)
                .build());
    }

    /**
     * Handles the Up button by delegating its behavior to the given NavController. This is
     * an alternative to using {@link NavController#navigateUp()} directly when the given
     * {@link AppBarConfiguration} needs to be considered when determining what should happen
     * when the Up button is pressed.
     *
     * @param navController The NavController that hosts your content.
     * @param configuration Additional configuration options for determining what should happen
     *                      when the Up button is pressed.
     * @return True if the {@link NavController} was able to navigate up.
     */
    public static boolean navigateUp(@NonNull NavController navController,
            @NonNull AppBarConfiguration configuration) {
        DrawerLayout drawerLayout = configuration.getDrawerLayout();
        NavDestination currentDestination = navController.getCurrentDestination();
        Set<Integer> topLevelDestinations = configuration.getTopLevelDestinations();
        if (drawerLayout != null && currentDestination != null
                && matchDestinations(currentDestination, topLevelDestinations)) {
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
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On all other destinations, the ActionBar will show the Up button.
     * Call {@link NavController#navigateUp()} to handle the Up button.
     *
     * @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param navController The NavController that supplies the secondary menu. Navigation actions
     *                      on this NavController will be reflected in the title of the action bar.
     * @see #setupActionBarWithNavController(AppCompatActivity, NavController, AppBarConfiguration)
     */
    public static void setupActionBarWithNavController(@NonNull AppCompatActivity activity,
            @NonNull NavController navController) {
        setupActionBarWithNavController(activity, navController,
                new AppBarConfiguration.Builder(navController.getGraph())
                        .build());
    }

    /**
     * Sets up the ActionBar returned by {@link AppCompatActivity#getSupportActionBar()} for use
     * with a {@link NavController}.
     *
     * <p>By calling this method, the title in the action bar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On the start destination of your navigation graph, the ActionBar will show
     * the drawer icon if the given DrawerLayout is non null. On all other destinations,
     * the ActionBar will show the Up button.
     * Call {@link #navigateUp(NavController, DrawerLayout)} to handle the Up button.
     * @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the action bar.
     * @param drawerLayout The DrawerLayout that should be toggled from the home button
     * @see #setupActionBarWithNavController(AppCompatActivity, NavController, AppBarConfiguration)
     */
    public static void setupActionBarWithNavController(@NonNull AppCompatActivity activity,
            @NonNull NavController navController,
            @Nullable DrawerLayout drawerLayout) {
        navController.addOnNavigatedListener(
                new ActionBarOnNavigatedListener(activity,
                        new AppBarConfiguration.Builder(navController.getGraph())
                                .setDrawerLayout(drawerLayout)
                                .build()));
    }

    /**
     * Sets up the ActionBar returned by {@link AppCompatActivity#getSupportActionBar()} for use
     * with a {@link NavController}.
     *
     * <p>By calling this method, the title in the action bar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The {@link AppBarConfiguration} you provide controls how the Navigation button is
     * displayed.
     * Call {@link #navigateUp(NavController, AppBarConfiguration)} to handle the Up button.
     *  @param activity The activity hosting the action bar that should be kept in sync with changes
     *                 to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the action bar.
     * @param configuration Additional configuration options for customizing the behavior of the
     *                      ActionBar
     */
    public static void setupActionBarWithNavController(@NonNull AppCompatActivity activity,
            @NonNull NavController navController,
            @NonNull AppBarConfiguration configuration) {
        navController.addOnNavigatedListener(
                new ActionBarOnNavigatedListener(activity, configuration));
    }

    /**
     * Sets up a {@link Toolbar} for use with a {@link NavController}.
     *
     * <p>By calling this method, the title in the Toolbar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On all other destinations, the Toolbar will show the Up button. This
     * method will call {@link NavController#navigateUp()} when the Navigation button
     * is clicked.
     *
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController that supplies the secondary menu. Navigation actions
     *                      on this NavController will be reflected in the title of the Toolbar.
     * @see #setupWithNavController(Toolbar, NavController, AppBarConfiguration)
     */
    public static void setupWithNavController(@NonNull Toolbar toolbar,
            @NonNull NavController navController) {
        setupWithNavController(toolbar, navController,
                new AppBarConfiguration.Builder(navController.getGraph()).build());
    }

    /**
     * Sets up a {@link Toolbar} for use with a {@link NavController}.
     *
     * <p>By calling this method, the title in the Toolbar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On the start destination of your navigation graph, the Toolbar will show
     * the drawer icon if the given DrawerLayout is non null. On all other destinations,
     * the Toolbar will show the Up button. This method will call
     * {@link #navigateUp(NavController, DrawerLayout)} when the Navigation button is clicked.
     *
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the Toolbar.
     * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
     * @see #setupWithNavController(Toolbar, NavController, AppBarConfiguration)
     */
    public static void setupWithNavController(@NonNull Toolbar toolbar,
            @NonNull final NavController navController,
            @Nullable final DrawerLayout drawerLayout) {
        setupWithNavController(toolbar, navController,
                new AppBarConfiguration.Builder(navController.getGraph())
                        .setDrawerLayout(drawerLayout)
                        .build());
    }

    /**
     * Sets up a {@link Toolbar} for use with a {@link NavController}.
     *
     * <p>By calling this method, the title in the Toolbar will automatically be updated when
     * the destination changes (assuming there is a valid {@link NavDestination#getLabel label}).
     *
     * <p>The {@link AppBarConfiguration} you provide controls how the Navigation button is
     * displayed and what action is triggered when the Navigation button is tapped. This method
     * will call {@link #navigateUp(NavController, AppBarConfiguration)} when the Navigation button
     * is clicked.
     *
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the Toolbar.
     * @param configuration Additional configuration options for customizing the behavior of the
     *                      Toolbar
     */
    public static void setupWithNavController(@NonNull Toolbar toolbar,
            @NonNull final NavController navController,
            @NonNull final AppBarConfiguration configuration) {
        navController.addOnNavigatedListener(
                new ToolbarOnNavigatedListener(toolbar, configuration));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateUp(navController, configuration);
            }
        });
    }

    /**
     * Sets up a {@link CollapsingToolbarLayout} and {@link Toolbar} for use with a
     * {@link NavController}.
     *
     * <p>By calling this method, the title in the CollapsingToolbarLayout will automatically be
     * updated when the destination changes (assuming there is a valid
     * {@link NavDestination#getLabel label}).
     *
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On all other destinations, the Toolbar will show the Up button. This
     * method will call {@link NavController#navigateUp()} when the Navigation button
     * is clicked.
     *
     * @param collapsingToolbarLayout The CollapsingToolbarLayout that should be kept in sync with
     *                                changes to the NavController.
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController that supplies the secondary menu. Navigation actions
     *                      on this NavController will be reflected in the title of the Toolbar.
     */
    public static void setupWithNavController(
            @NonNull CollapsingToolbarLayout collapsingToolbarLayout,
            @NonNull Toolbar toolbar,
            @NonNull NavController navController) {
        setupWithNavController(collapsingToolbarLayout, toolbar, navController,
                new AppBarConfiguration.Builder(navController.getGraph()).build());
    }

    /**
     * Sets up a {@link CollapsingToolbarLayout} and {@link Toolbar} for use with a
     * {@link NavController}.
     *
     * <p>By calling this method, the title in the CollapsingToolbarLayout will automatically be
     * updated when the destination changes (assuming there is a valid
     * {@link NavDestination#getLabel label}).
     *
     * <p>The start destination of your navigation graph is considered the only top level
     * destination. On the start destination of your navigation graph, the Toolbar will show
     * the drawer icon if the given DrawerLayout is non null. On all other destinations,
     * the Toolbar will show the Up button. This method will call
     * {@link #navigateUp(NavController, DrawerLayout)} when the Navigation button is clicked.
     *
     * @param collapsingToolbarLayout The CollapsingToolbarLayout that should be kept in sync with
     *                                changes to the NavController.
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the Toolbar.
     * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
     */
    public static void setupWithNavController(
            @NonNull CollapsingToolbarLayout collapsingToolbarLayout,
            @NonNull Toolbar toolbar,
            @NonNull final NavController navController,
            @Nullable final DrawerLayout drawerLayout) {
        setupWithNavController(collapsingToolbarLayout, toolbar, navController,
                new AppBarConfiguration.Builder(navController.getGraph())
                        .setDrawerLayout(drawerLayout)
                        .build());
    }

    /**
     * Sets up a {@link CollapsingToolbarLayout} and {@link Toolbar} for use with a
     * {@link NavController}.
     *
     * <p>By calling this method, the title in the CollapsingToolbarLayout will automatically be
     * updated when the destination changes (assuming there is a valid
     * {@link NavDestination#getLabel label}).
     *
     * <p>The {@link AppBarConfiguration} you provide controls how the Navigation button is
     * displayed and what action is triggered when the Navigation button is tapped. This method
     * will call {@link #navigateUp(NavController, AppBarConfiguration)} when the Navigation button
     * is clicked.
     *
     * @param collapsingToolbarLayout The CollapsingToolbarLayout that should be kept in sync with
     *                                changes to the NavController.
     * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
     * @param navController The NavController whose navigation actions will be reflected
     *                      in the title of the Toolbar.
     * @param configuration Additional configuration options for customizing the behavior of the
     *                      Toolbar
     */
    public static void setupWithNavController(
            @NonNull CollapsingToolbarLayout collapsingToolbarLayout,
            @NonNull Toolbar toolbar,
            @NonNull final NavController navController,
            @NonNull final AppBarConfiguration configuration) {
        navController.addOnNavigatedListener(new CollapsingToolbarOnNavigatedListener(
                collapsingToolbarLayout, toolbar, configuration));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateUp(navController, configuration);
            }
        });
    }

    /**
     * Sets up a {@link NavigationView} for use with a {@link NavController}. This will call
     * {@link #onNavDestinationSelected(MenuItem, NavController)} when a menu item is selected.
     * The selected item in the NavigationView will automatically be updated when the destination
     * changes.
     * <p>
     * If the {@link NavigationView} is directly contained with a {@link DrawerLayout},
     * the drawer will be closed when a menu item is selected.
     * <p>
     * Similarly, if the {@link NavigationView} has a {@link BottomSheetBehavior} associated with
     * it (as is the case when using a {@link android.support.design.widget.BottomSheetDialog}),
     * the bottom sheet will be hidden when a menu item is selected.
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
                            } else {
                                BottomSheetBehavior bottomSheetBehavior =
                                        findBottomSheetBehavior(navigationView);
                                if (bottomSheetBehavior != null) {
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                }
                            }
                        }
                        return handled;
                    }
                });
        final WeakReference<NavigationView> weakReference = new WeakReference<>(navigationView);
        navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
            @Override
            public void onNavigated(@NonNull NavController controller,
                    @NonNull NavDestination destination) {
                NavigationView view = weakReference.get();
                if (view == null) {
                    controller.removeOnNavigatedListener(this);
                    return;
                }
                Menu menu = view.getMenu();
                for (int h = 0, size = menu.size(); h < size; h++) {
                    MenuItem item = menu.getItem(h);
                    item.setChecked(matchDestination(destination, item.getItemId()));
                }
            }
        });
    }

    /**
     * Walks up the view hierarchy, trying to determine if the given View is contained within
     * a bottom sheet.
     */
    @SuppressWarnings("WeakerAccess")
    static BottomSheetBehavior findBottomSheetBehavior(@NonNull View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                return findBottomSheetBehavior((View) parent);
            }
            return null;
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof BottomSheetBehavior)) {
            // We hit a CoordinatorLayout, but the View doesn't have the BottomSheetBehavior
            return null;
        }
        return (BottomSheetBehavior) behavior;
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
        final WeakReference<BottomNavigationView> weakReference =
                new WeakReference<>(bottomNavigationView);
        navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
            @Override
            public void onNavigated(@NonNull NavController controller,
                    @NonNull NavDestination destination) {
                BottomNavigationView view = weakReference.get();
                if (view == null) {
                    controller.removeOnNavigatedListener(this);
                    return;
                }
                Menu menu = view.getMenu();
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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean matchDestination(@NonNull NavDestination destination,
            @IdRes int destId) {
        NavDestination currentDestination = destination;
        while (currentDestination.getId() != destId && currentDestination.getParent() != null) {
            currentDestination = currentDestination.getParent();
        }
        return currentDestination.getId() == destId;
    }

    /**
     * Determines whether the given <code>destinationIds</code> match the NavDestination. This
     * handles both the default case (the destination's id is in the given ids) and the nested
     * case where the given ids is a parent/grandparent/etc of the destination.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean matchDestinations(@NonNull NavDestination destination,
            @NonNull Set<Integer> destinationIds) {
        NavDestination currentDestination = destination;
        do {
            if (destinationIds.contains(currentDestination.getId())) {
                return true;
            }
            currentDestination = currentDestination.getParent();
        } while (currentDestination != null);
        return false;
    }

    /**
     * Finds the actual start destination of the graph, handling cases where the graph's starting
     * destination is itself a NavGraph.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static NavDestination findStartDestination(@NonNull NavGraph graph) {
        NavDestination startDestination = graph;
        while (startDestination instanceof NavGraph) {
            NavGraph parent = (NavGraph) startDestination;
            startDestination = parent.findNode(parent.getStartDestination());
        }
        return startDestination;
    }
}
