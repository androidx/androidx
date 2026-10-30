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

package androidx.xr.glimmer.remote.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.xr.glimmer.remote.surface

@Suppress("RestrictedApiAndroidX") // remove once the API becomes public
@Sampled
@Composable
@RemoteComposable
fun StaticSurfaceSample() {
    RemoteBox(
        // Constant flags are resolved when the document is created, so this surface always looks
        // focused and never changes its state.
        modifier = RemoteModifier.surface(focused = true.rb)
    ) {
        RemoteText(
            text = "This is a surface".rs,
            color = Color.White.rc,
            fontSize = 24.rsp,
            modifier = RemoteModifier.padding(24.rdp),
        )
    }
}

@Suppress("RestrictedApiAndroidX") // remove once the API becomes public
@Sampled
@Composable
@RemoteComposable
fun HostStateSurfaceSample() {
    // A named remote boolean can be updated by the host at runtime, for example when
    // the host moves focus to the element that is displayed by this document.
    val focused = rememberNamedRemoteBoolean(name = "surfaceFocused", initialValue = false)
    RemoteBox(modifier = RemoteModifier.surface(focused = focused)) {
        RemoteText(
            text = "This is a focusable surface".rs,
            color = Color.White.rc,
            fontSize = 24.rsp,
            modifier = RemoteModifier.padding(24.rdp),
        )
    }
}

@Suppress("RestrictedApiAndroidX") // remove once the API becomes public
@Sampled
@Composable
@RemoteComposable
fun PlayerStateSurfaceSample() {
    val pressed = remember { MutableRemoteBoolean(false) }
    RemoteBox(
        // Update the pressed state from touch events on the player side, so the visual feedback
        // responds immediately.
        modifier =
            RemoteModifier.surface(pressed = pressed, focused = true.rb)
                .onTouchDown(valueChangeAction(pressed, true.rb))
                .onTouchUp(valueChangeAction(pressed, false.rb))
                .onTouchCancel(valueChangeAction(pressed, false.rb))
    ) {
        RemoteText(
            text = "This is a pressable surface".rs,
            color = Color.White.rc,
            fontSize = 24.rsp,
            modifier = RemoteModifier.padding(24.rdp),
        )
    }
}

@Suppress("RestrictedApiAndroidX") // remove once the API becomes public
@Sampled
@Composable
@RemoteComposable
fun HostActionSurfaceSample() {
    val pressed = remember { MutableRemoteBoolean(false) }
    RemoteBox(
        // The pressed state is updated on the player side, so the visual feedback responds
        // immediately, while the click itself is sent back to the host when the touch is released.
        // A cancelled touch only resets the pressed state and doesn't count as a click.
        modifier =
            RemoteModifier.surface(pressed = pressed, focused = true.rb)
                .onTouchDown(valueChangeAction(pressed, true.rb))
                .onTouchUp(
                    combinedAction(
                        // visual response
                        valueChangeAction(pressed, false.rb),
                        // host callback
                        hostAction(name = "onSurfaceClick".rs),
                    )
                )
                .onTouchCancel(valueChangeAction(pressed, false.rb))
    ) {
        RemoteText(
            text = "This is a clickable surface".rs,
            color = Color.White.rc,
            fontSize = 24.rsp,
            modifier = RemoteModifier.padding(24.rdp),
        )
    }
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun StaticSurfacePreview() {
    StaticSurfaceSample()
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun HostStateSurfacePreview() {
    HostStateSurfaceSample()
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun PlayerStateSurfacePreview() {
    PlayerStateSurfaceSample()
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun HostActionSurfacePreview() {
    HostActionSurfaceSample()
}
