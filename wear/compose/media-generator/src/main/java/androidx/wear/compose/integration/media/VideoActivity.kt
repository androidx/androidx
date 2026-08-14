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

package androidx.wear.compose.integration.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoActivity : ComponentActivity() {
    companion object {
        /*
         * Note: These intent action strings must exactly match the global variables
         * INTENT_RESTART_ANIMATION and INTENT_PERFORM_FORWARD_FLICK defined in the Python
         * orchestration script (scripts/utils.py).
         */
        const val INTENT_RESTART_ANIMATION =
            "androidx.wear.compose.integration.media.RESTART_ANIMATION"

        const val INTENT_PERFORM_FORWARD_FLICK =
            "androidx.wear.compose.integration.media.PERFORM_FORWARD_FLICK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /*
         * Unlike in the static screenshots system where we cycle through all samples in one run,
         * here in the video recording system, Python launches VideoActivity individually for each
         * target sample to avoid state pollution and ensure each animation starts from a clean,
         * deterministic state.
         */
        val sampleName = intent.getStringExtra("sample_name") ?: ""

        setContent {
            /*
             * [AUTOPLAY & OHG SAMPLES] ADB Hook: The UI Restarter (RESTART_ANIMATION)
             * Used to force-replay `triggers_on_load=True` animations (like ConfirmationDialog
             * or OHG samples) that play immediately upon UI inflation.
             *
             * To prevent the recording from missing the beginning of these animations while the ADB
             * screen recorder is starting up, Python sends the RESTART_ANIMATION broadcast
             * once recording actively begins. This forces Compose to rebuild the sample UI.
             *
             * Standard interactive gesture samples do not use this restart mechanism, as they
             * remain static and wait for ADB touch inputs to trigger their animations.
             */
            var recomposeKey by remember { mutableIntStateOf(0) }

            BroadcastReceiverEffect(INTENT_RESTART_ANIMATION) { recomposeKey++ }

            MaterialTheme {
                AppScaffold(timeText = {}) {
                    if (sampleName.contains("OneHandedGesture")) {
                        OhgRecordingEnvironment(lifecycle = this@VideoActivity.lifecycle) {
                            RenderSample(sampleName = sampleName, recomposeKey = recomposeKey)
                        }
                    } else {
                        RenderSample(sampleName = sampleName, recomposeKey = recomposeKey)
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastReceiverEffect(intentAction: String, onReceive: (Intent?) -> Unit) {
    val context = LocalContext.current

    val currentOnReceive by rememberUpdatedState(onReceive)

    DisposableEffect(context, intentAction) {
        val filter = IntentFilter(intentAction)

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    currentOnReceive(intent)
                }
            }

        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        onDispose { context.unregisterReceiver(receiver) }
    }
}

@Composable
private fun rememberNoOpBackDispatcherOwner(lifecycle: Lifecycle): OnBackPressedDispatcherOwner {
    return remember(lifecycle) {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle = lifecycle

            override val onBackPressedDispatcher = OnBackPressedDispatcher {}
        }
    }
}

/*
 * Accesses internal OneHandedGestureManager CompositionLocal via reflection
 * to intercept wrist gestures without modifying internal Compose M3 API visibility.
 * UNCHECKED_CAST is suppressed for AndroidX build log simplifier compliance.
 */
@Suppress("BanUncheckedReflection", "UNCHECKED_CAST")
private fun getLocalOneHandedGestureManager(): ProvidableCompositionLocal<Any>? {
    return try {
        val clazz =
            Class.forName(
                "androidx.wear.compose.material3.onehandedgesture.OneHandedGestureManagerKt"
            )

        val method = clazz.getDeclaredMethod("getLocalOneHandedGestureManager")

        method.isAccessible = true

        method.invoke(null) as? ProvidableCompositionLocal<Any>
    } catch (e: Exception) {
        null
    }
}

/*
 * Wraps real OneHandedGestureManager in a dynamic proxy to capture gesture callbacks.
 * UNCHECKED_CAST is suppressed for AndroidX build log simplifier compliance.
 */
@Suppress("BanUncheckedReflection", "UNCHECKED_CAST")
@Composable
private fun rememberGestureManagerProxy(
    realManager: Any?,
    scope: CoroutineScope,
    onGestureRegistered: ((suspend (Offset) -> Unit)?) -> Unit,
): Any? {
    return remember(realManager) {
        try {
            val managerClass =
                Class.forName(
                    "androidx.wear.compose.material3.onehandedgesture.OneHandedGestureManager"
                )

            Proxy.newProxyInstance(
                managerClass.classLoader,
                arrayOf(managerClass),
                object : InvocationHandler {
                    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                        when (method?.name) {
                            "registerGesture" -> {
                                args?.let {
                                    val onAvailable = it[5] as? () -> Unit
                                    val onGesture = it[6] as? suspend (Offset) -> Unit
                                    onGestureRegistered(onGesture)
                                    scope.launch {
                                        delay(1000)
                                        onAvailable?.invoke()
                                    }
                                }
                            }
                            "updateGesture" -> {
                                args?.let {
                                    val onGesture = it[6] as? suspend (Offset) -> Unit
                                    onGestureRegistered(onGesture)
                                }
                            }
                        }

                        return method?.invoke(realManager, *(args ?: emptyArray()))
                    }
                },
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun OhgRecordingEnvironment(lifecycle: Lifecycle, content: @Composable () -> Unit) {
    /*
     * One-Handed Gesture (OHG) Recording Environment
     * Emulators do not support physical wrist gestures. To record OHG samples on emulators,
     * we intercept the OneHandedGestureManager via a dynamic proxy to handle two things:
     *
     * 1. Hand Animation Cue (`onAvailable`): Automatically invoked 1 second after registration,
     *    forcing the component to display its hand/finger tapping animation cue on screen.
     * 2. ADB Forward Flick Trigger (`onGesture`): Captures the component's internal `onGesture`
     *    callback. When Python sends the PERFORM_FORWARD_FLICK broadcast via ADB, we invoke
     *    `onGesture()` to trigger the forward flick UI response (e.g. button click or column scroll).
     */
    val scope = rememberCoroutineScope()

    var onGestureCallback: (suspend (Offset) -> Unit)? = remember { null }

    BroadcastReceiverEffect(VideoActivity.INTENT_PERFORM_FORWARD_FLICK) {
        scope.launch { onGestureCallback?.invoke(Offset.Zero) }
    }

    /*
     * Disables the system swipe-to-dismiss behavior. We specifically need this to
     * intercept the back action of the 'close' edge buttons in the
     * OneHandedGestureScalingLazyColumnSample and OneHandedGestureTransformingLazyColumnSample.
     * If we didn't swallow this event, the flick would instantly close the app.
     */
    val noOpBackDispatcherOwner = rememberNoOpBackDispatcherOwner(lifecycle)

    /*
     * Intercept the real gesture manager and replace it with our gesture proxy.
     * The proxy steals the `onGesture` callback and passes it up to our receiver.
     */
    val localOneHandedGestureManager = getLocalOneHandedGestureManager()

    val realManager = localOneHandedGestureManager?.current

    val gestureManagerProxy =
        rememberGestureManagerProxy(
            realManager = realManager,
            scope = scope,
            onGestureRegistered = { onGestureCallback = it },
        )

    // Inject the gesture proxy and custom back dispatcher into the CompositionLocalProvider

    val providedList = mutableListOf<ProvidedValue<*>>()

    providedList.add(LocalOnBackPressedDispatcherOwner provides noOpBackDispatcherOwner)

    if (localOneHandedGestureManager != null && gestureManagerProxy != null) {
        providedList.add(localOneHandedGestureManager.provides(gestureManagerProxy))
    }

    CompositionLocalProvider(*providedList.toTypedArray(), content = content)
}

@Composable
private fun RenderSample(sampleName: String, recomposeKey: Int) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        /*
         * Inflates the sample initially for all samples. For `triggers_on_load` samples
         * (Autoplay/OHG), the recomposeKey will increment later and force this block to
         * run a second time, clearing and rebuilding the UI to reset the animation.
         */
        key(recomposeKey) {
            var ready by remember { mutableStateOf(false) }

            /*
             * Emits a 100ms black screen reset window upon recomposition, producing a distinct black
             * frame marker in the raw MP4 recording. This allows Python OpenCV frame analysis
             * (`get_true_start_time`) to pinpoint the exact animation start time and
             * cleanly crop off pre-roll footage.
             */
            LaunchedEffect(Unit) {
                delay(100)
                ready = true
            }

            if (!ready) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            } else {
                videoRegistry[sampleName]?.invoke() ?: FallbackSample(sampleName)
            }
        }
    }
}
