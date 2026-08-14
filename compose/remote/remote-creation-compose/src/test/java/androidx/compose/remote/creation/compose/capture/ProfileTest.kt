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

package androidx.compose.remote.creation.compose.capture

import androidx.collection.intSetOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class ProfileTest {
    private val displayInfo = CreationDisplayInfo(100, 100, 160)

    @Test
    fun createCustomProfile_defaultValues() {
        val profile = createCustomProfile()

        assertThat(profile.apiLevel).isEqualTo(CoreDocument.DOCUMENT_API_LEVEL)
        assertThat(profile.operationsProfiles).isEqualTo(RcProfiles.PROFILE_ANDROIDX)
        assertThat(profile.platform).isInstanceOf(AndroidxRcPlatformServices::class.java)

        val writer = profile.create(displayInfo, null)
        assertThat(writer).isInstanceOf(RemoteComposeWriterAndroid::class.java)
    }

    @Test
    fun createCustomProfile_customParameters() {
        val customOps = intSetOf(Operations.HEADER, Operations.DRAW_RECT, Operations.DRAW_LINE)
        val profile =
            createCustomProfile(
                apiLevel = 7,
                profileFlags = RcProfiles.PROFILE_WEAR_WIDGETS,
                supportedOperations = customOps,
            )

        assertThat(profile.apiLevel).isEqualTo(7)
        assertThat(profile.operationsProfiles).isEqualTo(RcProfiles.PROFILE_WEAR_WIDGETS)
        assertThat(profile.supportedOperations)
            .containsExactly(Operations.HEADER, Operations.DRAW_RECT, Operations.DRAW_LINE)

        val writer = profile.create(displayInfo, null)
        assertThat(writer).isInstanceOf(RemoteComposeWriterAndroid::class.java)
        assertThat(profile.supportedOperations).doesNotContain(Operations.DRAW_CIRCLE)
    }
}
