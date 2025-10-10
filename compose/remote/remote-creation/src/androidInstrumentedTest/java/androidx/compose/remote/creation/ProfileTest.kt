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
import androidx.compose.remote.core.Profiles
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.creation.profile.PlatformProfile
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun testAndroidx() {
        val androidx = PlatformProfile.ANDROIDX

        assertEquals(CoreDocument.DOCUMENT_API_LEVEL, androidx.apiLevel)
        assertEquals(Profiles.PROFILE_ANDROIDX, androidx.operationsProfiles)

        val writer = androidx.create(100, 100, "test")
        assertIs<RemoteComposeWriter>(writer)

        assertIs<AndroidxPlatformServices>(writer.mPlatform)
    }

    @Test
    fun testWidgetsv6() {
        val widgets = PlatformProfile.WIDGETS_V6

        assertEquals(6, widgets.apiLevel)
        assertEquals(Profiles.PROFILE_BASELINE, widgets.operationsProfiles)

        val writer = widgets.create(100, 100, "test")
        assertIs<WidgetsProfileWriterV6>(writer)

        assertIs<AndroidxPlatformServices>(writer.mPlatform)
    }

    @Test
    fun testWidgetsv6Text() {
        val widgets = PlatformProfile.WIDGETS_V6

        val writer = widgets.create(100, 100, "test")

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
        val androidx = PlatformProfile.ANDROIDX

        val writer = androidx.create(100, 100, "test")

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
