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

package androidx.compose.runtime

import kotlin.jvm.JvmStatic

@ExperimentalComposeApi
public object ComposeRuntimeFlags {
    /**
     * Constant to control the default value of [isLinkBufferComposerEnabled], extracted for
     * convenience.
     */
    @Suppress("FeatureFlagSetup") private const val isLinkBufferComposerEnabledByDefault = false

    /**
     * A feature flag than can be used to enable the link-list based slot table implementation
     * instead of the gap buffer based slot table. The linked-list implementation is designed to
     * improve performance primarily when deleting, moving, and reordering composable content by
     * reducing the amount of internal array copy operations.
     *
     * This flag is **disabled** by default.
     *
     * This flag *must* be set to the desired value prior to calling `setContent` the first time and
     * never change thereafter as the runtime does not support having multiple implementations of
     * the slot table simultaneously.
     *
     * For performance reasons, this flag is also configured via proguard rule for release builds so
     * that production builds of apps can ship with only one implementation of the slot table.
     * Leaving both implementations in a release build of an Android app will lead to measurable
     * performance differences as it prevents R8 from devirtualizing calls in the Compose runtime.
     * (Apps that do not currently optimize release builds with R8 are less likely to notice
     * performance differences by having two parallel slot table implementations, though we continue
     * to recommend enabling R8 as it makes a big difference to Compose's performance.)
     *
     * To set this flag in release builds with R8, add this configuration to your module's proguard
     * rules:
     * ```
     * -assumevalues public class androidx.compose.runtime.ComposeRuntimeFlags {
     *     static boolean isLinkBufferComposerEnabled() return true;
     * }
     * ```
     *
     * Assigning to this property in a build that has been optimized by R8 will always no-op
     * regardless of whether you declare the configuration rule in your app. In minified builds,
     * this flag can only be configured by R8.
     *
     * The Compose runtime ships with a default proguard configuration rule that matches this flag's
     * default (disabled) value that ships with the library. Changing this field programmatically in
     * an app optimized by R8 will only affect debug builds without the matching proguard rule. In
     * R8 release builds, the proguard configuration always takes precedence and programmatic
     * assignments to this flag become no-ops.
     */
    // TODO: b/485957718
    @JvmStatic
    @Suppress("FeatureFlagSetup")
    public var isLinkBufferComposerEnabled: Boolean = isLinkBufferComposerEnabledByDefault
        get() = if (isMinified) isLinkBufferComposerEnabledByDefault else field
        set(value) {
            if (!isMinified) {
                field = value
            }
        }

    /**
     * Assigned to `true` via proguard rule. When the Runtime is used with an application's release
     * build, assignments to [isLinkBufferComposerEnabled] are ignored.
     */
    @Suppress("FeatureFlagSetup") private var isMinified = false
}
