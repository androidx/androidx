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

package androidx.privacysandbox.ui.core

import androidx.annotation.RestrictTo

/**
 * List of all supported internal API versions (Client-Core communication).
 *
 * NEVER REMOVE / MODIFY RELEASED VERSIONS: That could break compatibility of client library built
 * with previous/future library version.
 */
// TODO(b/406975359): Add min_supported_version
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class SdkRuntimeUiLibVersions(public val apiLevel: Int) {

    V1(apiLevel = 1);

    public companion object {
        public val CURRENT_VERSION: SdkRuntimeUiLibVersions = values().maxBy { v -> v.apiLevel }
    }
}
