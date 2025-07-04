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

package androidx.activity

import android.os.Build
import android.window.BackEvent
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.navigationevent.NavigationEvent

/** Compat around the [BackEvent] class */
class BackEventCompat
@VisibleForTesting
@JvmOverloads
constructor(
    /**
     * Absolute X location of the touch point of this event in the coordinate space of the view that
     * * received this back event.
     */
    val touchX: Float,
    /**
     * Absolute Y location of the touch point of this event in the coordinate space of the view that
     * received this back event.
     */
    val touchY: Float,
    /** Value between 0 and 1 on how far along the back gesture is. */
    @FloatRange(from = 0.0, to = 1.0) val progress: Float,
    /** Indicates which edge the swipe starts from. */
    val swipeEdge: @SwipeEdge Int,
    /** Frame time of the back event. */
    val frameTimeMillis: Long = 0,
) {

    /**
     * Constructs a [BackEventCompat] from a [BackEvent] object.
     *
     * This constructor is used for API level 34 and above, mapping the [BackEvent]'s properties to
     * the corresponding values in [BackEventCompat].
     *
     * @param backEvent The [BackEvent] instance to convert.
     */
    @RequiresApi(34)
    constructor(
        backEvent: BackEvent
    ) : this(
        Api34Impl.touchX(backEvent),
        Api34Impl.touchY(backEvent),
        Api34Impl.progress(backEvent),
        Api34Impl.swipeEdge(backEvent),
        if (Build.VERSION.SDK_INT >= 36) {
            Api36Impl.frameTimeMillis(backEvent)
        } else {
            0
        },
    )

    /**
     * Constructs a [BackEventCompat] from a [NavigationEvent] object.
     *
     * This constructor is used for compatibility with [NavigationEvent] and maps its properties to
     * the corresponding values in [BackEventCompat].
     *
     * @param navigationEvent The [NavigationEvent] instance to convert.
     */
    constructor(
        navigationEvent: NavigationEvent
    ) : this(
        navigationEvent.touchX,
        navigationEvent.touchY,
        navigationEvent.progress,
        navigationEvent.swipeEdge,
        navigationEvent.frameTimeMillis,
    )

    /**  */
    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDGE_LEFT, EDGE_RIGHT, EDGE_NONE)
    annotation class SwipeEdge

    /**
     * Convert this [BackEventCompat] object to a [BackEvent] object.
     *
     * @return A new [BackEvent] object populated with this [BackEventCompat] data.
     * @throws UnsupportedOperationException if this API is called on an API prior to 34.
     */
    @RequiresApi(34)
    fun toBackEvent(): BackEvent {
        return if (Build.VERSION.SDK_INT >= 36) {
            Api36Impl.createOnBackEvent(touchX, touchY, progress, swipeEdge, frameTimeMillis)
        } else {
            Api34Impl.createOnBackEvent(touchX, touchY, progress, swipeEdge)
        }
    }

    /**
     * Convert this [BackEventCompat] object to a [NavigationEvent] object.
     *
     * @return A new [NavigationEvent] object populated with this [BackEventCompat] data.
     */
    fun toNavigationEvent(): NavigationEvent {
        return NavigationEvent(touchX, touchY, progress, swipeEdge, frameTimeMillis)
    }

    override fun toString(): String {
        return "BackEventCompat{touchX=$touchX, touchY=$touchY, progress=$progress, " +
            "swipeEdge=$swipeEdge, frameTimeMillis=$frameTimeMillis}"
    }

    companion object {
        /** Indicates that the edge swipe starts from the left edge of the screen */
        const val EDGE_LEFT = 0

        /** Indicates that the edge swipe starts from the right edge of the screen */
        const val EDGE_RIGHT = 1

        /**
         * Indicates that the back event was not triggered by an edge swipe back gesture. This
         * applies to cases like using the back button in 3-button navigation or pressing a hardware
         * back button.
         */
        const val EDGE_NONE = 2
    }
}

@RequiresApi(34)
internal object Api34Impl {
    fun createOnBackEvent(touchX: Float, touchY: Float, progress: Float, swipeEdge: Int) =
        BackEvent(touchX, touchY, progress, swipeEdge)

    fun progress(backEvent: BackEvent) = backEvent.progress

    fun touchX(backEvent: BackEvent) = backEvent.touchX

    fun touchY(backEvent: BackEvent) = backEvent.touchY

    fun swipeEdge(backEvent: BackEvent) = backEvent.swipeEdge
}

@RequiresApi(36)
internal object Api36Impl {
    fun createOnBackEvent(
        touchX: Float,
        touchY: Float,
        progress: Float,
        swipeEdge: Int,
        frameTimeMillis: Long,
    ) = BackEvent(touchX, touchY, progress, swipeEdge, frameTimeMillis)

    fun frameTimeMillis(backEvent: BackEvent) = backEvent.frameTimeMillis
}
