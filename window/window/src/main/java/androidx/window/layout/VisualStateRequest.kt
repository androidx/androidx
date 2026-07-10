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

package androidx.window.layout

/**
 * A request to modify the visual state of the display for the window.
 *
 * Useful for when the display is an opaque optical see-through display, e.g. XR glasses, that
 * allows applications to request the display visuals remain sustained even if the screen would
 * normally turn off.
 *
 * @see WindowInfoController.requestVisualState
 */
public class VisualStateRequest internal constructor(private val sustainedVisualsEnabled: Boolean) {

    /**
     * If true, requests the screen visuals to be sustained on displays when the system would
     * otherwise turn off the screen. The default value is false.
     *
     * This feature is only supported on Android 16 (API level 37) and above. On lower API levels,
     * the request will be ignored.
     */
    public fun isSustainedVisualsEnabled(): Boolean = sustainedVisualsEnabled

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualStateRequest) return false
        return sustainedVisualsEnabled == other.sustainedVisualsEnabled
    }

    override fun hashCode(): Int {
        return sustainedVisualsEnabled.hashCode()
    }

    override fun toString(): String {
        return "VisualStateRequest{isSustainedVisualsEnabled=$sustainedVisualsEnabled}"
    }

    /** A builder for [VisualStateRequest]. */
    public class Builder {
        private var isSustainedVisualsEnabled: Boolean = false

        /** Sets whether to request screen visuals be sustained. */
        public fun setSustainedVisualsEnabled(isSustainedVisualsEnabled: Boolean): Builder = apply {
            this.isSustainedVisualsEnabled = isSustainedVisualsEnabled
        }

        /** Builds a [VisualStateRequest] with the provided properties. */
        public fun build(): VisualStateRequest {
            return VisualStateRequest(isSustainedVisualsEnabled)
        }
    }
}
