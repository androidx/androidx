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

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RootContentBehavior content scaling for raw draw-list documents. The documents are 100x100 and
 * fill themselves red, so the red area shows where the content was placed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerRootContentBehaviorTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun scaleFitCentresTheContent() {
        // A 200x100 player fits 100x100 content at scale 1, centred: x from 50 to 150.
        val bitmap = play(document(RootContentBehavior.SCALE_FIT))

        assertNotRed(bitmap, 25, 50)
        assertRed(bitmap, 100, 50)
        assertNotRed(bitmap, 175, 50)
    }

    @Test
    fun scaleFillBoundsStretchesTheContent() {
        val bitmap = play(document(RootContentBehavior.SCALE_FILL_BOUNDS))

        assertRed(bitmap, 25, 50)
        assertRed(bitmap, 175, 50)
    }

    @Test
    fun withoutScalingTheContentKeepsThePlayerSize() {
        // No RootContentBehavior: the document takes the player's size and draws unscaled.
        val bitmap =
            play(
                buildDocument {
                    painter.setColor(AndroidColor.RED).commit()
                    writer.drawRect(0f, 0f, 100f, 100f)
                }
            )

        assertRed(bitmap, 25, 50)
        assertNotRed(bitmap, 175, 50)
    }

    // RootContentBehavior is only in the deprecated profile.
    private fun document(mode: Int): CoreDocument =
        buildDocument(RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_DEPRECATED) {
            writer.setRootContentBehavior(
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                mode,
            )
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
        }

    private fun play(document: CoreDocument): Bitmap {
        rule.setContent {
            Box(modifier = Modifier.size(200.dp, 100.dp)) { RcPlayer(document = document) }
        }
        return rule.onRoot().captureToImage().asAndroidBitmap()
    }
}
