/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * A collection of runtime feature flags used to guard behavior changes, migrations, or experimental
 * paths within the `androidx.activity` package and its related integrations.
 *
 * These flags are **mutable** to support temporary toggling, staged rollouts, and regression
 * isolation. They are **not API-stable** and may be changed or removed at any time.
 *
 * All flags default to values that reflect the intended behavior for current stable releases. If
 * you encounter a regression that can be mitigated by toggling a flag, please file a bug
 * referencing the flag name.
 *
 * **Usage:**
 *
 * Flags should be set as early as possible, ideally during application startup, before any Activity
 * or Compose integration code runs. Changing a flag at runtime may lead to inconsistent behavior.
 *
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         ActivityFlags.useEnableDisableForOnBackPressedLifecycle = true
 *         super.onCreate()
 *     }
 * }
 * ```
 */
@ExperimentalActivityApi
public object ActivityFlags {

    /**
     * Toggles how [OnBackPressedDispatcher.addCallback] responds to lifecycle changes.
     *
     * When `true` (default), callbacks are **enabled and disabled** in place without being
     * re-added. This preserves handler ordering across lifecycle transitions and improves
     * interoperability with APIs such as `NavigationEventDispatcher` and `BackHandler`.
     *
     * When `false`, [OnBackPressedDispatcher] uses the legacy behavior of **adding and removing**
     * callbacks as the associated [LifecycleOwner] moves between [Lifecycle.Event.ON_START] and
     * [Lifecycle.Event.ON_STOP]. This can reorder the callback stack when lifecycle transitions
     * occur, which may affect the relative priority of registered handlers.
     *
     * This flag is intended for testing and migration. It may be removed once the legacy behavior
     * is fully retired.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    public var isOnBackPressedLifecycleOrderMaintained: Boolean = true
}
