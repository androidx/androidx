/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.navigation.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.ui.NavigationUI.matchDestinations
import java.lang.ref.WeakReference
import java.util.regex.Pattern

/**
 * The abstract OnDestinationChangedListener for keeping any type of app bar updated.
 * This handles both updating the title and updating the Up Indicator, transitioning between
 * the drawer icon and up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractAppBarOnDestinationChangedListener(
    private val context: Context,
    configuration: AppBarConfiguration
) : NavController.OnDestinationChangedListener {
    private val topLevelDestinations: Set<Int> = configuration.topLevelDestinations
    private val openableLayoutWeakReference = configuration.openableLayout?.run {
        WeakReference(this)
    }
    private var arrowDrawable: DrawerArrowDrawable? = null
    private var animator: ValueAnimator? = null

    protected abstract fun setTitle(title: CharSequence?)

    protected abstract fun setNavigationIcon(icon: Drawable?, @StringRes contentDescription: Int)

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (destination is FloatingWindow) {
            return
        }
        val openableLayout = openableLayoutWeakReference?.get()
        if (openableLayoutWeakReference != null && openableLayout == null) {
            controller.removeOnDestinationChangedListener(this)
            return
        }
        val label = destination.label
        if (label != null) {
            // Fill in the data pattern with the args to build a valid URI
            val title = StringBuffer()
            val fillInPattern = Pattern.compile("\\{(.+?)\\}")
            val matcher = fillInPattern.matcher(label)
            while (matcher.find()) {
                val argName = matcher.group(1)
                if (arguments != null && arguments.containsKey(argName)) {
                    matcher.appendReplacement(title, "")
                    title.append(arguments[argName].toString())
                } else {
                    throw IllegalArgumentException(
                        "Could not find $argName in $arguments to fill label $label"
                    )
                }
            }
            matcher.appendTail(title)
            setTitle(title)
        }
        val isTopLevelDestination = destination.matchDestinations(topLevelDestinations)
        if (openableLayout == null && isTopLevelDestination) {
            setNavigationIcon(null, 0)
        } else {
            setActionBarUpIndicator(openableLayout != null && isTopLevelDestination)
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun setActionBarUpIndicator(showAsDrawerIndicator: Boolean) {
        val (arrow, animate) = arrowDrawable?.run {
            this to true
        } ?: DrawerArrowDrawable(context).also { arrowDrawable = it } to false

        setNavigationIcon(
            arrow,
            if (showAsDrawerIndicator) R.string.nav_app_bar_open_drawer_description
            else R.string.nav_app_bar_navigate_up_description
        )

        val endValue = if (showAsDrawerIndicator) 0f else 1f
        if (animate) {
            val startValue = arrow.progress
            animator?.cancel()
            animator = ObjectAnimator.ofFloat(arrow, "progress", startValue, endValue)
            (animator as ObjectAnimator).start()
        } else {
            arrow.progress = endValue
        }
    }
}
