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

package androidx.wear.compose.material3.internal

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat

internal enum class WristOrientation {
    /**
     * Device worn on the left wrist with the screen in its default orientation. The RSB is
     * typically on the right side.
     */
    LEFT_WRIST_ROTATION_0,

    /**
     * Device worn on the left wrist with the screen rotated 180°. The RSB is typically on the left
     * side.
     */
    LEFT_WRIST_ROTATION_180,

    /**
     * Device worn on the right wrist with the screen in its default orientation. The RSB is
     * typically on the right side.
     */
    RIGHT_WRIST_ROTATION_0,

    /**
     * Device worn on the right wrist with the screen rotated 180°. The RSB is typically on the left
     * side.
     */
    RIGHT_WRIST_ROTATION_180,
}

internal fun WristOrientation.isLeftWrist(): Boolean =
    this == WristOrientation.LEFT_WRIST_ROTATION_0 ||
        this == WristOrientation.LEFT_WRIST_ROTATION_180

/**
 * [CompositionLocal] providing the global wrist orientation and hardware alignment.
 *
 * This includes the choice of wrist (left or right) and the screen rotation, which determines
 * whether the Rotating Side Button (RSB) is positioned on the left or right side of the device.
 */
internal val LocalWristOrientation: ProvidableCompositionLocal<WristOrientation> =
    compositionLocalWithComputedDefaultOf {
        if (cachedWristOrientation.value == null) {
            val applicationContext = LocalContext.currentValue.applicationContext
            val resolver = applicationContext.contentResolver
            cachedWristContentObserver =
                object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        cachedWristOrientation.value = getWristOrientation(resolver)
                    }
                }
            val isLeftWristUri = Settings.Global.getUriFor(WRIST_ORIENTATION_MODE)
            resolver.registerContentObserver(isLeftWristUri, false, cachedWristContentObserver!!)
            cachedWristOrientation.value = getWristOrientation(resolver)
        }
        cachedWristOrientation.value!!
    }

private fun getWristOrientation(resolver: ContentResolver): WristOrientation {
    return try {
        /*
         * See framework's Settings.Global.Wearable#WRIST_ORIENTATION_MODE.
         * Valid values:
         * - LEFT_WRIST_ROTATION_0 = "0" (default),
         * - LEFT_WRIST_ROTATION_180 = "1",
         * - RIGHT_WRIST_ROTATION_0 = "2",
         * - RIGHT_WRIST_ROTATION_180 = "3"
         */
        val rotation = Settings.Global.getInt(resolver, WRIST_ORIENTATION_MODE, 0)
        when (rotation) {
            1 -> WristOrientation.LEFT_WRIST_ROTATION_180
            2 -> WristOrientation.RIGHT_WRIST_ROTATION_0
            3 -> WristOrientation.RIGHT_WRIST_ROTATION_180
            else -> WristOrientation.LEFT_WRIST_ROTATION_0
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "Failed to fetch wrist orientation, using value: LEFT_WRIST_ROTATION_0", e)
        WristOrientation.LEFT_WRIST_ROTATION_0
    }
}

internal const val TAG = "CompositionLocals"
// See framework's Settings.Global.Wearable#WRIST_ORIENTATION_MODE.
private const val WRIST_ORIENTATION_MODE = "wear_wrist_orientation_mode"

private val cachedWristOrientation: MutableState<WristOrientation?> = mutableStateOf(null)

private var cachedWristContentObserver: ContentObserver? = null
