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
package androidx.camera.effects.playservices.lowlight

import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class InputSurfaceProviderTest {

    private val renderers = mutableListOf<FakeGlRenderer>()
    private var failGlInit = false
    private val renderPipeline =
        LowLightBoostRenderPipeline(
            glHandler = Handler(getMainLooper()),
            rendererFactory = {
                FakeGlRenderer(renderers.size + 1, failInit = failGlInit).also {
                    renderers.add(it)
                }
            },
            onInputFrameAvailable = { _, _ -> },
            onOutputFrameAvailable = { _, _ -> },
        )
    private val provider = InputSurfaceProvider(renderPipeline, CameraXExecutors.directExecutor())
    private val surfaceRequests = mutableListOf<SurfaceRequest>()
    private var completedRequests = 0

    @After
    fun tearDown() {
        renderPipeline.releaseBridges()
        provider.releaseDetachedResources()
        surfaceRequests.forEach { it.deferrableSurface.close() }
    }

    @Test
    fun eachRequest_getsNewSurface() {
        // Act.
        provideSurface()
        val firstSurface = renderPipeline.inputBridge.inputSurface
        provideSurface()

        // Assert.
        assertThat(renderPipeline.inputBridge.inputSurface).isNotSameInstanceAs(firstSurface)
    }

    @Test
    fun replacedSurface_isDrainedUntilItsRequestCompletes() {
        // Arrange.
        provideSurface()
        val oldRenderer = renderers.last()
        val oldSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)

        // Act.
        provideSurface()
        val isOldRendererReleasedBeforeCompletion = oldRenderer.isReleased
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Assert: the old resources are kept until the old request completes.
        assertThat(isOldRendererReleasedBeforeCompletion).isFalse()
        assertThat(oldRenderer.isReleased).isTrue()
        assertThat(oldSurface.isValid).isFalse()
        assertThat(completedRequests).isEqualTo(1)
    }

    @Test
    fun replacedSurfaceOfCompletedRequest_isReleasedRightAway() {
        // Arrange.
        provideSurface()
        val oldSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        val isValidAfterCompletion = oldSurface.isValid

        // Act.
        provideSurface()

        // Assert: the current Surface is kept after its request completes, until it is replaced.
        assertThat(isValidAfterCompletion).isTrue()
        assertThat(oldSurface.isValid).isFalse()
    }

    @Test
    fun newResolution_releasesOldSurfaceOnlyAfterOldRequestCompletes() {
        // Arrange.
        provideSurface()
        val oldSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)

        // Act: a request for another resolution replaces the input resources.
        provideSurface(resolution = Size(1280, 720))
        val isOldValidBeforeCompletion = oldSurface.isValid
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(isOldValidBeforeCompletion).isTrue()
        assertThat(oldSurface.isValid).isFalse()
    }

    @Test
    fun newRequestCompletesBeforeOldOne_releasesOnlyOldSurface() {
        // Arrange.
        provideSurface()
        val oldSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)
        provideSurface()
        val currentSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)

        // Act: the new request completes first, then the old one.
        surfaceRequests[1].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        val isOldValidAfterNewCompletes = oldSurface.isValid
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Assert: the current Surface is kept until it is replaced or released.
        assertThat(isOldValidAfterNewCompletes).isTrue()
        assertThat(oldSurface.isValid).isFalse()
        assertThat(currentSurface.isValid).isTrue()
        assertThat(renderPipeline.inputBridge.inputSurface).isSameInstanceAs(currentSurface)
    }

    @Test
    fun openGlInitFailure_providesNoSurface() {
        // Arrange.
        failGlInit = true

        // Act.
        val provided = provideSurface()

        // Assert.
        assertThat(provided).isFalse()
        assertThat(renderPipeline.inputBridge.inputSurface).isNull()
    }

    @Test
    fun releaseDetachedResources_releasesSurfacesOfPendingRequests() {
        // Arrange: the old Surface is kept for a request that hasn't completed.
        provideSurface()
        val oldSurface = requireNotNull(renderPipeline.inputBridge.inputSurface)
        provideSurface()

        // Act.
        provider.releaseDetachedResources()

        // Assert.
        assertThat(oldSurface.isValid).isFalse()
    }

    private fun provideSurface(resolution: Size = Size(1920, 1080)): Boolean {
        val surfaceRequest =
            SurfaceRequest(
                resolution,
                FakeCamera("0"),
                DynamicRange.SDR,
                SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
            ) {}
        surfaceRequests.add(surfaceRequest)
        val provided =
            provider.provideNewInputSurface(surfaceRequest, resolution, activeCamSurface = null) {
                completedRequests++
            }
        shadowOf(getMainLooper()).idle()
        return provided
    }
}
