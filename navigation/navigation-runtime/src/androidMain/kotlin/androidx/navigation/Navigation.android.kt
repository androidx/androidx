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
package androidx.navigation

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference

/**
 * Entry point for navigation operations.
 *
 * This class provides utilities for finding a relevant [NavController] instance from
 * various common places in your application, or for performing navigation in response to
 * UI events.
 */
public object Navigation {
    /**
     * Find a [NavController] given the id of a View and its containing
     * [Activity]. This is a convenience wrapper around [findNavController].
     *
     * This method will locate the [NavController] associated with this view.
     * This is automatically populated for the id of a [NavHost] and its children.
     *
     * @param activity The Activity hosting the view
     * @param viewId The id of the view to search from
     * @return the [NavController] associated with the view referenced by id
     * @throws IllegalStateException if the given viewId does not correspond with a
     * [NavHost] or is not within a NavHost.
     */
    @JvmStatic
    public fun findNavController(activity: Activity, @IdRes viewId: Int): NavController {
        val view = ActivityCompat.requireViewById<View>(activity, viewId)
        return findViewNavController(view)
            ?: throw IllegalStateException(
                "Activity $activity does not have a NavController set on $viewId"
            )
    }

    /**
     * Find a [NavController] given a local [View].
     *
     * This method will locate the [NavController] associated with this view.
     * This is automatically populated for views that are managed by a [NavHost]
     * and is intended for use by various [listener][android.view.View.OnClickListener]
     * interfaces.
     *
     * @param view the view to search from
     * @return the locally scoped [NavController] to the given view
     * @throws IllegalStateException if the given view does not correspond with a
     * [NavHost] or is not within a NavHost.
     */
    @JvmStatic
    public fun findNavController(view: View): NavController {
        return findViewNavController(view)
            ?: throw IllegalStateException("View $view does not have a NavController set")
    }

    /**
     * Create an [android.view.View.OnClickListener] for navigating
     * to a destination. This supports both navigating via an
     * [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to when the view is clicked
     * @param args arguments to pass to the final destination
     * @return a new click listener for setting on an arbitrary view
     */
    @JvmStatic
    @JvmOverloads
    public fun createNavigateOnClickListener(
        @IdRes resId: Int,
        args: Bundle? = null
    ): View.OnClickListener {
        return View.OnClickListener { view -> findNavController(view).navigate(resId, args) }
    }

    /**
     * Create an [android.view.View.OnClickListener] for navigating
     * to a destination via a generated [NavDirections].
     *
     * @param directions directions that describe this navigation operation
     * @return a new click listener for setting on an arbitrary view
     */
    @JvmStatic
    public fun createNavigateOnClickListener(directions: NavDirections): View.OnClickListener {
        return View.OnClickListener { view -> findNavController(view).navigate(directions) }
    }

    /**
     * Associates a NavController with the given View, allowing developers to use
     * [findNavController] and [findNavController] with that
     * View or any of its children to retrieve the NavController.
     *
     * This is generally called for you by the hosting [NavHost].
     * @param view View that should be associated with the given NavController
     * @param controller The controller you wish to later retrieve via
     * [findNavController]
     */
    @JvmStatic
    public fun setViewNavController(view: View, controller: NavController?) {
        view.setTag(R.id.nav_controller_view_tag, controller)
    }

    /**
     * Recurse up the view hierarchy, looking for the NavController
     * @param view the view to search from
     * @return the locally scoped [NavController] to the given view, if found
     */
    private fun findViewNavController(view: View): NavController? =
        generateSequence(view) {
            it.parent as? View?
        }.mapNotNull {
            getViewNavController(it)
        }.firstOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun getViewNavController(view: View): NavController? {
        val tag = view.getTag(R.id.nav_controller_view_tag)
        var controller: NavController? = null
        if (tag is WeakReference<*>) {
            controller = (tag as WeakReference<NavController>).get()
        } else if (tag is NavController) {
            controller = tag
        }
        return controller
    }
}
