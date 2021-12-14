/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample.util

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.window.sample.R

@RequiresApi(Build.VERSION_CODES.O)
private object PictureInPictureLauncher {
    fun startPictureInPictureO(activity: Activity) {
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }
}

object PictureInPictureUtil {
    /**
     * Appends the start picture in picture [MenuItem] to the given [Menu] if picture in picture
     * is supported.
     * @param inflater a [MenuInflater] to load the [Menu] from a resource.
     * @param menu the menu to contain the inflated resource.
     */
    fun appendPictureInPictureMenu(inflater: MenuInflater, menu: Menu) {
        if (Build.VERSION.SDK_INT > 26) {
            inflater.inflate(R.menu.picture_in_picture_menu, menu)
        }
    }

    /**
     * Requests that the [Activity] enters picture in picture mode if the [MenuItem] matches the
     * resource loaded in [appendPictureInPictureMenu]. If the [Activity] does not support picture
     * in picture then nothing happens.
     * @return true if the [MenuItem] has the same id as the start pip [MenuItem], false otherwise
     */
    fun handlePictureInPictureMenuItem(activity: Activity, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_enter_pip -> {
                startPictureInPicture(activity)
                true
            }
            else -> false
        }
    }

    fun startPictureInPicture(activity: Activity) {
        if (Build.VERSION.SDK_INT > 26) {
            PictureInPictureLauncher.startPictureInPictureO(activity)
        } else {
            Toast.makeText(activity, "PiP not supported", Toast.LENGTH_LONG).show()
        }
    }
}