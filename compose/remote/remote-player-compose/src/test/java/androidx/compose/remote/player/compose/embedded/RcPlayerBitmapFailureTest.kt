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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerBitmapFailureTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun nestedRelativeUriIsSkippedDuringSetupTraversal() {
        val document = CoreDocument(RemoteClock.SYSTEM)

        val bitmapId = 42
        val bitmapData =
            BitmapData(
                bitmapId,
                BitmapData.TYPE_PNG_8888,
                100.toShort(),
                BitmapData.ENCODING_URL,
                100.toShort(),
                "https://example.com/test.png".toByteArray(),
            )

        val boxLayout = BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, BoxLayout.START, BoxLayout.TOP)
        boxLayout.getList().add(bitmapData)
        document.operations.add(boxLayout)

        val playerState = RcPlayerState(document)
        val context =
            playerState.inspectableElements.first { it.name == "remoteContext" }.value
                as AndroidRemoteContext

        // The image slot in RemoteComposeState should NOT contain a decoded bitmap yet (lazy
        // decode)
        assertThat(context.mRemoteComposeState.containsId(bitmapId)).isFalse()

        // But the BitmapData metadata object should be registered
        assertThat(context.getObject(bitmapId)).isEqualTo(bitmapData)
    }

    @Test
    fun failedBitmapDecodeIsCachedAndNotRetriedOnSubsequentCalls() {
        var applyCount = 0
        val bitmapId = 42
        val corruptBitmapData =
            object :
                BitmapData(
                    bitmapId,
                    BitmapData.TYPE_PNG_8888,
                    10.toShort(),
                    BitmapData.ENCODING_INLINE,
                    10.toShort(),
                    byteArrayOf(1, 2, 3, 4),
                ) {
                override fun apply(context: RemoteContext) {
                    applyCount++
                    throw RuntimeException("Simulated decode failure")
                }
            }

        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(corruptBitmapData)
                operations.add(NamedVariable(bitmapId, NamedVariable.IMAGE_TYPE, "USER:badImage"))
            }

        val playerState = RcPlayerState(document)
        assertThat(playerState.bitmapState("badImage").value).isNull()
        assertThat(applyCount).isEqualTo(1)

        // Subsequent lookups must not re-run apply() for a known-bad bitmap
        assertThat(playerState.bitmapState("badImage").value).isNull()
        assertThat(applyCount).isEqualTo(1)
    }
}
