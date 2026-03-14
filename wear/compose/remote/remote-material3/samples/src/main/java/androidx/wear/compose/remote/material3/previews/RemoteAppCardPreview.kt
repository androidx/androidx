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

package androidx.wear.compose.remote.material3.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteAppCard
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
fun RemoteAppCardPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteAppCardDefault() } }

@WearPreviewDevices
@Composable
fun RemoteAppCardWithAppNameTimeTitlePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteAppCardWithAppNameTimeTitle() } }

@WearPreviewDevices
@Composable
fun RemoteAppCardWithAppNameTitleSubtitlePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteAppCardWithAppNameTitleSubtitle() } }

@Composable
@RemoteComposable
fun RemoteAppCardDefault() {
    RemoteAppCard(
        onClick = Action.Empty,
        appName = { RemoteText("App Name".rs) },
        time = { RemoteText("now".rs) },
        appImage = { RemoteIcon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
        title = { RemoteText("Card Title".rs) },
    ) {
        RemoteText("Card Content".rs)
    }
}

@Composable
@RemoteComposable
fun RemoteAppCardWithAppNameTimeTitle() {
    RemoteAppCard(
        onClick = Action.Empty,
        appName = { RemoteText("App Name".rs) },
        time = { RemoteText("now".rs) },
        title = { RemoteText("Card Title".rs) },
    ) {
        RemoteText("Card Content".rs)
    }
}

@Composable
@RemoteComposable
fun RemoteAppCardWithAppNameTitleSubtitle() {
    RemoteAppCard(
        onClick = Action.Empty,
        appName = { RemoteText("App Name".rs) },
        title = { RemoteText("Card Title".rs) },
    ) {
        RemoteText("Card Content".rs)
    }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize().padding(16.rdp),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
