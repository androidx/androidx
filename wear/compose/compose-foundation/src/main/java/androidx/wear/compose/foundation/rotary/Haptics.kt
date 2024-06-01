/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.google.wear.input.WearHapticFeedbackConstants
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

/** Handles haptics for rotary usage */
internal interface RotaryHapticHandler {

    /** Handles haptics when scroll is used */
    fun handleScrollHaptic(timestamp: Long, deltaInPixels: Float)

    /** Handles haptics when scroll with snap is used */
    fun handleSnapHaptic(timestamp: Long, deltaInPixels: Float)

    /** Handles haptics when edge of the list is reached */
    fun handleLimitHaptic(isStart: Boolean)
}

@Composable
internal fun rememberRotaryHapticHandler(
    scrollableState: ScrollableState,
    hapticsEnabled: Boolean
): RotaryHapticHandler =
    if (hapticsEnabled) {
        // TODO(b/319103162): Add platform haptics once AndroidX updates to Android VanillaIceCream
        rememberCustomRotaryHapticHandler(scrollableState)
    } else {
        rememberDisabledRotaryHapticHandler()
    }

/**
 * Remembers custom rotary haptic handler.
 *
 * @param scrollableState A scrollableState, used to determine whether the end of the scrollable was
 *   reached or not.
 */
@Composable
private fun rememberCustomRotaryHapticHandler(
    scrollableState: ScrollableState,
): RotaryHapticHandler {
    val hapticsProvider = rememberRotaryHapticFeedbackProvider()
    // Channel to which haptic events will be sent
    val hapticsChannel: Channel<RotaryHapticsType> = rememberHapticChannel()

    // Throttling events within specified timeframe.
    // Only first and last events will be received. Check [throttleLatest] function for more info.
    val throttleThresholdMs: Long = 30
    // A scroll threshold after which haptic is produced.
    val hapticsThresholdPx: Long = 50

    LaunchedEffect(hapticsChannel, throttleThresholdMs) {
        hapticsChannel.receiveAsFlow().throttleLatest(throttleThresholdMs).collect { hapticType ->
            // 'withContext' launches performHapticFeedback in a separate thread,
            // as otherwise it produces a visible lag (b/219776664)
            val currentTime = System.currentTimeMillis()
            debugLog { "Haptics started" }
            withContext(Dispatchers.Default) {
                debugLog {
                    "Performing haptics, delay: " + "${System.currentTimeMillis() - currentTime}"
                }
                hapticsProvider.performHapticFeedback(hapticType)
            }
        }
    }
    return remember(scrollableState, hapticsChannel, hapticsProvider) {
        CustomRotaryHapticHandler(scrollableState, hapticsChannel, hapticsThresholdPx)
    }
}

@Composable
private fun rememberRotaryHapticFeedbackProvider(): RotaryHapticFeedbackProvider =
    LocalView.current.let { view ->
        remember {
            val hapticConstants = getCustomRotaryConstants(view)
            RotaryHapticFeedbackProvider(view, hapticConstants)
        }
    }

@VisibleForTesting
internal fun getCustomRotaryConstants(view: View): HapticConstants =
    when {
        // Order here is very important: We want to use WearSDK haptic constants for
        // all devices having api 34 and up, but for Wear3.5 and Wear 4 constants should be
        // different for Galaxy watches and other devices.
        hasWearSDK(view.context) -> HapticConstants.WearSDKHapticConstants
        isGalaxyWatch() -> HapticConstants.GalaxyWatchConstants
        isWear3_5(view.context) -> HapticConstants.Wear3Point5RotaryHapticConstants
        isWear4() -> HapticConstants.Wear4RotaryHapticConstants
        else -> HapticConstants.DisabledHapticConstants
    }

@VisibleForTesting
internal sealed class HapticConstants(
    val scrollFocus: Int?,
    val scrollTick: Int?,
    val scrollLimit: Int?
) {
    /** Rotary haptic constants from WearSDK */
    object WearSDKHapticConstants :
        HapticConstants(
            WearHapticFeedbackConstants.getScrollItemFocus(),
            WearHapticFeedbackConstants.getScrollTick(),
            WearHapticFeedbackConstants.getScrollLimit()
        )

    /**
     * Rotary haptic constants for Galaxy Watch. These constants are used by Samsung for producing
     * rotary haptics
     */
    object GalaxyWatchConstants : HapticConstants(102, 101, 50107)

    /** Hidden constants from HapticFeedbackConstants.java API 33, Wear 4 */
    object Wear4RotaryHapticConstants : HapticConstants(19, 18, 20)

    /** Hidden constants from HapticFeedbackConstants.java API 30, Wear 3.5 */
    object Wear3Point5RotaryHapticConstants : HapticConstants(10003, 10002, 10003)

    object DisabledHapticConstants : HapticConstants(null, null, null)
}

