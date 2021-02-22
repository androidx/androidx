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

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.transition.TransitionManager
import java.lang.ref.WeakReference

/**
 * The OnDestinationChangedListener specifically for keeping a Toolbar updated.
 * This handles both updating the title and updating the Up Indicator, transitioning between
 * the drawer icon and up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ToolbarOnDestinationChangedListener(
    toolbar: Toolbar,
    configuration: AppBarConfiguration
) : AbstractAppBarOnDestinationChangedListener(toolbar.context, configuration) {
    private val toolbarWeakReference: WeakReference<Toolbar> = WeakReference(toolbar)

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val toolbar = toolbarWeakReference.get()
        if (toolbar == null) {
            controller.removeOnDestinationChangedListener(this)
            return
        }
        super.onDestinationChanged(controller, destination, arguments)
    }

    override fun setTitle(title: CharSequence?) {
        toolbarWeakReference.get()?.let { toolbar ->
            toolbar.title = title
        }
    }

    override fun setNavigationIcon(icon: Drawable?, @StringRes contentDescription: Int) {
        toolbarWeakReference.get()?.run {
            val useTransition = icon == null && navigationIcon != null
            navigationIcon = icon
            setNavigationContentDescription(contentDescription)
            if (useTransition) {
                TransitionManager.beginDelayedTransition(this)
            }
        }
    }
}
