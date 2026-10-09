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
import android.util.Log
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.serialize.MapSerializer
import androidx.compose.remote.player.compose.embedded.doc.getOperationsList
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperationPolicy
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperations
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** Tests for [UnknownOperations], the policy for paint ops the embedded player cannot execute. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerUnknownOperationsTest {

    // Sets UnknownOperationPolicy.Fail for the duration of each test.
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun failPolicyThrowsForUnknownPaintOperation() {
        // The rule's Fail policy is what makes every embedded test reject unknown paint ops.
        assertThat(UnknownOperations.policy).isEqualTo(UnknownOperationPolicy.Fail)

        val error =
            assertThrows(IllegalStateException::class.java) {
                UnknownOperations.onUnknown(UnknownPaintOperation())
            }

        assertThat(error).hasMessageThat().contains(UnknownPaintOperation::class.java.name)
    }

    @Test
    fun logPolicyWarnsAndKeepsDrawing() {
        UnknownOperations.policy = UnknownOperationPolicy.Log
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
        }
        document.getOperationsList().add(UnknownPaintOperation())

        val bitmap = rule.renderDocument(document)

        assertRed(bitmap, 50, 50)
        assertThat(ShadowLog.getLogsForTag("RcPlayer").map { it.type to it.msg })
            .contains(Log.WARN to "Embedded RcPlayer cannot execute UnknownPaintOperation")
    }

    @Test
    fun dataAndSetupOperationsAreNotUnknown() {
        // Runs under the Fail policy: none of these may reach UnknownOperations.
        val bitmap = rule.renderDocument {
            writer.addSound(byteArrayOf(1, 2, 3, 4))
            writer.addText("text")
            writer.addFloatArray(floatArrayOf(1f, 2f))
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
        }

        assertRed(bitmap, 50, 50)
    }

    /** A paint op the embedded dispatcher has no branch for. */
    private class UnknownPaintOperation : PaintOperation() {
        override fun paint(context: PaintContext) {}

        override fun write(buffer: WireBuffer) {}

        override fun deepToString(indent: String): String = "UnknownPaintOperation"

        override fun serialize(serializer: MapSerializer) {}
    }
}
