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

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Limits

/**
 * Configures remote-core document parsing limits for external/encoded image references.
 *
 * Enables URL and file image reference decoding flags so documents containing remote or file-backed
 * image URLs can be parsed successfully, with actual decoding deferred to the player's image
 * loaders.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteImageSupport {
    public fun enableEncodedImageReferences() {
        Limits.ENABLE_IMAGE_URLS = true
        Limits.ENABLE_IMAGE_FILES = true
    }
}
