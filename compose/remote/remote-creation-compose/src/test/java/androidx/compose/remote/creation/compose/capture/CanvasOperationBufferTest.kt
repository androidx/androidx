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

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CanvasOperationBufferTest {

    @Test(timeout = 5000)
    fun testPropagateSpanWithDeepDAG() {
        val v = MutableRemoteFloat(1f)
        var x: RemoteFloat = v
        for (i in 0 until 30) { // 2^30 is 1B, should timeout if complexity is bad.
            x = x + x
        }

        val buffer = CanvasOperationBuffer()
        val op1 = buffer.recordRenderingOp(CanvasOp.Draw {})
        val op2 = buffer.recordRenderingOp(CanvasOp.Draw {})
        buffer.addRoots(op1, x)
        buffer.addRoots(op2, x)

        val platform = RcPlatformServices.None
        val creationState = RemoteComposeCreationState(platform, Size(100f, 100f))

        try {
            buffer.flush(creationState)
        } catch (e: Exception) {
            // If it fails with some other error, we want to know, but we mainly care about
            // timeout/recursion
            throw e
        }
    }

    @Test
    fun testParentChildSpanRenderingDependencyOrderPreserved() {
        val buffer = CanvasOperationBuffer()
        val op1 = buffer.recordRenderingOp(CanvasOp.Draw {}) // Parent op 1

        // Simulate recordInChildSpan behavior
        val childSpan = buffer.createChildSpan()
        val prevInsertPoint = buffer.insertPoint
        val prevLastOp = buffer.lastRenderingOp
        buffer.insertPoint = childSpan
        val childOp = buffer.recordRenderingOp(CanvasOp.Draw {})
        buffer.insertPoint = prevInsertPoint
        buffer.lastRenderingOp = prevLastOp

        val op2 = buffer.recordRenderingOp(CanvasOp.Draw {}) // Parent op 2

        // Verify intra-span dependency edges are strictly preserved
        assertThat(childOp.deps).containsExactly(op1)
        assertThat(op2.deps).containsExactly(op1)

        val platform = RcPlatformServices.None
        val creationState = RemoteComposeCreationState(platform, Size(100f, 100f))
        buffer.flush(creationState)
    }

    @Test
    fun testConditionalSpanSaveScopeElisionPreservation() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Enable optimizations to test elisionPass behavior
        val canvas = RecordingCanvas(bitmap, enableOptimizations = true)
        val creationState = RemoteComposeCreationState(RcPlatformServices.None, Size(100f, 100f))
        canvas.setRemoteComposeCreationState(creationState)

        canvas.save()
        canvas.translate(50f, 50f)

        val condition = RemoteBoolean(true)
        canvas.drawConditionally(condition) { canvas.drawRect(0f, 0f, 10f, 10f, Paint()) }
        canvas.restore()

        // Manually invoke the optimizations (including elision pass) before flush clears the tree
        canvas.buffer.optimizeSpan(canvas.buffer.spanTreeRoot)
        val serializedTreeAfterOptimization = canvas.buffer.toString()
        // Verify enclosing Save/Translate is preserved across elision on the root span around
        // conditional block
        assertThat(serializedTreeAfterOptimization)
            .isEqualTo(
                "Span(depth=0, ops=[Save(children=[Transform(Translate), DrawConditionally(true)])], child=Span(depth=1, ops=[Draw]))"
            )

        canvas.flush()
        assertThat(canvas.buffer.toString()).isEqualTo("CanvasOperationBuffer(empty)")
    }

    @Test
    fun testSaveGetRootSaveNode() {
        val root = CanvasOp.Save(parent = null)
        val child1 = CanvasOp.Save(parent = root)
        val child2 = CanvasOp.Save(parent = child1)

        assertThat(root.getRootSaveNode()).isSameInstanceAs(root)
        assertThat(child1.getRootSaveNode()).isSameInstanceAs(root)
        assertThat(child2.getRootSaveNode()).isSameInstanceAs(root)
    }

    @Test
    fun testCanvasOpToStringOverrides() {
        val draw = CanvasOp.Draw {}
        val clip = CanvasOp.Clip {}
        val transformTranslate = CanvasOp.Transform(PendingOp.Translate(0f.rf, 0f.rf))
        val transformRotate = CanvasOp.Transform(PendingOp.Rotate(0f.rf, null, null))
        val save = CanvasOp.Save()

        assertThat(draw.toString()).isEqualTo("Draw")
        assertThat(clip.toString()).isEqualTo("Clip")
        assertThat(transformTranslate.toString()).isEqualTo("Transform(Translate)")
        assertThat(transformRotate.toString()).isEqualTo("Transform(Rotate)")
        assertThat(save.toString()).isEqualTo("Save(children=[])")
    }

    @Test
    fun testPendingOpSkewWithVariables() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RecordingCanvas(bitmap, enableOptimizations = true)
        val creationState = RemoteComposeCreationState(RcPlatformServices.None, Size(100f, 100f))
        canvas.setRemoteComposeCreationState(creationState)

        val sx = RemoteFloat(0.5f)
        val sy = RemoteFloat(0.2f)
        canvas.skew(sx, sy)
        canvas.drawRect(0f, 0f, 10f, 10f, Paint())

        // Verify buffer string before flush shows Skew transform
        assertThat(canvas.buffer.toString()).contains("Transform(Skew)")
    }
}
