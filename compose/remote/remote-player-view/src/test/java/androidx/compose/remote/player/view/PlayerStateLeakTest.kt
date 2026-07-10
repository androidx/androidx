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

package androidx.compose.remote.player.view

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.view.platform.RemoteComposeView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Test helper subclass to expose protected mInner
class TestRemoteComposePlayer(context: Context) : RemoteComposePlayer(context) {
    val innerView: RemoteComposeView
        get() = mInner
}

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PlayerStateLeakTest {

    @Test
    fun testPlayerStateLeak_differentVariableOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = TestRemoteComposePlayer(context) // Use helper
        val displayInfo = CreationDisplayInfo(100, 100, 160)

        // Document 1: Registers "color.test" (gets ID 42), then "var_other" (gets ID 43)
        val doc1 = createTestDocument(displayInfo, registerColorFirst = true)

        // Document 2: Registers "var_other" (gets ID 42), then "color.test" (gets ID 43)
        val doc2 = createTestDocument(displayInfo, registerColorFirst = false)

        // 1. Load Document 1
        player.setDocument(doc1)
        player.setColor("color.test", 0xFF111111.toInt())

        // 2. Load Document 2 on the SAME player (simulating recomposition/update)
        player.setDocument(doc2)

        // 3. Update "color.test" by name. It should update ID 43 (in Doc 2).
        val targetColor = 0xFFFFFFFF.toInt()
        player.setColor("color.test", targetColor)

        // 4. Assertions using public APIs (cast to AndroidRemoteContext)
        val remoteContext = player.innerView.remoteContext as AndroidRemoteContext
        val state = remoteContext.mRemoteComposeState

        // Look up IDs dynamically instead of hardcoding
        val colorVarId = remoteContext.getVariableId("color.test")
        val otherVarId = remoteContext.getVariableId("var_other")

        val colorAtId = state.getColor(colorVarId)
        val colorAtOtherId = state.getColor(otherVarId)

        // Verify the fix
        assertEquals("Color variable should be updated to targetColor", targetColor, colorAtId)
        assertNotEquals(
            "Other variable should NOT be overwritten with color value",
            targetColor,
            colorAtOtherId,
        )
        assertNotEquals(
            "Stale color from Doc 1 should not leak to other variable",
            0xFF111111.toInt(),
            colorAtOtherId,
        )
    }

    private fun createTestDocument(
        displayInfo: CreationDisplayInfo,
        registerColorFirst: Boolean,
    ): RemoteDocument {
        val rcPlatform = androidx.compose.remote.core.RcPlatformServices.None
        val factory = RemoteComposeWriterFactory { creationDisplayInfo, profile, _ ->
            RemoteComposeWriter(
                profile,
                RemoteComposeBuffer(),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        }
        val profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                rcPlatform,
                factory,
            )
        val writer = profile.create(displayInfo, null)

        writer.beginGlobal()
        if (registerColorFirst) {
            writer.addNamedColor("color.test", 0xFF111111.toInt())
            writer.addNamedFloat("var_other", 1.0f)
        } else {
            writer.addNamedFloat("var_other", 1.0f)
            writer.addNamedColor("color.test", 0xFF222222.toInt())
        }
        writer.endGlobal()

        writer.root {
            // Minimal layout to pass validation
        }

        return RemoteDocument(writer.encodeToByteArray())
    }
}
