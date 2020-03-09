/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window.sample.backend

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.graphics.Rect
import androidx.core.util.Consumer
import androidx.window.DeviceState
import androidx.window.DisplayFeature
import androidx.window.DisplayFeature.TYPE_FOLD
import androidx.window.WindowBackend
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor

/**
 * Sample backend implementation that reports a {@link DisplayFeature#TYPE_FOLD} display feature
 * that goes across the screen in the middle. It can be used as a mock backend implementation
 * with [WindowManager] for automated or manual testing.
 * <p> If the application window is portrait-oriented, then the reported fold in the screen will
 * be horizontal (vertically-folding device configuration). If the window is landscape-oriented,
 * then the reported fold will be vertical (horizontally-folding device configuration).
 * @see [WindowManager]
 * @see [getWindowLayoutInfo]
 */
class MidScreenFoldBackend : WindowBackend {
    override fun getDeviceState(): DeviceState {
        return DeviceState.Builder().setPosture(DeviceState.POSTURE_OPENED).build()
    }

    override fun getWindowLayoutInfo(context: Context): WindowLayoutInfo {
        val activity = getActivityFromContext(context) ?: throw IllegalArgumentException(
            "Used non-visual Context used with WindowManager. Please use an Activity or a " +
                    "ContextWrapper around an Activity instead.")
        val displaySize = Point(activity.window.decorView.width, activity.window.decorView.height)
        val featureRect = if (displaySize.x >= displaySize.y) { // Landscape
            Rect(displaySize.x / 2, 0, displaySize.x / 2, displaySize.y)
        } else { // Portrait
            Rect(0, displaySize.y / 2, displaySize.x, displaySize.y / 2)
        }

        val displayFeature = DisplayFeature.Builder()
            .setBounds(featureRect)
            .setType(TYPE_FOLD)
            .build()
        val featureList = ArrayList<DisplayFeature>()
        featureList.add(displayFeature)
        return WindowLayoutInfo.Builder().setDisplayFeatures(featureList).build()
    }

    /**
     * Unwraps the hierarchy of [ContextWrapper]-s until [Activity] is reached.
     * @return Base [Activity] context or `null` if not available.
     */
    private fun getActivityFromContext(c: Context?): Activity? {
        var context = c
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    override fun registerDeviceStateChangeCallback(
        executor: Executor,
        callback: Consumer<DeviceState>
    ) {}

    override fun unregisterDeviceStateChangeCallback(callback: Consumer<DeviceState>) {
    }

    override fun registerLayoutChangeCallback(
        context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {}

    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
    }
}