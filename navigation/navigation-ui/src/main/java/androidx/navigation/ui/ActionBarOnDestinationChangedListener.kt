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
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

/**
 * The OnDestinationChangedListener specifically for keeping the ActionBar updated.
 * This handles both updating the title and updating the Up Indicator, transitioning between
 * the drawer icon and up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ActionBarOnDestinationChangedListener(
    private val activity: AppCompatActivity,
    configuration: AppBarConfiguration
) : AbstractAppBarOnDestinationChangedListener(
    checkNotNull(activity.drawerToggleDelegate) {
        "Activity $activity does not have an DrawerToggleDelegate set"
    }.actionBarThemedContext,
    configuration
) {
    override fun setTitle(title: CharSequence?) {
        val actionBar = checkNotNull(activity.supportActionBar) {
            "Activity $activity does not have an ActionBar set via setSupportActionBar()"
        }
        actionBar.title = title
    }

    override fun setNavigationIcon(icon: Drawable?, @StringRes contentDescription: Int) {
        val actionBar = checkNotNull(activity.supportActionBar) {
            "Activity $activity does not have an ActionBar set via setSupportActionBar()"
        }
        actionBar.setDisplayHomeAsUpEnabled(icon != null)
        val delegate = checkNotNull(activity.drawerToggleDelegate) {
            "Activity $activity does not have an DrawerToggleDelegate set"
        }
        delegate.setActionBarUpIndicator(icon, contentDescription)
    }
}