@Composable
private fun rememberHapticChannel() = remember {
    Channel<RotaryHapticsType>(capacity = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

/**
 * This class handles haptic feedback based on the [scrollableState], scrolled pixels and
 * [hapticsThresholdPx]. Haptic is not fired in this class, instead it's sent to [hapticsChannel]
 * where it'll be performed later.
 *
 * @param scrollableState Haptic performed based on this state
 * @param hapticsChannel Channel to which haptic events will be sent
 * @param hapticsThresholdPx A scroll threshold after which haptic is produced.
 */
private class CustomRotaryHapticHandler(
    private val scrollableState: ScrollableState,
    private val hapticsChannel: Channel<RotaryHapticsType>,
    private val hapticsThresholdPx: Long = 50
) : RotaryHapticHandler {

    private var overscrollHapticTriggered = false
    private var currScrollPosition = 0f
    private var prevHapticsPosition = 0f

    override fun handleScrollHaptic(timestamp: Long, deltaInPixels: Float) {
        if (scrollableState.reachedTheLimit(deltaInPixels)) {
            handleLimitHaptic(scrollableState.canScrollBackward)
        } else {
            overscrollHapticTriggered = false
            currScrollPosition += deltaInPixels
            val diff = abs(currScrollPosition - prevHapticsPosition)

            if (diff >= hapticsThresholdPx) {
                hapticsChannel.trySend(RotaryHapticsType.ScrollTick)
                prevHapticsPosition = currScrollPosition
            }
        }
    }

    override fun handleSnapHaptic(timestamp: Long, deltaInPixels: Float) {
        if (scrollableState.reachedTheLimit(deltaInPixels)) {
            handleLimitHaptic(scrollableState.canScrollBackward)
        } else {
            overscrollHapticTriggered = false
            hapticsChannel.trySend(RotaryHapticsType.ScrollItemFocus)
        }
    }

    override fun handleLimitHaptic(isStart: Boolean) {
        if (!overscrollHapticTriggered) {
            hapticsChannel.trySend(RotaryHapticsType.ScrollLimit)
            overscrollHapticTriggered = true
        }
    }
}

/** Rotary haptic types */
@JvmInline
@VisibleForTesting
internal value class RotaryHapticsType(private val type: Int) {
    companion object {

        /**
         * A scroll ticking haptic. Similar to texture haptic - performed each time when a
         * scrollable content is scrolled by a certain distance
         */
        public val ScrollTick: RotaryHapticsType = RotaryHapticsType(1)

        /**
         * An item focus (snap) haptic. Performed when a scrollable content is snapped to a specific
         * item.
         */
        public val ScrollItemFocus: RotaryHapticsType = RotaryHapticsType(2)

        /**
         * A limit(overscroll) haptic. Performed when a list reaches the limit (start or end) and
         * can't scroll further
         */
        public val ScrollLimit: RotaryHapticsType = RotaryHapticsType(3)
    }
}

/** Remember disabled haptics handler */
@Composable
private fun rememberDisabledRotaryHapticHandler(): RotaryHapticHandler = remember {
    object : RotaryHapticHandler {
        override fun handleScrollHaptic(timestamp: Long, deltaInPixels: Float) {
            // Do nothing
        }

        override fun handleSnapHaptic(timestamp: Long, deltaInPixels: Float) {
            // Do nothing
        }

        override fun handleLimitHaptic(isStart: Boolean) {
            // Do nothing
        }
    }
}

/** Rotary haptic feedback */
private class RotaryHapticFeedbackProvider(
    private val view: View,
    private val hapticConstants: HapticConstants
) {
    fun performHapticFeedback(
        type: RotaryHapticsType,
    ) {
        when (type) {
            RotaryHapticsType.ScrollItemFocus -> {
                hapticConstants.scrollFocus?.let { view.performHapticFeedback(it) }
            }
            RotaryHapticsType.ScrollTick -> {
                hapticConstants.scrollTick?.let { view.performHapticFeedback(it) }
            }
            RotaryHapticsType.ScrollLimit -> {
                hapticConstants.scrollLimit?.let { view.performHapticFeedback(it) }
            }
        }
    }
}

private fun isGalaxyWatch(): Boolean =
    Build.MANUFACTURER.contains("Samsung", ignoreCase = true) &&
        Build.MODEL.matches("^SM-R.*\$".toRegex())

private fun isWear3_5(context: Context): Boolean =
    Build.VERSION.SDK_INT == Build.VERSION_CODES.R && getWearPlatformMrNumber(context) >= 5

private fun isWear4(): Boolean = Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU

private fun hasWearSDK(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

private fun getWearPlatformMrNumber(context: Context): Int =
    Settings.Global.getString(context.contentResolver, WEAR_PLATFORM_MR_NUMBER)?.toIntOrNull() ?: 0

private const val WEAR_PLATFORM_MR_NUMBER: String = "wear_platform_mr_number"

private fun ScrollableState.reachedTheLimit(scrollDelta: Float): Boolean =
    (scrollDelta > 0 && !canScrollForward) || (scrollDelta < 0 && !canScrollBackward)

/** Debug logging that can be enabled. */
private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("RotaryHaptics: ${generateMsg()}")
    }
}

/**
 * Throttling events within specified timeframe. Only first and last events will be received.
 *
 * For example, a flow emits elements 1 to 30, with a 100ms delay between them:
 * ```
 * val flow = flow {
 *     for (i in 1..30) {
 *         delay(100)
 *         emit(i)
 *     }
 * }
 * ```
 *
 * With timeframe=1000 only those integers will be received: 1, 10, 20, 30 .
 */
@VisibleForTesting
internal fun <T> Flow<T>.throttleLatest(timeframe: Long): Flow<T> = flow {
    conflate().collect {
        emit(it)
        delay(timeframe)
    }
}
