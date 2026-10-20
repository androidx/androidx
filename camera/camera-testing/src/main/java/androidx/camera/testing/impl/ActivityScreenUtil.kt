/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("ActivityScreenUtil")

package androidx.camera.testing.impl

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi

/**
 * Makes this [Activity] show over the keyguard, turn the screen on and keep the screen on, so that
 * tests can bring it to the RESUMED state even if the device is locked or the screen is off.
 *
 * Should be called from [Activity.onCreate] (or before the activity becomes visible).
 *
 * @param dismissKeyguard whether to also request dismissing a non-secure keyguard.
 */
@MainThread
@JvmOverloads
public fun Activity.turnScreenOnAndShowWhenLocked(dismissKeyguard: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        Api27Impl.setShowWhenLockedAndTurnScreenOn(this)
    } else {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    if (dismissKeyguard) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26Impl.requestDismissKeyguard(this)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private object Api27Impl {
    fun setShowWhenLockedAndTurnScreenOn(activity: Activity) {
        activity.setShowWhenLocked(true)
        activity.setTurnScreenOn(true)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object Api26Impl {
    fun requestDismissKeyguard(activity: Activity) {
        activity
            .getSystemService(KeyguardManager::class.java)
            ?.requestDismissKeyguard(activity, null)
    }
}
