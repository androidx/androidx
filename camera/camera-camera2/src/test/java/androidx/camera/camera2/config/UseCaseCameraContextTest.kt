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

package androidx.camera.camera2.config

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.adapter.CameraSessionLifecycleAdapter
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.impl.CameraCallbackMap
import androidx.camera.camera2.impl.CameraGraphConfigProvider
import androidx.camera.camera2.impl.ComboRequestListener
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraGraph
import androidx.camera.camera2.testing.FakeSurface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class UseCaseCameraContextTest {
    private val cameraStateAdapter = CameraStateAdapter(CameraSessionLifecycleAdapter())

    @Test
    fun closeGraph_clearsSurfaceToStreamMap() {
        val surface = FakeSurface()
        val context = createContext(surface)
        assertThat(context.surfaceToStreamMap).containsExactly(surface, StreamId(0))

        context.closeGraph()
        surface.close()

        assertThat(context.surfaceToStreamMap).isEmpty()
        assertThat(context.getStreamIdsFromSurfaces(listOf(surface))).isEmpty()
    }

    @Test
    fun closeGraph_releasesConfigUseCasesAndSurfaces_whenGraphInitialized() = runTest {
        val (context, surfaceRef, useCaseRef) =
            createProductionContextWithWeakRefs(testScope = this, initializeGraphAndMap = true)
        val retainedGraph: CameraGraph = context.graph

        context.closeGraph()
        GarbageCollectionUtil.runFinalization()

        assertThat(context.surfaceToStreamMap).isEmpty()
        assertThat(retainedGraph).isNotNull()
        assertThat(surfaceRef.get()).isNull()
        assertThat(useCaseRef.get()).isNull()
    }

    @Test
    fun closeGraph_releasesConfigUseCasesAndSurfaces_whenGraphNotInitialized() = runTest {
        val (context, surfaceRef, useCaseRef) =
            createProductionContextWithWeakRefs(testScope = this, initializeGraphAndMap = false)

        context.closeGraph()
        GarbageCollectionUtil.runFinalization()

        assertThat(context.surfaceToStreamMap).isEmpty()
        assertThat(surfaceRef.get()).isNull()
        assertThat(useCaseRef.get()).isNull()
    }

    private fun createProductionContextWithWeakRefs(
        testScope: TestScope,
        initializeGraphAndMap: Boolean,
    ): Triple<UseCaseCameraContext, WeakReference<DeferrableSurface>, WeakReference<FakeUseCase>> {
        val cameraMetadata = FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate)
        val cameraQuirks =
            CameraQuirks(
                cameraMetadata,
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    cameraMetadata,
                ),
            )
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(cameraMetadata.camera),
                cameraQuirks = cameraQuirks,
                zslControl = ZslControlNoOpImpl(),
                templateParamsOverride = TemplateParamsQuirkOverride(cameraQuirks.quirks),
                cameraMetadata = cameraMetadata,
            )
        val surface = FakeSurface()
        val useCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .apply {
                            setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                            addSurface(surface)
                        }
                        .build()
                )
            }
        val config =
            UseCaseCameraConfig.create(
                sessionConfigAdapter = SessionConfigAdapter(listOf(useCase)),
                cameraGraphConfigProvider = configProvider,
                cameraGraphFactory = { graphConfig ->
                    CameraGraphSimulator.create(
                        testScope = testScope,
                        testContext = ApplicationProvider.getApplicationContext(),
                        cameraMetadata = cameraMetadata,
                        graphConfig = graphConfig,
                    )
                },
                cameraStateAdapter = cameraStateAdapter,
                sessionProcessor = null,
                extensionMode = null,
            )
        val context = config.provideUseCaseCameraContext(cameraStateAdapter)
        if (initializeGraphAndMap) {
            assertThat(context.surfaceToStreamMap).containsKey(surface)
        }
        // Close the test surface before releasing its strong reference so GC does not trigger
        // DeferrableSurface's unclosed-termination-future detector.
        surface.close()
        return Triple(context, WeakReference(surface), WeakReference(useCase))
    }

    private fun createContext(surface: FakeSurface): UseCaseCameraContext {
        val fakeCameraGraph = FakeCameraGraph()
        return UseCaseCameraContext(
            cameraGraphProvider = { fakeCameraGraph },
            cameraStateAdapter = cameraStateAdapter,
            graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
            streamConfigMapProvider = { emptyMap() },
            defaultSurfaceToStreamMap = mapOf(surface to StreamId(0)),
        )
    }
}
