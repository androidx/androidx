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

package androidx.navigation;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
     * Find a {@link NavController} given the id of a View and its containing
     * {@link Activity}. This is a convenience wrapper around {@link #findNavController(View)}.
     *
     * <p>This method will locate the {@link NavController} associated with this view.
     * This is automatically populated for the id of a {@link NavHost} and its children.</p>
     *
     * @param activity The Activity hosting the view
     * @param viewId The id of the view to search from
     * @return the {@link NavController} associated with the view referenced by id
     * @throws IllegalStateException if the given viewId does not correspond with a
     * {@link NavHost} or is not within a NavHost.
     */
    @NonNull
    public static NavController findNavController(@NonNull Activity activity, @IdRes int viewId) {
        View view = ActivityCompat.requireViewById(activity, viewId);
        NavController navController = findViewNavController(view);
        if (navController == null) {
            throw new IllegalStateException("Activity " + activity
                    + " does not have a NavController set on " + viewId);
        }
        return navController;
    }

    /**
     * Find a {@link NavController} given a local {@link View}.
     *
     * <p>This method will locate the {@link NavController} associated with this view.
     * This is automatically populated for views that are managed by a {@link NavHost}
     * and is intended for use by various {@link android.view.View.OnClickListener listener}
     * interfaces.</p>
     *
     * @param view the view to search from
     * @return the locally scoped {@link NavController} to the given view
     * @throws IllegalStateException if the given view does not correspond with a
     * {@link NavHost} or is not within a NavHost.
     */
    @NonNull
    public static NavController findNavController(@NonNull View view) {
        NavController navController = findViewNavController(view);
        if (navController == null) {
            throw new IllegalStateException("View " + view + " does not have a NavController set");
        }
        return navController;
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
                findNavController(view).navigate(resId, args);
            }
        };
    }

    /**
     * Associates a NavController with the given View, allowing developers to use
     * {@link #findNavController(View)} and {@link #findNavController(Activity, int)} with that
     * View or any of its children to retrieve the NavController.
     * <p>
     * This is generally called for you by the hosting {@link NavHost}.
     * @param view View that should be associated with the given NavController
     * @param controller The controller you wish to later retrieve via
     *                   {@link #findNavController(View)}
     */
    public static void setViewNavController(@NonNull View view,
            @Nullable NavController controller) {
        view.setTag(R.id.nav_controller_view_tag, controller);
    }

    /**
     * Recurse up the view hierarchy, looking for the NavController
     * @param view the view to search from
     * @return the locally scoped {@link NavController} to the given view, if found
     */
    @Nullable
    private static NavController findViewNavController(@NonNull View view) {
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

    @SuppressWarnings("unchecked")
    @Nullable
    private static NavController getViewNavController(@NonNull View view) {
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
