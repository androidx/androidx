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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * CompositionLocal for global reduce-motion setting, which turns off animations and screen
 * movements. To use, call LocalReduceMotion.current.enabled(), which returns a Boolean.
 */
val LocalReduceMotion: ProvidableCompositionLocal<ReduceMotion> = staticCompositionLocalOf {
    ReduceMotion {
        val context = LocalContext.current.applicationContext
        val flow = getReduceMotionFlowFor(context)
        flow.collectAsStateWithLifecycle().value
    }
}

/**
 * CompositionLocal containing the background scrim color of [BasicSwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
val LocalSwipeToDismissBackgroundScrimColor: ProvidableCompositionLocal<Color> =
    compositionLocalOf {
        Color.Black
    }

/**
 * CompositionLocal containing the content scrim color of [BasicSwipeToDismissBox].
 *
 * Defaults to [Color.Black] if not explicitly set.
 */
val LocalSwipeToDismissContentScrimColor: ProvidableCompositionLocal<Color> = compositionLocalOf {
    Color.Black
}

/**
 * ReduceMotion provides a means for callers to determine whether an app should turn off animations
 * and screen movement.
 */
fun interface ReduceMotion {
    @Composable fun enabled(): Boolean
}

private val reduceMotionCache = AtomicReference<StateFlow<Boolean>>()

// Callers of this function should pass an application context. Passing an activity context might
// result in activity leaks.
@Composable
private fun getReduceMotionFlowFor(applicationContext: Context): StateFlow<Boolean> {
    val resolver = applicationContext.contentResolver
    val reduceMotionUri = Settings.Global.getUriFor(REDUCE_MOTION)
    val coroutineScope = rememberCoroutineScope()

    return reduceMotionCache.updateAndGet {
        it
            ?: callbackFlow {
                    val contentObserver =
                        object :
                            ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                            override fun deliverSelfNotifications(): Boolean {
                                // Returning true to receive change notification so that
                                // the flow sends new value after it is initialized.
                                return true
                            }

                            override fun onChange(selfChange: Boolean, uri: Uri?) {
                                super.onChange(selfChange, uri)
                                trySend(getReducedMotionSettingValue(resolver))
                            }
                        }

                    coroutineScope.launch {
                        resolver.registerContentObserver(reduceMotionUri, false, contentObserver)
                        // Force send value when flow is initialized
                        resolver.notifyChange(reduceMotionUri, contentObserver)
                    }

                    awaitClose { resolver.unregisterContentObserver(contentObserver) }
                }
                .stateIn(
                    MainScope(),
                    SharingStarted.WhileSubscribed(5000),
                    getReducedMotionSettingValue(resolver)
                )
    }
}

private fun getReducedMotionSettingValue(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, REDUCE_MOTION, REDUCE_MOTION_DEFAULT) == 1
}

// See framework's Settings.Global.Wearable#REDUCE_MOTION.
private const val REDUCE_MOTION = "reduce_motion"
private const val REDUCE_MOTION_DEFAULT = 0
