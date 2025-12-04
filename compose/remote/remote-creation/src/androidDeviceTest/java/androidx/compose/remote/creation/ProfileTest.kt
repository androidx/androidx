/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.creation

import android.graphics.Color
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun testAndroidx() {
        val androidx = RcPlatformProfiles.ANDROIDX

        assertEquals(CoreDocument.DOCUMENT_API_LEVEL, androidx.apiLevel)
        assertEquals(RcProfiles.PROFILE_ANDROIDX, androidx.operationsProfiles)

        val writer = androidx.create(CreationDisplayInfo(100, 100, 1f), "test")
        assertIs<RemoteComposeWriter>(writer)

        assertIs<AndroidxRcPlatformServices>(writer.mPlatform)
    }

    @Test
    fun testWidgetsv6() {
        val widgets = RcPlatformProfiles.WIDGETS_V6

        assertEquals(6, widgets.apiLevel)
        assertEquals(RcProfiles.PROFILE_BASELINE, widgets.operationsProfiles)

        val writer = widgets.create(CreationDisplayInfo(100, 100, 1f), "test")
        assertIs<WidgetsProfileWriterV6>(writer)

        assertIs<AndroidxRcPlatformServices>(writer.mPlatform)
    }

    @Test
    fun testWidgetsv6Text() {
        val widgets = RcPlatformProfiles.WIDGETS_V6

        val writer = widgets.create(CreationDisplayInfo(100, 100, 1f), "test")

        val hello = writer.textCreateId("Hello")

        writer.startTextComponent(
            RecordingModifier(),
            hello,
            Color.WHITE,
            10f,
            0,
            400f,
            null,
            0,
            0,
            Integer.MAX_VALUE,
        )

        assertTrue(writer.encodeToByteArray().isNotEmpty())

        // Fails with dynamic size
        val fontSizeVar = writer.addFloatConstant(10f)
        assertFailsWith<IllegalArgumentException>("Invalid alpha in V6") {
            writer.startTextComponent(
                RecordingModifier(),
                hello,
                Color.WHITE,
                fontSizeVar,
                0,
                400f,
                null,
                0,
                0,
                Integer.MAX_VALUE,
            )
        }
    }

    @Test
    fun testAndroidXText() {
        val androidx = RcPlatformProfiles.ANDROIDX

        val writer = androidx.create(CreationDisplayInfo(100, 100, 1f), "test")

        val hello = writer.textCreateId("Hello")

        writer.startTextComponent(
            RecordingModifier(),
            hello,
            Color.WHITE,
            10f,
            0,
            400f,
            null,
            0,
            0,
            Integer.MAX_VALUE,
        )

        // Works with dynamic size
        val fontSizeVar = writer.addFloatConstant(10f)
        writer.startTextComponent(
            RecordingModifier(),
            hello,
            Color.WHITE,
            fontSizeVar,
            0,
            400f,
            null,
            0,
            0,
            Integer.MAX_VALUE,
        )

        assertTrue(writer.encodeToByteArray().isNotEmpty())
    }
}
