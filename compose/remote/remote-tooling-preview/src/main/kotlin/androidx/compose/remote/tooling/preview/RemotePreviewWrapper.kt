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

package androidx.compose.remote.tooling.preview

import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider

/**
 * Default [PreviewWrapperProvider] of Remote Compose, that uses [RcPlatformProfiles.ANDROIDX]
 * profile.
 *
 * To apply this wrapper to a Composable Preview, use the
 * [androidx.compose.ui.tooling.preview.PreviewWrapper] annotation:
 * ```kotlin
 * @Preview
 * @PreviewWrapper(RemotePreviewWrapper::class)
 * @Composable
 * fun MyRemotePreview() {
 *     // remote compose content
 * }
 * ```
 */
public class RemotePreviewWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        RemoteContentPreview(profile = RcPlatformProfiles.ANDROIDX, content = content)
    }
}
