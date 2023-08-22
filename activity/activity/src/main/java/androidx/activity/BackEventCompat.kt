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
import androidx.annotation.DoNotInline
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Compat around the [BackEvent] class
 */
class BackEventCompat @VisibleForTesting constructor(
    /**
     * Absolute X location of the touch point of this event in the coordinate space of the view that
     *      * received this back event.
     */
    val touchX: Float,
    /**
     * Absolute Y location of the touch point of this event in the coordinate space of the view that
     * received this back event.
     */
    val touchY: Float,
    /**
     * Value between 0 and 1 on how far along the back gesture is.
     */
    @FloatRange(from = 0.0, to = 1.0)
    val progress: Float,
    /**
     * Indicates which edge the swipe starts from.
     */
    val swipeEdge: @SwipeEdge Int
) {

    @RequiresApi(34)
    constructor(backEvent: BackEvent) : this (
        Api34Impl.touchX(backEvent),
        Api34Impl.touchY(backEvent),
        Api34Impl.progress(backEvent),
        Api34Impl.swipeEdge(backEvent)
    )

    /**
     */
    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDGE_LEFT, EDGE_RIGHT)
    annotation class SwipeEdge

    /**
     * Convert this compat object to [BackEvent] object.
     *
     * @return [BackEvent] object
     *
     * @throws UnsupportedOperationException if this API is called on an API prior to 34.
     */
    @RequiresApi(34)
    fun toBackEvent(): BackEvent {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.createOnBackEvent(touchX, touchY, progress, swipeEdge)
        } else {
            throw UnsupportedOperationException("This method is only supported on API level 34+")
        }
    }

    override fun toString(): String {
        return "BackEventCompat{touchX=$touchX, touchY=$touchY, progress=$progress, " +
            "swipeEdge=$swipeEdge}"
    }

    companion object {
        /** Indicates that the edge swipe starts from the left edge of the screen  */
        const val EDGE_LEFT = 0

        /** Indicates that the edge swipe starts from the right edge of the screen  */
        const val EDGE_RIGHT = 1
    }
}

@RequiresApi(34)
internal object Api34Impl {
    @DoNotInline
    fun createOnBackEvent(touchX: Float, touchY: Float, progress: Float, swipeEdge: Int) =
        BackEvent(touchX, touchY, progress, swipeEdge)

    @DoNotInline
    fun progress(backEvent: BackEvent) = backEvent.progress

    @DoNotInline
    fun touchX(backEvent: BackEvent) = backEvent.touchX

    @DoNotInline
    fun touchY(backEvent: BackEvent) = backEvent.touchY

    @DoNotInline
    fun swipeEdge(backEvent: BackEvent) = backEvent.swipeEdge
}
