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

import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.ui.geometry.Size
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CanvasOperationBufferTest {

    @Test(timeout = 5000)
    fun testPropagateSpanWithDeepDAG() {
        val v = MutableRemoteFloat(1f)
        var x: RemoteFloat = v
        for (i in 0 until 30) { // 2^30 is 1B, should timeout if complexity is bad.
            x = x + x
        }

        val buffer = CanvasOperationBuffer()
        val op1 = buffer.recordRenderingOp {}
        val op2 = buffer.recordRenderingOp {}
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
}
