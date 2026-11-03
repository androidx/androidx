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

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.util.Size
import android.view.Surface
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LowLightBoostRenderPipelineTest {

    private val handler = Handler(Looper.getMainLooper())
    private val renderers = mutableListOf<FakeGlRenderer>()
    private val surfaceTextures = mutableListOf<SurfaceTexture>()
    private val surfaces = mutableListOf<Surface>()
    private val bridgeTextures = mutableListOf<SurfaceTexture>()
    private var bridgeFrameCount = 0

    private val pipeline =
        LowLightBoostRenderPipeline(
            glHandler = handler,
            rendererFactory = { FakeGlRenderer(renderers.size + 1).also { renderers.add(it) } },
            onInputFrameAvailable = { _, _ -> },
            onOutputFrameAvailable = { _, _ -> },
        )

    @After
    fun tearDown() {
        pipeline.releaseBridges()
        surfaces.forEach { it.release() }
        surfaceTextures.forEach { it.release() }
    }

    @Test
    fun bridge_reusesResourcesForSameResolution() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val surface = bridge.inputSurface

        // Act.
        val recreated = bridge.ensureInitialized(Size(1920, 1080))

        // Assert.
        assertThat(recreated).isFalse()
        assertThat(bridge.inputSurface).isSameInstanceAs(surface)
        assertThat(renderers).hasSize(1)
        bridge.release()
    }

    @Test
    fun bridge_recreatesResourcesWhenResolutionChanges() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val output = createSurface()
        bridge.registerOutputSurface(output)

        // Act.
        val recreated = bridge.ensureInitialized(Size(1600, 1200))

        // Assert: the old renderer is released and the output must be registered again.
        assertThat(recreated).isTrue()
        assertThat(renderers[0].isReleased).isTrue()
        assertThat(bridge.glRenderer).isSameInstanceAs(renderers[1])
        assertThat(bridge.isOutputRegistered(output)).isFalse()
        bridge.release()
        assertThat(renderers[1].isReleased).isTrue()
        assertThat(bridge.isValid()).isFalse()
    }

    @Test
    fun bridge_registersOutputOnce() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val output = createSurface()

        // Act.
        bridge.registerOutputSurface(output)
        bridge.registerOutputSurface(output)
        bridge.unregisterOutputSurface(output)

        // Assert.
        assertThat(renderers[0].registerCallCount).isEqualTo(1)
        assertThat(bridge.isOutputRegistered(output)).isFalse()
        assertThat(renderers[0].registeredSurfaces).isEmpty()
        bridge.release()
    }

    @Test
    fun bridge_recreation_canKeepDrainingOldSurface() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val oldSurface = requireNotNull(bridge.inputSurface)
        val oldTexture = bridgeTextures.last()
        val output = createSurface()
        bridge.registerOutputSurface(output)
        var detached: OpenGlBridge.DetachedResources? = null

        // Act: keep the old surface, e.g. while the camera may still produce frames into it.
        bridge.ensureInitialized(
            Size(1600, 1200),
            onResourcesDetached = {
                detached = it
                it.drainUntilReleased()
            },
        )
        deliverFrame(oldTexture)

        // Assert: the old renderer keeps draining frames without outputs, and doesn't deliver
        // them to the bridge, until the old resources are released.
        val oldResources = requireNotNull(detached)
        assertThat(renderers[0].isReleased).isFalse()
        assertThat(renderers[0].registeredSurfaces).isEmpty()
        assertThat(oldResources.drainedFrameCount).isEqualTo(1)
        assertThat(bridgeFrameCount).isEqualTo(0)
        assertThat(oldSurface.isValid).isTrue()
        assertThat(oldResources.surface).isSameInstanceAs(oldSurface)
        assertThat(bridge.inputSurface).isNotSameInstanceAs(oldSurface)
        oldResources.release()
        deliverFrame(oldTexture)
        assertThat(renderers[0].isReleased).isTrue()
        assertThat(oldResources.drainedFrameCount).isEqualTo(1)
        assertThat(oldSurface.isValid).isFalse()
        assertThat(oldResources.surface).isNull()
        bridge.release()
    }

    @Test
    fun bridge_framesQueuedBeforeDrainStarts_areConsumedAndDrainingContinues() {
        // Arrange: frames of the old texture are queued but not handled yet.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val oldTexture = bridgeTextures.last()
        repeat(3) { postFrame(oldTexture) }
        var detached: OpenGlBridge.DetachedResources? = null
        bridge.ensureInitialized(
            Size(1600, 1200),
            onResourcesDetached = {
                detached = it
                it.drainUntilReleased()
            },
        )
        val oldResources = requireNotNull(detached)

        // Act: the queued frames are handled, then another frame arrives.
        shadowOf(Looper.getMainLooper()).idle()
        val countAfterQueuedFrames = oldResources.drainedFrameCount
        deliverFrame(oldTexture)

        // Assert: the queued frames are consumed by the draining resources, not delivered to the
        // bridge, and draining continues.
        assertThat(countAfterQueuedFrames).isEqualTo(3)
        assertThat(oldResources.drainedFrameCount).isEqualTo(4)
        assertThat(bridgeFrameCount).isEqualTo(0)
        oldResources.release()
        bridge.release()
    }

    @Test
    fun bridge_frameArrivingBetweenDetachAndDrain_isConsumed() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val oldTexture = bridgeTextures.last()
        val detached = bridge.detachResources()

        // Act: a frame arrives before draining starts, and is handled after it started.
        postFrame(oldTexture)
        detached.drainUntilReleased()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert.
        assertThat(detached.drainedFrameCount).isEqualTo(1)
        assertThat(bridgeFrameCount).isEqualTo(0)
        detached.release()
        bridge.release()
    }

    @Test
    fun bridge_framesOfDetachedTexture_areNotDelivered() {
        // Arrange.
        val bridge = createBridge()
        bridge.ensureInitialized(Size(1920, 1080))
        val oldTexture = bridgeTextures.last()
        var detached: OpenGlBridge.DetachedResources? = null
        bridge.ensureInitialized(Size(1600, 1200), onResourcesDetached = { detached = it })
        val newTexture = bridgeTextures.last()

        // Act.
        deliverFrame(oldTexture)
        val countAfterOldFrame = bridgeFrameCount
        deliverFrame(newTexture)

        // Assert: only frames of the current texture are delivered, and detached resources that
        // aren't draining don't consume frames.
        assertThat(countAfterOldFrame).isEqualTo(0)
        assertThat(bridgeFrameCount).isEqualTo(1)
        assertThat(requireNotNull(detached).drainedFrameCount).isEqualTo(0)
        requireNotNull(detached).release()
        bridge.release()
    }

    @Test
    fun ensureInputGlInitialized_withForceNewInput_replacesSurfaceAndRebindsCameraSurface() {
        // Arrange.
        val resolution = Size(1920, 1080)
        pipeline.ensureInputGlInitialized(resolution)
        val oldRenderer = inputRenderer()
        val oldSurface = requireNotNull(pipeline.inputBridge.inputSurface)
        val camSurface = createSurface()
        pipeline.bindSessionCameraSurfaceToInputBridge(camSurface)
        var detached: OpenGlBridge.DetachedResources? = null

        // Act: same resolution, so only forceNewInput recreates the resources.
        pipeline.ensureInputGlInitialized(
            resolution,
            activeCamSurface = camSurface,
            forceNewInput = true,
            onOldResourcesDetached = {
                detached = it
                it.drainUntilReleased()
            },
        )

        // Assert: a new surface is created, the old one is handed over, and the camera surface
        // is registered on the new renderer only.
        val newSurface = requireNotNull(pipeline.inputBridge.inputSurface)
        assertThat(newSurface).isNotSameInstanceAs(oldSurface)
        assertThat(oldRenderer.isReleased).isFalse()
        assertThat(oldRenderer.registeredSurfaces).isEmpty()
        assertThat(detached?.surface).isSameInstanceAs(oldSurface)
        assertThat(oldSurface.isValid).isTrue()
        assertThat(inputRenderer()).isNotSameInstanceAs(oldRenderer)
        assertThat(inputRenderer().registeredSurfaces).containsExactly(camSurface)
        assertThat(pipeline.isSessionCameraSurfaceBound(camSurface)).isTrue()
        requireNotNull(detached).release()
        assertThat(oldRenderer.isReleased).isTrue()
        assertThat(oldSurface.isValid).isFalse()
    }

    @Test
    fun bridgeInitFailure_leavesNoInputSurface() {
        // Arrange: a renderer that fails to initialize, e.g. when EGL is unavailable.
        val failingRenderer = FakeGlRenderer(failInit = true)
        val bridge =
            OpenGlBridge(
                name = "TestBridge",
                glHandler = handler,
                recreateOnResolutionChange = true,
                surfaceFactory = { Surface(it) },
                rendererFactory = { failingRenderer },
                onFrameAvailable = { _, _ -> },
            )

        // Act.
        val recreated = bridge.ensureInitialized(Size(1920, 1080))

        // Assert: no input surface is created, so it is never connected to the camera.
        assertThat(recreated).isFalse()
        assertThat(bridge.inputSurface).isNull()
        assertThat(bridge.isValid()).isFalse()
        assertThat(bridge.glRenderer).isNull()
        assertThat(failingRenderer.isReleased).isTrue()
    }

    @Test
    fun passthroughFrame_rendersThroughInputBridge() {
        // Arrange.
        val output = registerOutput()

        // Act.
        pipeline.renderPassthroughFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Assert.
        assertThat(inputRenderer().renderedSurfaces).containsExactly(output)
        assertThat(inputRenderer().registeredSurfaces).containsExactly(output)
        assertThat(outputRenderer().registeredSurfaces).isEmpty()
    }

    @Test
    fun boostedFrame_movesOutputToOutputBridge() {
        // Arrange.
        val output = registerOutput()
        pipeline.renderPassthroughFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Act.
        pipeline.renderBoostedFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Assert: the output is registered on exactly one renderer.
        assertThat(outputRenderer().renderedSurfaces).containsExactly(output)
        assertThat(outputRenderer().registeredSurfaces).containsExactly(output)
        assertThat(inputRenderer().registeredSurfaces).isEmpty()
    }

    @Test
    fun unregisterOutput_removesItFromBothBridges() {
        // Arrange.
        val surfaceOutput = FakeSurfaceOutput(createSurface())
        val output = registerOutput(surfaceOutput)
        pipeline.renderBoostedFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Act.
        val removed = pipeline.unregisterOutputSurface(surfaceOutput)

        // Assert.
        assertThat(removed).isSameInstanceAs(output)
        assertThat(pipeline.isOutputSurfacesEmpty()).isTrue()
        assertThat(outputRenderer().registeredSurfaces).isEmpty()
        assertThat(inputRenderer().registeredSurfaces).isEmpty()
    }

    @Test
    fun newOutput_replacesPreviousOutput() {
        // Arrange.
        val oldSurfaceOutput = FakeSurfaceOutput(createSurface())
        val oldOutput = registerOutput(oldSurfaceOutput)
        pipeline.renderPassthroughFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Act.
        val newOutput = registerOutput()
        pipeline.renderPassthroughFrameToOutputs(TIMESTAMP_NS, IDENTITY)

        // Assert: the old output is unregistered, and closing it later doesn't remove the new one.
        assertThat(inputRenderer().registeredSurfaces).containsExactly(newOutput)
        assertThat(pipeline.unregisterOutputSurface(oldSurfaceOutput)).isNull()
        assertThat(pipeline.isOutputSurfacesEmpty()).isFalse()
        assertThat(oldOutput).isNotSameInstanceAs(newOutput)
    }

    @Test
    fun nonPrivateOutput_isNotRegistered() {
        // Arrange.
        val surfaceOutput = FakeSurfaceOutput(createSurface(), format = ImageFormat.YUV_420_888)

        // Act & Assert.
        assertThat(pipeline.registerOutputSurface(surfaceOutput, createSurface())).isFalse()
        assertThat(pipeline.isOutputSurfacesEmpty()).isTrue()
    }

    @Test
    fun detachBoostedSessionResources_movesOutputToPassthroughBeforeDetaching() {
        // Arrange.
        val output = registerOutput()
        pipeline.renderBoostedFrameToOutputs(TIMESTAMP_NS, IDENTITY)
        val boostedRenderer = outputRenderer()
        val targetSurface = requireNotNull(pipeline.outputBridge.inputSurface)

        // Act.
        val detached = pipeline.detachBoostedSessionResources(fallbackCamSurface = null)

        // Assert: the output leaves the boosted renderer before the renderer is released.
        assertThat(boostedRenderer.registeredSurfaces).isEmpty()
        assertThat(boostedRenderer.isReleased).isFalse()
        assertThat(inputRenderer().registeredSurfaces).containsExactly(output)
        assertThat(targetSurface.isTerminated).isTrue()
        detached.release()
        assertThat(boostedRenderer.isReleased).isTrue()
    }

    @Test
    fun ensureBoostedGlInitialized_withForceNewIntermediate_replacesTargetSurface() {
        // Arrange.
        pipeline.ensureBoostedGlInitialized(Size(1600, 1200))
        val oldRenderer = outputRenderer()
        val oldTargetSurface = requireNotNull(pipeline.outputBridge.inputSurface)

        // Act.
        pipeline.ensureBoostedGlInitialized(Size(1600, 1200), forceNewIntermediate = true)

        // Assert.
        val newTargetSurface = requireNotNull(pipeline.outputBridge.inputSurface)
        assertThat(oldRenderer.isReleased).isTrue()
        assertThat(oldTargetSurface.isTerminated).isTrue()
        assertThat(newTargetSurface).isNotSameInstanceAs(oldTargetSurface)
        assertThat(newTargetSurface.isTerminated).isFalse()
        assertThat(newTargetSurface.isUnusedForSession).isTrue()
    }

    @Test
    fun ensureBoostedGlInitialized_returnsWhetherTargetSurfaceIsUsable() {
        // Arrange.
        val failingPipeline =
            LowLightBoostRenderPipeline(
                glHandler = handler,
                rendererFactory = { FakeGlRenderer(failInit = true) },
                onInputFrameAvailable = { _, _ -> },
                onOutputFrameAvailable = { _, _ -> },
            )

        // Act & Assert.
        assertThat(pipeline.ensureBoostedGlInitialized(Size(1920, 1080))).isTrue()
        assertThat(failingPipeline.ensureBoostedGlInitialized(Size(1920, 1080))).isFalse()
        assertThat(failingPipeline.outputBridge.inputSurface).isNull()
    }

    @Test
    fun sessionTargetSurface_canBeParceledOnlyOnce() {
        // Arrange.
        val targetSurface = SessionTargetSurface(createSurfaceTexture())
        val parcel = Parcel.obtain()
        try {
            // Act & Assert.
            targetSurface.writeToParcel(parcel, 0)
            assertThat(targetSurface.isUnusedForSession).isFalse()
            assertThrows(DeadObjectException::class.java) {
                targetSurface.writeToParcel(parcel, 0)
            }
        } finally {
            parcel.recycle()
            targetSurface.release()
        }
    }

    @Test
    fun terminatedSessionTargetSurface_cannotBeParceled() {
        // Arrange.
        val targetSurface = SessionTargetSurface(createSurfaceTexture())
        targetSurface.markTerminated()
        val parcel = Parcel.obtain()
        try {
            // Act & Assert.
            assertThrows(DeadObjectException::class.java) {
                targetSurface.writeToParcel(parcel, 0)
            }
        } finally {
            parcel.recycle()
            targetSurface.release()
        }
    }

    @Test
    fun releasedSessionTargetSurface_cannotBeParceled() {
        // Arrange: a released surface that isn't marked as terminated.
        val targetSurface = SessionTargetSurface(createSurfaceTexture())
        targetSurface.release()
        val parcel = Parcel.obtain()
        try {
            // Act & Assert.
            assertThat(targetSurface.isTerminated).isFalse()
            assertThrows(DeadObjectException::class.java) {
                targetSurface.writeToParcel(parcel, 0)
            }
        } finally {
            parcel.recycle()
        }
    }

    private fun createBridge(): OpenGlBridge<Surface> =
        OpenGlBridge(
            name = "TestBridge",
            glHandler = handler,
            recreateOnResolutionChange = true,
            surfaceFactory = { texture ->
                bridgeTextures.add(texture)
                Surface(texture)
            },
            rendererFactory = { FakeGlRenderer(renderers.size + 1).also { renderers.add(it) } },
            onFrameAvailable = { _, _ -> bridgeFrameCount++ },
        )

    /**
     * Queues a frame of [texture] like a producer does. In Robolectric's legacy graphics mode,
     * posting a canvas of a [Surface] reports a new frame of its [SurfaceTexture].
     */
    private fun postFrame(texture: SurfaceTexture) {
        val producer = Surface(texture)
        try {
            producer.unlockCanvasAndPost(producer.lockCanvas(null))
        } finally {
            producer.release()
        }
    }

    /** Queues a frame of [texture] and handles it. */
    private fun deliverFrame(texture: SurfaceTexture) {
        postFrame(texture)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Initializes both bridges and registers an output. */
    private fun registerOutput(
        surfaceOutput: FakeSurfaceOutput = FakeSurfaceOutput(createSurface())
    ): Surface {
        pipeline.ensureInputGlInitialized(Size(1920, 1080))
        pipeline.ensureBoostedGlInitialized(Size(1920, 1080))
        val surface = surfaceOutput.getSurface(Runnable::run) {}
        assertThat(pipeline.registerOutputSurface(surfaceOutput, surface)).isTrue()
        return surface
    }

    private fun inputRenderer(): FakeGlRenderer = pipeline.inputBridge.glRenderer as FakeGlRenderer

    private fun outputRenderer(): FakeGlRenderer =
        pipeline.outputBridge.glRenderer as FakeGlRenderer

    private fun createSurfaceTexture(): SurfaceTexture =
        SurfaceTexture(surfaceTextures.size + 100).also { surfaceTextures.add(it) }

    private fun createSurface(): Surface = Surface(createSurfaceTexture()).also { surfaces.add(it) }

    private companion object {
        private const val TIMESTAMP_NS = 1_000L
        private val IDENTITY = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    }
}
