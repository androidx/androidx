/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.demo.common.util

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.window.demo.common.R

@RequiresApi(Build.VERSION_CODES.O)
private object PictureInPictureLauncherO {
    fun startPictureInPicture(activity: Activity) {
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }

    fun setPictureInPictureParams(activity: Activity) {
        activity.setPictureInPictureParams(PictureInPictureParams.Builder().build())
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object PictureInPictureLauncherS {
    fun startPictureInPicture(activity: Activity, autoEnterPip: Boolean = false) {
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder()
            .setAutoEnterEnabled(autoEnterPip)
            .build())
    }

    fun setPictureInPictureParams(activity: Activity, autoEnterPip: Boolean = false) {
        activity.setPictureInPictureParams(PictureInPictureParams.Builder()
            .setAutoEnterEnabled(autoEnterPip)
            .build())
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

    fun startPictureInPicture(activity: Activity, autoEnterPip: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PictureInPictureLauncherS.startPictureInPicture(activity, autoEnterPip)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureLauncherO.startPictureInPicture(activity)
        } else {
            Toast.makeText(activity, "PiP not supported", Toast.LENGTH_LONG).show()
        }
    }

    fun setPictureInPictureParams(activity: Activity, autoEnterPip: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PictureInPictureLauncherS.setPictureInPictureParams(activity, autoEnterPip)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureLauncherO.setPictureInPictureParams(activity)
            if (autoEnterPip) {
                Toast.makeText(activity, "Auto enter PIP not supported", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            Toast.makeText(activity, "PiP not supported", Toast.LENGTH_LONG).show()
        }
    }
}
