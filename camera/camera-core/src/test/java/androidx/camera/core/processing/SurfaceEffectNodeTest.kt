/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing

import android.os.Build
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SurfaceEffectNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceEffectNodeTest {

    @Test
    fun releaseNode_effectIsReleased() {
        // Arrange: set up releasable effect and the wrapping node.
        var isReleased = false
        val releasableEffect = object :
            SurfaceEffectInternal {
            override fun onInputSurface(request: SurfaceRequest) {}

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {}

            override fun getExecutor(): Executor {
                return directExecutor()
            }

            override fun release() {
                isReleased = true
            }
        }
        val node = SurfaceEffectNode(releasableEffect, directExecutor())

        // Act: release the node.
        node.release()

        // Assert: effect is released too.
        assertThat(isReleased).isTrue()
    }
}