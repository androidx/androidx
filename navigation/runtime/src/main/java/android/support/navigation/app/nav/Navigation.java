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

package android.support.navigation.app.nav;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewParent;

import com.android.support.navigation.R;

import java.lang.ref.WeakReference;

/**
 * Entry point for navigation operations.
 *
 * <p>This class provides utilities for finding a relevant {@link NavController} instance from
 * various common places in your application, or for performing navigation in response to
 * UI events.</p>
 */
public class Navigation {
    // No instances. Static utilities only.
    private Navigation() {
    }

    /**
     * Find a {@link NavController} given a local {@link Fragment}.
     *
     * <p>This method will locate the {@link NavController} associated with this Fragment,
     * looking first for a {@link NavHostFragment} along the given Fragment's parent chain.
     * If a {@link NavController} is not found, this method will look for one along this
     * Fragment's {@link Fragment#getView() view hierarchy} as specified by
     * {@link #findController(View)}.</p>
     *
     * @param fragment the locally scoped Fragment for navigation
     * @return the locally scoped {@link NavController} for navigating from this {@link Fragment}
     */
    public static NavController findController(Fragment fragment) {
        if (fragment == null) {
            return null;
        }

        Fragment findFragment = fragment;
        while (findFragment != null) {
            if (findFragment instanceof NavHostFragment) {
                return ((NavHostFragment) findFragment).getNavController();
            }
            findFragment = findFragment.getParentFragment();
        }

        // Try looking for one associated with the view instead, if applicable
        return findController(fragment.getView());
    }

    /**
     * Find a {@link NavController} given a local {@link View}.
     *
     * <p>This method will locate the {@link NavController} associated with this view.
     * This is automatically populated for views that are managed by a {@link NavHostFragment}
     * and is intended for use by various {@link android.view.View.OnClickListener listener}
     * interfaces.</p>
     *
     * @param view the view to search from
     * @return the locally scoped {@link NavController} to the given view
     */
    public static NavController findController(View view) {
        if (view == null) {
            return null;
        }
        while (view != null) {
            NavController controller = getViewNavController(view);
            if (controller != null) {
                return controller;
            }
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    /**
     * Create an {@link android.view.View.OnClickListener} for navigating via an action.
     *
     * @param actionId navigation action to take when the view is clicked
     * @return a new click listener for setting on an arbitrary view
     */
    public static View.OnClickListener createNavigateOnClickListener(@IdRes final int actionId) {
        return createNavigateOnClickListener(actionId, null);
    }

    /**
     * Create an {@link android.view.View.OnClickListener} for navigating via an action.
     *
     * @param actionId navigation action to take when the view is clicked
     * @param args arguments to pass to the final destination
     * @return a new click listener for setting on an arbitrary view
     */
    public static View.OnClickListener createNavigateOnClickListener(@IdRes final int actionId,
                                                                     final Bundle args) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NavController controller = findController(view);
                if (controller != null) {
                    controller.navigate(actionId, args);
                } else {
                    throw new IllegalStateException(
                            "OnClickListener#onClick could not find NavController for view "
                            + view);
                }
            }
        };
    }

    /**
     * Create an {@link android.view.View.OnClickListener} for navigating
     * to an explicit destination.
     *
     * @param destId destination to navigate to when the view is clicked
     * @return a new click listener for setting on an arbitrary view
     */
    public static View.OnClickListener createNavigateToOnClickListener(@IdRes final int destId) {
        return createNavigateToOnClickListener(destId, null);
    }

    /**
     * Create an {@link android.view.View.OnClickListener} for navigating
     * to an explicit destination.
     *
     * @param destId destination to navigate to when the view is clicked
     * @param args arguments to pass to the final destination
     * @return a new click listener for setting on an arbitrary view
     */
    public static View.OnClickListener createNavigateToOnClickListener(@IdRes final int destId,
                                                                       final Bundle args) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NavController controller = findController(view);
                if (controller != null) {
                    controller.navigateTo(destId, args);
                } else {
                    throw new IllegalStateException(
                            "OnClickListener#onClick could not find NavController for view "
                            + view);
                }
            }
        };
    }

    static void setViewNavController(View view, NavController controller) {
        view.setTag(R.id.nav_controller_view_tag, controller);
    }

    static NavController getViewNavController(View view) {
        Object tag = view.getTag(R.id.nav_controller_view_tag);
        NavController controller = null;
        if (tag instanceof WeakReference) {
            controller = ((WeakReference<NavController>) tag).get();
        } else if (tag instanceof NavController) {
            controller = (NavController) tag;
        }
        return controller;
    }
}
