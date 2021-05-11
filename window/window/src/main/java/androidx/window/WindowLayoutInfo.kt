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
package androidx.window

/**
 * Contains the list of [DisplayFeature]-s located within the window. For example, a hinge or
 * display fold can go across the window, in which case it might make sense to separate the
 * visual content and interactive elements into two groups, e.g. list-detail or view-controls.
 *
 * Only the features that are present within the current window bounds are reported. Their
 * positions and sizes can change if the window is moved or resized on screen.
 * @see WindowManager.registerLayoutChangeCallback
 */
public class WindowLayoutInfo internal constructor(
    /**
     * [displayFeatures] all the [DisplayFeature] within the window.
     */
    public val displayFeatures: List<DisplayFeature>
) {

    override fun toString(): String {
        return displayFeatures.joinToString(
            separator = ", ",
            prefix = "WindowLayoutInfo{ DisplayFeatures[",
            postfix = "] }"
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WindowLayoutInfo
        return displayFeatures == that.displayFeatures
    }

    override fun hashCode(): Int {
        return displayFeatures.hashCode()
    }

    /**
     * Builder for [WindowLayoutInfo] objects.
     */
    public class Builder {
        private var displayFeatures: List<DisplayFeature> = emptyList()

        /**
         * Sets the display features for the [WindowLayoutInfo] instance.
         */
        public fun setDisplayFeatures(displayFeatures: List<DisplayFeature>): Builder {
            this.displayFeatures = displayFeatures.toList()
            return this
        }

        /**
         * Creates a [WindowLayoutInfo] instance with the specified fields.
         * @return A WindowLayoutInfo instance.
         */
        public fun build(): WindowLayoutInfo {
            return WindowLayoutInfo(displayFeatures.toList())
        }
    }
}
