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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Collection of flags used to guard experimental or new feature support in
 * `wear.compose.remote.material3`.
 */
@RestrictTo(LIBRARY_GROUP)
public object RemoteMaterial3Flags {
    /**
     * When true, [RemoteMaterialTheme.colorScheme] defaults to [dynamicRemoteColorScheme] (when no
     * color scheme has been provided, e.g. `RemoteMaterialTheme {}`) instead of the named
     * [RemoteColorScheme] (`"WearM3.*"`).
     *
     * An explicitly provided [RemoteColorScheme] is always used as-is regardless of this flag.
     */
    // TODO(b/571412247): Remove this flag once themed colors are stable.
    @field:Suppress("MutableBareField") @JvmField public var isThemedColorsEnabled: Boolean = false
}
