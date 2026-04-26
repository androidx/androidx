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
package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun SlantedButtonPreview() {
    val context = LocalContext.current
    RemoteDocPreview(SlantedButtonDemo())
}

@Suppress("RestrictedApiAndroidX")
fun SlantedButtonDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        root {
            row(
                Modifier.fillMaxSize().background(0xFFEEEEEE.toInt()),
                horizontal = ColumnLayout.CENTER,
                vertical = ColumnLayout.CENTER,
            ) {
                slantedButton("Slanted Button", 0xFF6200EE.toInt())
                box(Modifier.height(40f))
                slantedButton("Secondary Action", 0xFF03DAC6.toInt())
                box(Modifier.height(40f))
                slantedButton("Danger Zone", 0xFFB00020.toInt())
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.slantedButton(label: String, color: Int) {
    val width = 450f
    val height = 120f
    val slant = 40f

    box(
        Modifier.size(width.toInt(), height.toInt()).drawWithContent {
            // This block runs inside RemoteComposeWriter scope

            // Create a slanted path for the button background
            val path = pathCreate(slant, 0f)
            pathAppendLineTo(path, width, 0f)
            pathAppendLineTo(path, width - slant, height)
            pathAppendLineTo(path, 0f, height)
            pathAppendClose(path)

            // 1. Draw background behind the content
            painter.setColor(color).setStyle(Paint.Style.FILL).commit()
            drawPath(path)

            // 2. Draw the component's content (the text defined in the box block)
            // This is triggered by our new drawContent() modifier logic
            drawComponentContent()

            // 3. Draw a border on top of everything
            painter.setColor(0x44000000).setStyle(Paint.Style.STROKE).setStrokeWidth(2f).commit()
            drawPath(path)
        },
        horizontal = BoxLayout.CENTER,
        vertical = BoxLayout.CENTER,
    ) {
        text(label, color = 0xFFFFFFFF.toInt(), fontSize = 36f)
    }
}
