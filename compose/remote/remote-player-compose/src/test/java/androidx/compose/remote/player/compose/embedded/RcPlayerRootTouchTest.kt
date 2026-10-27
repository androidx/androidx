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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that the embedded player only publishes the root touch position when something in the
 * document reads it, so pressing and dragging over a document without touch handling writes no
 * player state. Root touch expressions and layout documents reading the touch position are covered
 * by `RcPlayerInteractivityTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerRootTouchTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun documentWithoutTouch_pressAndDrag_appliesNoSnapshotChanges() {
        rule.setRemoteContent { RemoteText("Static".rs) }
        rule.waitForIdle()

        val applies = countSnapshotApplies {
            rule.onRoot().performTouchInput {
                down(center)
                moveBy(Offset(2f, 0f))
                moveBy(Offset(2f, 0f))
                moveBy(Offset(2f, 0f))
                up()
            }
        }

        assertThat(applies).isEqualTo(0)
    }

    @Test
    fun drawReadingTouchPosition_followsPointer() {
        // No touch expression: only the draw reads the touch position.
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            drawRect(0f, 0f, RemoteContext.FLOAT_TOUCH_POS_X, 10f)
        }
        setPlayerContent(document)
        assertThat(redWidth()).isEqualTo(0)

        rule.onRoot().performTouchInput { down(Offset(60f, 50f)) }
        rule.waitForIdle()
        assertThat(redWidth()).isIn(59..61)

        rule.onRoot().performTouchInput { moveTo(Offset(30f, 50f)) }
        rule.waitForIdle()
        assertThat(redWidth()).isIn(29..31)
        rule.onRoot().performTouchInput { up() }
    }

    private fun setPlayerContent(document: CoreDocument) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
            }
        }
        rule.waitForIdle()
    }

    /** The width of the red bar drawn along the top row of the player. */
    private fun redWidth(): Int =
        rule.captureRootPixels(100, 1).count { pixel ->
            AndroidColor.red(pixel) > 200 &&
                AndroidColor.green(pixel) < 60 &&
                AndroidColor.blue(pixel) < 60
        }

    /** Counts the non-empty snapshot applies caused by [block] and the frames it leads to. */
    private fun countSnapshotApplies(block: () -> Unit): Int {
        var applies = 0
        val handle = Snapshot.registerApplyObserver { changed, _ ->
            if (changed.isNotEmpty()) applies++
        }
        try {
            block()
            rule.runOnIdle { Snapshot.sendApplyNotifications() }
            rule.waitForIdle()
        } finally {
            handle.dispose()
        }
        return applies
    }
}
