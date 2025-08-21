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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.creation.profile.PlatformProfile
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.Test

class ProfileTest {
    @Test
    fun testAndroidx() {
        val androidx = PlatformProfile.ANDROIDX

        assertEquals(CoreDocument.DOCUMENT_API_LEVEL, androidx.apiLevel)
        assertEquals(Operations.PROFILE_ANDROIDX, androidx.operationsProfiles)

        val writer = androidx.create(100, 100, "test")
        assertIs<RemoteComposeWriter>(writer)

        assertIs<AndroidxPlatformServices>(writer.mPlatform)
    }

    @Test
    fun testWidgetsv6() {
        val androidx = PlatformProfile.WIDGETS_V6

        assertEquals(6, androidx.apiLevel)
        assertEquals(Operations.PROFILE_BASELINE, androidx.operationsProfiles)

        val writer = androidx.create(100, 100, "test")
        assertIs<WidgetsProfileWriterV6>(writer)

        assertIs<AndroidxPlatformServices>(writer.mPlatform)
    }
}
