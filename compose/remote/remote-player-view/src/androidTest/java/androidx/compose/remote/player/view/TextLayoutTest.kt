/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.player.core.RemoteComposeDocument
import androidx.compose.remote.player.core.platform.AndroidPaintContext
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.test.filters.SdkSuppress
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class TextLayoutTest() {
    @get:Rule val name = TestName()

    val mPlatform = AndroidxPlatformServices()

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)))
        }

    fun baseTest(writer: RemoteComposeWriter) {
        val buffer = writer.buffer()
        val bufferSize = writer.bufferSize()
        val doc = RemoteComposeDocument(ByteArrayInputStream(buffer, 0, bufferSize))

        val canvas = Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888))

        val remoteContext = AndroidRemoteContext()
        remoteContext.setPaintContext(AndroidPaintContext(remoteContext, canvas))
        doc.initializeContext(remoteContext)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
    }

    @Test
    fun testTextLayout() {
        val writer = RemoteComposeWriter(200, 200, "TextLayout", mPlatform)
        writer.root({
            val text1 = writer.addText("Hello")
            val text2 = writer.addText("")
            writer.column(
                RecordingModifier().background(Color.YELLOW).padding(8),
                BoxLayout.CENTER,
                BoxLayout.TOP,
                {
                    writer.textComponent(
                        RecordingModifier(),
                        text1,
                        Color.RED,
                        10f,
                        0,
                        400f,
                        "default",
                        3,
                        1,
                        1,
                    ) {}
                    writer.textComponent(
                        RecordingModifier(),
                        text2,
                        Color.RED,
                        10f,
                        0,
                        400f,
                        "serif",
                        3,
                        1,
                        1,
                    ) {}
                },
            )
        })
        baseTest(writer)
    }
}
