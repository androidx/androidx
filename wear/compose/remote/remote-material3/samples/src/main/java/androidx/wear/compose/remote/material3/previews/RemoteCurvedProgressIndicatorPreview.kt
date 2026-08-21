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

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteCurvedProgressIndicator
import androidx.wear.compose.remote.material3.RemoteProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorAnimatedSample
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import java.text.DecimalFormat

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteCurvedProgressEnabled() } }
}

@Composable
fun RemoteCurvedProgressEnabled() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
    )
}

@Composable
fun RemoteCurvedProgressEnabledRtl() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        reverseDirection = true.rb,
    )
}

@Composable
fun RemoteCurvedProgressNoGap() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        gapAngleDegrees = 0f.rf,
    )
}

// High Progress - Remaining Track Segment No Collapse Demos
@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressNoCollapse99ProgressPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressNoCollapse99Progress() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressNoCollapse100ProgressPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressNoCollapse100Progress() }
    }
}

@Composable
fun RemoteCurvedProgressNoCollapse99Progress() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.99f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapsible = false.rb,
    )
}

@Composable
fun RemoteCurvedProgressNoCollapse100Progress() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 1.0f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapsible = false.rb,
    )
}

@Composable
fun RemoteCurvedProgressIndicatorCollapse4Progress() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.04f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapsible = true.rb,
    )
}

@Composable
fun RemoteCurvedProgressIndicatorCollapse96Progress() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.96f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapsible = true.rb,
    )
}

@Composable
fun RemoteCurvedProgressIndicatorCollapseNoFadeOut96Progress() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.96f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapsible = true.rb,
        dotFadeOutFraction = 0f.rf,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCollapse4ProgressPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCollapse4Progress() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCollapse96ProgressPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCollapse96Progress() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCollapseNoFadeOut96ProgressPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCollapseNoFadeOut96Progress() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCustomColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCustomColor() }
    }
}

@Composable
fun RemoteCurvedProgressIndicatorCustomColor() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        colors =
            RemoteProgressIndicatorDefaults.colors(
                indicatorColor = Color.Red.rc,
                trackColor = Color.Blue.rc,
            ),
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorDisabled() }
    }
}

@Composable
fun RemoteCurvedProgressIndicatorDisabled() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        enabled = false.rb,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorOutroLoopPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorOutroLoop() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorIntroLoopPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorIntroLoop() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCountdownIntroLoopPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCountdownIntroLoop() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCountdownOutroLoopPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCountdownOutroLoop() }
    }
}

@WearPreviewDevices
@Composable
public fun RemoteCurvedProgressIndicatorAnimatedPreview() {
    RemoteContentPreview { Container { RemoteCurvedProgressIndicatorAnimatedSample() } }
}

@Composable
fun RemoteCurvedProgressIndicatorOutroLoop() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 6f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Ramp progress from 95% to 100% over 5 seconds, then hold at 100% for 1 second.
    // The outro animation triggers at 850ms remaining, which for a 5-minute timer (300,000ms)
    // is at ~99.71% progress.
    val rampProgress = clamp(t / (5f.rf / 6f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.95f.rf, 1.0000f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            totalTimerDurationMillis = 300_000L,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteCurvedProgressIndicatorIntroLoop() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 12f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Hold at 0% for 2 seconds to clearly show the intro animation scaling in the dot,
    // then ramp smoothly from 0% -> 7% over the next 8 seconds and hold for 2 seconds.
    val rampProgress = clamp((t - (2.0f.rf / 12f.rf)) / (8.0f.rf / 12f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.00f.rf, 0.07f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            totalTimerDurationMillis = 66_667L,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteCurvedProgressIndicatorCountdownIntroLoop() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 12f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Hold at 100% for 2 seconds to clearly show the intro animation scaling in the dot,
    // then ramp smoothly from 100% -> 93% over the next 8 seconds and hold for 2 seconds.
    val rampProgress = clamp((t - (2.0f.rf / 12f.rf)) / (8.0f.rf / 12f.rf), 0f.rf, 1f.rf)
    val progress = lerp(1.00f.rf, 0.93f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            totalTimerDurationMillis = 66_667L,
            countDown = true.rb,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteCurvedProgressIndicatorCountdownOutroLoop() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 6f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Ramp progress from 5% to 0% over 5 seconds, then hold at 0% for 1 second.
    // The outro animation triggers at 850ms remaining, which for a 5-minute timer (300,000ms)
    // is at ~0.29% progress.
    val rampProgress = clamp(t / (5f.rf / 6f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.05f.rf, 0.0000f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            totalTimerDurationMillis = 300_000L,
            countDown = true.rb,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorExpandFromZeroPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorExpandFromZero() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCollapseToZeroPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCollapseToZero() }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorNoCollapsePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorNoCollapse() }
    }
}

@Composable
fun RemoteCurvedProgressIndicatorExpandFromZero() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 6f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Ramp progress from 0% -> 50% over 5 seconds, then hold at 50% for 1 second.
    // The active dot expands linearly from 0.
    val rampProgress = clamp(t / (5f.rf / 6f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.00f.rf, 0.50f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            dotCollapsible = true.rb,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteCurvedProgressIndicatorCollapseToZero() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 6f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Ramp progress from 50% -> 100% over 5 seconds, then hold at 100% for 1 second.
    // The remaining track collapses linearly.
    val rampProgress = clamp(t / (5f.rf / 6f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.50f.rf, 1.00f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            dotCollapsible = true.rb,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteCurvedProgressIndicatorNoCollapse() {
    val time = RemoteTime().ContinuousSec()
    val cycleDuration = 6f.rf
    val t = (time % cycleDuration) / cycleDuration
    // Ramp progress from 50% -> 100% over 5 seconds, then hold at 100% for 1 second.
    // The remaining track collapses linearly.
    val rampProgress = clamp(t / (5f.rf / 6f.rf), 0f.rf, 1f.rf)
    val progress = lerp(0.50f.rf, 1.00f.rf, rampProgress)
    RemoteBox(modifier = RemoteModifier.size(150.rdp), contentAlignment = RemoteAlignment.Center) {
        RemoteCurvedProgressIndicator(
            modifier = RemoteModifier.fillMaxSize(),
            progress = progress,
            startAngle = 135f.rf,
            sweepAngle = 90f.rf,
            dotCollapsible = false.rb,
        )
        val livePctString = (progress * 100f.rf).toRemoteString(DecimalFormat("0.00")) + "%".rs
        RemoteText(
            text = livePctString,
            color = Color.White.rc,
            fontSize = 10.rsp,
            textAlign = TextAlign.Center,
        )
    }
}
