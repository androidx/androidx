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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
@RemoteComposable
fun TestDrawContentDemo() {
    val rcFloat = 200f.rf
    val visibility = rememberMutableRemoteInt(Component.Visibility.VISIBLE)

    RemoteRow {
        RemoteBox(
            modifier =
                RemoteModifier.Companion.height(rcFloat)
                    .width(rcFloat)
                    .border(1.rdp, Color.Companion.Red.rc)
                    .clickable(
                        ValueChange(remoteState = visibility, updatedValue = (visibility + 1) % 2)
                    )
        ) {
            RemoteText(text = "Hello world!")
        }
        RemoteBox(
            modifier =
                RemoteModifier.Companion.height(rcFloat)
                    .width(rcFloat)
                    .visibility(visibility)
                    .border(1.rdp, Color.Companion.Green.rc)
        ) {
            RemoteText(text = "Hello world!")
        }
        RemoteBox(
            modifier =
                RemoteModifier.Companion.height(rcFloat)
                    .width(rcFloat)
                    .border(1.rdp, Color.Companion.Blue.rc)
                    .drawWithContent {
                        drawContent()
                        val paint = RemotePaint.Companion()
                        paint.color = RemoteColor.Companion.rgb(1f, 1f, 1f, 0f)
                        drawCircle(paint, center, 40.rf)
                        rotate(30f.rf) { scale(0.5f.rf) { drawContent() } }
                    }
        ) {
            RemoteText(text = "Hello world!")
        }
    }
}
