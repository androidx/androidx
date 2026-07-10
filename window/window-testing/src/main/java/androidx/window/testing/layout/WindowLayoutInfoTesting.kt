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
@file:JvmName("WindowLayoutInfoTesting")

package androidx.window.testing.layout

import androidx.window.layout.DisplayFeature
import androidx.window.layout.WindowLayoutInfo

/**
 * Returns a [WindowLayoutInfo] with default values for testing.
 *
 * @param displayFeatures a [List] of [DisplayFeature], the default value is an empty [List].
 * @param engagementModes a [Set] of [WindowLayoutInfo.EngagementMode], the default value is
 *   [WindowLayoutInfo.EngagementMode.VISUALS_ON] and [WindowLayoutInfo.EngagementMode.AUDIO_ON].
 * @return [WindowLayoutInfo] with matching parameters.
 * @see WindowLayoutInfoPublisherRule.overrideWindowLayoutInfo
 */
@Deprecated(
    message =
        "WindowLayoutInfo.EngagementMode is deprecated. Use TestWindowLayoutInfo without engagementModes",
    replaceWith = ReplaceWith("TestWindowLayoutInfo(displayFeatures)"),
)
@Suppress("FunctionName")
@JvmName("createWindowLayoutInfo")
public fun TestWindowLayoutInfo(
    displayFeatures: List<DisplayFeature> = emptyList(),
    @Suppress("DEPRECATION") engagementModes: Set<WindowLayoutInfo.EngagementMode>,
): WindowLayoutInfo {
    @Suppress("DEPRECATION")
    return WindowLayoutInfo(displayFeatures, engagementModes)
}

/**
 * Returns a [WindowLayoutInfo] with default values for testing.
 *
 * @param displayFeatures a [List] of [DisplayFeature], the default value is an empty [List].
 * @return [WindowLayoutInfo] with matching parameters.
 * @see WindowLayoutInfoPublisherRule.overrideWindowLayoutInfo
 */
@Suppress("FunctionName")
@JvmName("createWindowLayoutInfo")
@JvmOverloads
public fun TestWindowLayoutInfo(
    displayFeatures: List<DisplayFeature> = emptyList()
): WindowLayoutInfo {
    return WindowLayoutInfo(displayFeatures)
}
