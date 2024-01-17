/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [DefaultSurfaceProcessor].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DefaultSurfaceProcessorTest {

    @Test
    fun setFactorySupplier_factoryProvidesInstance() {
        // Arrange: create a no-op processor and set it on the factory.
        val noOpProcessor = object : SurfaceProcessorInternal {
            override fun onInputSurface(request: SurfaceRequest) {
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
            }

            override fun release() {
            }
        }
        DefaultSurfaceProcessor.Factory.setSupplier { noOpProcessor }
        // Assert: new instance returns the no-op processor.
        assertThat(DefaultSurfaceProcessor.Factory.newInstance(DynamicRange.SDR))
            .isSameInstanceAs(noOpProcessor)
    }
}
