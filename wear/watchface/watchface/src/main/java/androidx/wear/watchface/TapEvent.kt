/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.wear.watchface.control.IInteractiveWatchFace
import java.time.Instant

/** @hide */
@IntDef(
    value = [
        TapType.DOWN,
        TapType.UP,
        TapType.CANCEL
    ]
)
public annotation class TapType {
    public companion object {
        /**
         * Used to indicate a "down" touch event on the watch face.
         *
         * The watch face will receive an [UP] or a [CANCEL] event to follow this event, to
         * indicate whether this down event corresponds to a tap gesture to be handled by the watch
         * face, or a different type of gesture that is handled by the system, respectively.
         */
        public const val DOWN: Int = IInteractiveWatchFace.TAP_TYPE_DOWN

        /**
         * Used in to indicate that a previous [TapType.DOWN] touch event has been canceled. This
         * generally happens when the watch face is touched but then a move or long press occurs.
         *
         * The watch face should not trigger any action, as the system is already processing the
         * gesture.
         */
        public const val CANCEL: Int = IInteractiveWatchFace.TAP_TYPE_CANCEL

        /**
         * Used to indicate that an "up" event on the watch face has occurred that has not been
         * consumed by the system. A [TapType.DOWN] will always occur first. This event will not
         * be sent if a [TapType.CANCEL] is sent.
         *
         * Therefore, a [TapType.DOWN] event and the successive [TapType.UP] event are guaranteed
         * to be close enough to be considered a tap according to the value returned by
         * [android.view.ViewConfiguration.getScaledTouchSlop].
         */
        public const val UP: Int = IInteractiveWatchFace.TAP_TYPE_UP
    }
}

/**
 * An input event received by the watch face.
 *
 * @param xPos X coordinate of the event
 * @param yPos Y coordinate of the event
 * @param tapTime The [Instant] at which the tap event occurred
 */
public class TapEvent(
    @Px public val xPos: Int,
    @Px public val yPos: Int,
    public val tapTime: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TapEvent

        if (xPos != other.xPos) return false
        if (yPos != other.yPos) return false
        if (tapTime != other.tapTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xPos
        result = 31 * result + yPos
        result = 31 * result + tapTime.hashCode()
        return result
    }

    override fun toString(): String = "[$xPos, $yPos @$tapTime]"
}
