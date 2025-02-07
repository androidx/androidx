/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.foundation

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.os.HandlerCompat

/**
 * CompositionLocal for global reduce-motion setting, which turns off animations and screen
 * movements. To use, call LocalReduceMotion.current, which returns a Boolean.
 */
public val LocalReduceMotion: ProvidableCompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        val view = LocalView.currentValue
        @Suppress("UNCHECKED_CAST")
        val cachedValue = view.getTag(R.id.reduce_motion_tag) as MutableState<Boolean>?
        val currentState =
            if (cachedValue != null) {
                cachedValue
            } else {
                val resolver = LocalContext.currentValue.contentResolver
                val currentValue = getReducedMotionSettingValue(resolver)
                val state = mutableStateOf(currentValue)
                view.setTag(R.id.reduce_motion_tag, state)
                val contentObserver =
                    object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean, uri: Uri?) {
                            state.value = getReducedMotionSettingValue(resolver)
                        }
                    }
                val reduceMotionUri = Settings.Global.getUriFor(REDUCE_MOTION)
                resolver.registerContentObserver(reduceMotionUri, false, contentObserver)
                view.addOnAttachStateChangeListener(
                    object : OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            resolver.registerContentObserver(
                                reduceMotionUri,
                                false,
                                contentObserver
                            )
                        }

                        override fun onViewDetachedFromWindow(v: View) {
                            resolver.unregisterContentObserver(contentObserver)
                        }
                    }
                )
                state
            }

        currentState.value
    }

/**
 * CompositionLocal containing the background scrim color of [BasicSwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
public val LocalSwipeToDismissBackgroundScrimColor: ProvidableCompositionLocal<Color> =
    compositionLocalOf {
        Color.Black
    }

/**
 * CompositionLocal containing the content scrim color of [BasicSwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
public val LocalSwipeToDismissContentScrimColor: ProvidableCompositionLocal<Color> =
    compositionLocalOf {
        Color.Black
    }

private fun getReducedMotionSettingValue(resolver: ContentResolver): Boolean {
    return try {
        Settings.Global.getInt(resolver, REDUCE_MOTION, REDUCE_MOTION_DEFAULT) == 1
    } catch (e: SecurityException) {
        Log.w(TAG, "Failed to fetch reduce motion setting, using value: false", e)
        false
    }
}

// See framework's Settings.Global.Wearable#REDUCE_MOTION.
private const val REDUCE_MOTION = "reduce_motion"
private const val REDUCE_MOTION_DEFAULT = 0

internal const val TAG = "CompositionLocals"
