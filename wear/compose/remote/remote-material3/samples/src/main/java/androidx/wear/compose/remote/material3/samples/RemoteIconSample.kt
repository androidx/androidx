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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteIconSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteIcon(
        modifier = modifier,
        imageVector = ImageVector.vectorResource(R.drawable.gs_map_wght500rond100_vd_theme_24),
        contentDescription = null,
    )
}
