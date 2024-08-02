/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.core.app

import android.app.Activity
import android.content.res.Configuration
import androidx.annotation.RequiresApi

/**
 * Class that encapsulates the information that is delivered when
 * [Activity.onMultiWindowModeChanged] is dispatched to a [OnMultiWindowModeChangedProvider].
 */
public class MultiWindowModeChangedInfo(
    /**
     * Gets the new multi-window mode.
     *
     * @return True if the activity is in multi-window mode.
     */
    public val isInMultiWindowMode: Boolean,
) {
    @RequiresApi(26) private var newConfiguration: Configuration? = null

    /**
     * Construct an instance that contains the new multi-window mode and the new configuration with
     * the new multi-window mode applied.
     *
     * @param isInMultiWindowMode True if the activity is in multi-window mode.
     * @param newConfig The new configuration of the activity with the state {@param
     *   isInMultiWindowMode}.
     */
    @RequiresApi(26)
    public constructor(
        isInMultiWindowMode: Boolean,
        newConfig: Configuration
    ) : this(isInMultiWindowMode) {
        this.newConfiguration = newConfig
    }

    @get:RequiresApi(26)
    public val newConfig: Configuration
        /**
         * Gets the new [Configuration] of the with activity with the state [isInMultiWindowMode]
         * applied.
         *
         * Note that this is only valid on devices that are running API 26
         * ([android.os.Build.VERSION_CODES.O]) or higher.
         *
         * @return The new configuration of the activity with the state [isInMultiWindowMode].
         * @throws IllegalStateException if the new [Configuration] is not available (i.e., you are
         *   running on a device less that [android.os.Build.VERSION_CODES.O] which is when this
         *   information first became available).
         */
        get() {
            return checkNotNull(newConfiguration) {
                "MultiWindowModeChangedInfo must be constructed " +
                    "with the constructor that takes a Configuration to access the newConfig. " +
                    "Are you running on an API 26 or higher device that makes this " +
                    "information available?"
            }
        }
}
