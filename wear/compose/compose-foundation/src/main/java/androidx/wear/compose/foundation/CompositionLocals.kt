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
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * CompositionLocal for global reduce-motion setting, which turns off animations and
 * screen movements. To use, call LocalReduceMotion.current.enabled(), which returns a Boolean.
 */
@get:ExperimentalWearFoundationApi
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalWearFoundationApi
val LocalReduceMotion: ProvidableCompositionLocal<ReduceMotion> = staticCompositionLocalOf {
    ReduceMotion {
        val context = LocalContext.current.applicationContext
        getReduceMotionFlowFor(context).value
    }
}

/**
 * CompositionLocal containing the background scrim color of [SwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
public val LocalSwipeToDismissBackgroundScrimColor: ProvidableCompositionLocal<Color> =
    compositionLocalOf { Color.Black }

/**
 * CompositionLocal containing the content scrim color of [SwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
public val LocalSwipeToDismissContentScrimColor: ProvidableCompositionLocal<Color> =
    compositionLocalOf { Color.Black }

/**
 * ReduceMotion provides a means for callers to determine whether an app should turn off
 * animations and screen movement.
 */
@ExperimentalWearFoundationApi
fun interface ReduceMotion {
    @Composable
    fun enabled(): Boolean
}

private val reduceMotionCache = AtomicReference<StateFlow<Boolean>>()

// Callers of this function should pass an application context. Passing an activity context might
// result in activity leaks.
private fun getReduceMotionFlowFor(applicationContext: Context): StateFlow<Boolean> {
    val resolver = applicationContext.contentResolver
    val reduceMotionUri = Settings.Global.getUriFor(REDUCE_MOTION)
    val channel = Channel<Unit>(CONFLATED)
    val contentObserver =
        object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                channel.trySend(Unit)
            }
        }
    return reduceMotionCache.updateAndGet {
        it ?: flow {
            resolver.registerContentObserver(reduceMotionUri, false, contentObserver)
            try {
                for (value in channel) {
                    val newValue = getReducedMotionSettingValue(resolver)
                    emit(newValue)
                }
            } finally {
                resolver.unregisterContentObserver(contentObserver)
            }
        }.stateIn(
            MainScope(),
            SharingStarted.WhileSubscribed(),
            getReducedMotionSettingValue(resolver)
        )
    }
}

private fun getReducedMotionSettingValue(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(
        resolver,
        REDUCE_MOTION,
        REDUCE_MOTION_DEFAULT
    ) == 1
}

// See framework's Settings.Global.Wearable#REDUCE_MOTION.
private const val REDUCE_MOTION = "reduce_motion"
private const val REDUCE_MOTION_DEFAULT = 0
