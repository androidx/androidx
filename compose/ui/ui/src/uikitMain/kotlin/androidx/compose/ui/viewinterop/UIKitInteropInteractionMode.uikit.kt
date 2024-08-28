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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.ExperimentalComposeUiApi
import platform.UIKit.UIScrollView

/**
 * Represents a set of strategies on how the touches are processed when user interacts with the
 * interop view.
 */
@ExperimentalComposeUiApi
sealed interface UIKitInteropInteractionMode {
    /**
     * Represents a mode where the touches are not processed by the Compose UI if the interop view
     * is hit by the initial touch in the gesture.
     */
    @ExperimentalComposeUiApi
    data object NonCooperative : UIKitInteropInteractionMode

    /**
     * Represents a mode where the touches can be processed by the Compose UI if the interop view is
     * hit by the touch event. Compose UI is given a chance to process the motion of the touch while
     * delaying delivery of the touch events to the interop view. If the touch is treated as
     * a pan gesture, Compose will prevent touches from being delivered to the interop view until
     * the gesture ends. If the touch is hold still the tracked touches are treated as cancelled by
     * Compose and will be processed exclusively by the interop view.
     *
     * This behavior aligns with the default behavior of [UIScrollView]
     *
     * This mode is useful when the interop view is inside a scrollable container and the user might
     * want to scroll the container despite the first touch being landed on the interop view.
     *
     * @property delayMillis Indicates how much time in milliseconds is given for Compose to intercept
     * the touches before delivering them to the interop view. The default value is [DefaultDelayMillis].
     */
    @ExperimentalComposeUiApi
    class Cooperative(
        val delayMillis: Int = DefaultDelayMillis
    ) : UIKitInteropInteractionMode {
        init {
            require(delayMillis > 0) { "Delay must be a positive value" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Cooperative) return false

            if (delayMillis != other.delayMillis) return false

            return true
        }

        override fun hashCode(): Int {
            return delayMillis.hashCode()
        }

        companion object {
            /**
             * The default delay in milliseconds before the touch is delivered to the interop view.
             * Same as the default delay in [UIScrollView].
             */
            const val DefaultDelayMillis = 150
        }
    }
}