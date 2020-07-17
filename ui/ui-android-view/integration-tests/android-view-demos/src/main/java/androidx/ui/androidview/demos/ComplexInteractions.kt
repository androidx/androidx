/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.androidview.demos

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.ui.core.ContextAmbient
import androidx.ui.core.setContent
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory
import androidx.compose.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.material.Button
import androidx.ui.viewinterop.AndroidView

// TODO(b/158099918): Add this demo to AndroidViewDemos.kt once b/158099918 has been resolved.
@Suppress("unused")
val ComplexTouchInterop = DemoCategory("Complex Touch Interop", listOf(
    ComposableDemo("ReproOffsetIssue") { ComposeInAndroidInComposeEtcTargetingDemo() }
))

@Composable
fun ComposeInAndroidInComposeEtcTargetingDemo() {
    val context = ContextAmbient.current
    Column {
        Text("In this demo, there is a compose button inside Android, which is inside Compose, " +
                "which inside Android... and on so on for a few times.  The conversions of " +
                "pointer input events at every level still work.")
        AndroidWithCompose(context) {
            AndroidWithCompose(context) {
                AndroidWithCompose(context) {
                    Button(onClick = { }) {
                        Text("Click me")
                    }
                }
            }
        }
    }
}

@Composable
fun AndroidWithCompose(context: Context, children: @Composable () -> Unit) {
    val anotherLayout = FrameLayout(context).also { view ->
        view.setContent(Recomposer.current()) {
            children()
        }
        view.setPadding(50, 50, 50, 50)
    }
    AndroidView(anotherLayout)
}