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
import androidx.compose.remote.creation.compose.state.cacheKey
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
        val childSpan = buffer.insertPoint.createChildSpan()
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
                "Span(depth=0, ops=[SaveRestore(children=[Transform(Translate), DrawConditionally(true)])], child=Span(depth=1, ops=[Draw]))"
            )

        canvas.flush()
        assertThat(canvas.buffer.toString()).isEqualTo("CanvasOperationBuffer(empty)")
    }

    @Test
    fun testSaveGetRootSaveNode() {
        val root = CanvasOp.SaveRestore(parent = null)
        val child1 = CanvasOp.SaveRestore(parent = root)
        val child2 = CanvasOp.SaveRestore(parent = child1)

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
        val saveRestore = CanvasOp.SaveRestore()

        assertThat(draw.toString()).isEqualTo("Draw")
        assertThat(clip.toString()).isEqualTo("Clip")
        assertThat(transformTranslate.toString()).isEqualTo("Transform(Translate)")
        assertThat(transformRotate.toString()).isEqualTo("Transform(Rotate)")
        assertThat(saveRestore.toString()).isEqualTo("SaveRestore(children=[])")
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

    /**
     * Tests that variable IDs generated for hoisted common sub-expressions are deterministic and
     * strictly preserve the order in which expressions were recorded.
     *
     * How this detects non-determinism: If ID generation depended on non-deterministic factors
     * (such as object hash codes, memory addresses, or collection bucket layouts), the relative ID
     * ordering between two independent expressions would be dictated by those factors rather than
     * recording order. By evaluating the buffer under two inverted recording orders:
     * 1. Recording (exp1, exp2) asserts id(exp1) < id(exp2)
     * 2. Recording (exp2, exp1) asserts id(exp2) < id(exp1) any ordering mechanism driven by key
     *    hashes or unordered collections would produce a fixed or non-deterministic order that
     *    fails one of the two assertions.
     */
    @Test
    fun testCommonSubExpressionEliminationDeterministicOrder() {
        val v1 = MutableRemoteFloat(1f)
        val v2 = MutableRemoteFloat(2f)
        val v3 = MutableRemoteFloat(3f)
        val v4 = MutableRemoteFloat(4f)

        val exp1 = v1 + v2
        val exp2 = v3 + v4

        // Case 1: Record exp1 before exp2 -> exp1 must receive a lower variable ID
        run {
            val buffer = CanvasOperationBuffer()
            val creationState =
                RemoteComposeCreationState(RcPlatformServices.None, Size(100f, 100f))

            val op1 = buffer.recordRenderingOp(CanvasOp.Draw {})
            buffer.addRoots(op1, exp1, exp2)
            val op2 = buffer.recordRenderingOp(CanvasOp.Draw {})
            buffer.addRoots(op2, exp1, exp2)

            buffer.flush(creationState)

            val id1 = creationState.remoteVariableToId.getOrDefault(exp1.cacheKey, -1)
            val id2 = creationState.remoteVariableToId.getOrDefault(exp2.cacheKey, -1)

            assertThat(id1).isNotEqualTo(-1)
            assertThat(id2).isNotEqualTo(-1)
            assertThat(id1).isLessThan(id2)
        }

        // Case 2: Record exp2 before exp1 -> exp2 must receive a lower variable ID
        run {
            val buffer = CanvasOperationBuffer()
            val creationState =
                RemoteComposeCreationState(RcPlatformServices.None, Size(100f, 100f))

            val op1 = buffer.recordRenderingOp(CanvasOp.Draw {})
            buffer.addRoots(op1, exp2, exp1)
            val op2 = buffer.recordRenderingOp(CanvasOp.Draw {})
            buffer.addRoots(op2, exp2, exp1)

            buffer.flush(creationState)

            val id1 = creationState.remoteVariableToId.getOrDefault(exp1.cacheKey, -1)
            val id2 = creationState.remoteVariableToId.getOrDefault(exp2.cacheKey, -1)

            assertThat(id1).isNotEqualTo(-1)
            assertThat(id2).isNotEqualTo(-1)
            assertThat(id2).isLessThan(id1)
        }
    }
}
