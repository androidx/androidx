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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.deltaFromReferenceInSeconds
import androidx.compose.remote.creation.compose.state.floor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteLong
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteHorizontalPageIndicator
import androidx.wear.compose.remote.material3.RemoteVerticalPageIndicator
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.rememberRemotePageIndicatorState
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
fun RemoteHorizontalPageIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
        RemoteHorizontalPageIndicator(state = state, modifier = RemoteModifier.fillMaxSize())
    }

@WearPreviewDevices
@Composable
fun RemoteVerticalPageIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
        RemoteVerticalPageIndicator(state = state, modifier = RemoteModifier.fillMaxSize())
    }

@WearPreviewDevices
@Composable
fun RemoteHorizontalPageIndicatorAnimatedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        RemotePageIndicatorAnimatedPreviewHelper(
            isHorizontal = true,
            rememberKey = "preview_start_time_horiz_indicator",
        )
    }

@WearPreviewDevices
@Composable
fun RemoteVerticalPageIndicatorAnimatedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        RemotePageIndicatorAnimatedPreviewHelper(
            isHorizontal = false,
            rememberKey = "preview_start_time_vert_indicator",
        )
    }

@Composable
private fun RemotePageIndicatorAnimatedPreviewHelper(isHorizontal: Boolean, rememberKey: String) {
    val startTime = rememberNamedRemoteLong(rememberKey, System.currentTimeMillis())
    val animTime = -deltaFromReferenceInSeconds(startTime) * 2f.rf

    // Continuous scroll simulator (page + offset fraction) cycle over 8 scaled seconds
    val cycleDuration = 8f.rf
    val t = animTime - floor(animTime / cycleDuration) * cycleDuration

    val conditions =
        listOf(
            1f.rf to 0f.rf,
            2f.rf to t - 1f.rf,
            3f.rf to 1f.rf,
            4f.rf to t - 2f.rf,
            5f.rf to 2f.rf,
            6f.rf to 7f.rf - t,
            7f.rf to 1f.rf,
        )
    val pagePos =
        conditions.foldRight(8f.rf - t) { (threshold, outcome), acc ->
            t.isLessThan(threshold).select(outcome, acc)
        }

    val selectedPage = floor(pagePos)
    val pageOffset = pagePos - selectedPage

    val state =
        rememberRemotePageIndicatorState(
            selectedPage = selectedPage.toRemoteInt(),
            pageOffset = pageOffset,
            pageCount = 3,
        )

    if (isHorizontal) {
        RemoteHorizontalPageIndicator(state = state, modifier = RemoteModifier.fillMaxSize())
    } else {
        RemoteVerticalPageIndicator(state = state, modifier = RemoteModifier.fillMaxSize())
    }
}
