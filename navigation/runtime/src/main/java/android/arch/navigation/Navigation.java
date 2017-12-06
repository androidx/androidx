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

package android.arch.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewParent;

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
     * Retrieve a suitable display name for a given id.
     * @param context Context used to resolve a resource's name
     * @param id The id to get a display name for
     * @return The resource's name if it is a valid id or just the id itself if it is not
     * a valid resource
     */
    @NonNull
    static String getDisplayName(@NonNull Context context, int id) {
        try {
            return context.getResources().getResourceName(id);
        } catch (Resources.NotFoundException e) {
            return Integer.toString(id);
        }
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
    public static NavController findController(@Nullable Fragment fragment) {
        if (fragment == null) {
            return null;
        }

        Fragment findFragment = fragment;
        while (findFragment != null) {
            if (findFragment instanceof NavHostFragment) {
                return ((NavHostFragment) findFragment).getNavController();
            }
            Fragment primaryNavFragment = findFragment.getFragmentManager()
                    .getPrimaryNavigationFragment();
            if (primaryNavFragment instanceof NavHostFragment) {
                return ((NavHostFragment) primaryNavFragment).getNavController();
            }
            findFragment = findFragment.getParentFragment();
        }

        // Try looking for one associated with the view instead, if applicable
        return findController(fragment.getView());
    }

    /**
     * Find a {@link NavController} given the id of a View and its containing
     * {@link Activity}. This is a convenience wrapper around {@link #findController(View)}.
     *
     * <p>This method will locate the {@link NavController} associated with this view.
     * This is automatically populated for the id of a {@link NavHostFragment} and its children.</p>
     *
     * @param activity The Activity hosting the view
     * @param viewId The id of the view to search from
     * @return the {@link NavController} associated with the view referenced by id
     */
    @Nullable
    public static NavController findController(@NonNull Activity activity, @IdRes int viewId) {
        return Navigation.findController(activity.findViewById(viewId));
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
    @Nullable
    public static NavController findController(@Nullable View view) {
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
     * Create an {@link android.view.View.OnClickListener} for navigating
     * to a destination. This supports both navigating via an
     * {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to when the view is clicked
     * @return a new click listener for setting on an arbitrary view
     */
    @NonNull
    public static View.OnClickListener createNavigateOnClickListener(@IdRes final int resId) {
        return createNavigateOnClickListener(resId, null);
    }

    /**
     * Create an {@link android.view.View.OnClickListener} for navigating
     * to a destination. This supports both navigating via an
     * {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to when the view is clicked
     * @param args arguments to pass to the final destination
     * @return a new click listener for setting on an arbitrary view
     */
    @NonNull
    public static View.OnClickListener createNavigateOnClickListener(@IdRes final int resId,
            @Nullable final Bundle args) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NavController controller = findController(view);
                if (controller != null) {
                    controller.navigate(resId, args);
                } else {
                    throw new IllegalStateException(
                            "OnClickListener#onClick could not find NavController for view "
                            + view);
                }
            }
        };
    }

    /**
     * Associates a NavController with the given View, allowing developers to use
     * {@link #findController(View)} and {@link #findController(Activity, int)} with that View or
     * any of its children to retrieve the NavController.
     * <p>
     * This is generally called for you by the hosting component, such as a {@link NavHostFragment}.
     * @param view View that should be associated with the given NavController
     * @param controller The controller you wish to later retrieve via
     *                   {@link #findController(View)}
     */
    public static void setViewNavController(@NonNull View view,
            @Nullable NavController controller) {
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
