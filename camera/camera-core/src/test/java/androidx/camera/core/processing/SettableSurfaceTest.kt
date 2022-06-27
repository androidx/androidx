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

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.SurfaceOutput
import androidx.camera.testing.fakes.FakeCamera
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SettableSurface].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SettableSurfaceTest {
    private lateinit var fakeDeferrableSurface: DeferrableSurface
    private lateinit var fakeSettableSurface: SettableSurface

    @Before
    fun setUp() {
        fakeDeferrableSurface = createFakeDeferrableSurface()
        fakeSettableSurface = createFakeSettableSurface()
    }

    @After
    fun tearDown() {
        fakeDeferrableSurface.close()
        fakeSettableSurface.close()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setSource_surfaceIsPropagated() {
        // Act.
        fakeSettableSurface.setSource(fakeDeferrableSurface)
        // Assert.
        Truth.assertThat(fakeSettableSurface.surface.get())
            .isEqualTo(fakeDeferrableSurface.surface.get())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun createSurfaceRequestAsSource_surfaceIsPropagated() {
        // Act.
        val surfaceRequest = fakeSettableSurface.createSurfaceRequestAsSource(
            FakeCamera(), false
        )
        surfaceRequest.provideSurface(
            fakeDeferrableSurface.surface.get(),
            CameraXExecutors.directExecutor()
        ) { fakeDeferrableSurface.close() }
        // Assert.
        Truth.assertThat(fakeSettableSurface.surface.get())
            .isEqualTo(fakeDeferrableSurface.surface.get())
    }

    @Test(expected = IllegalStateException::class)
    fun setSourceTwice_throwsException() {
        fakeSettableSurface.setSource(fakeDeferrableSurface)
        fakeSettableSurface.setSource(fakeDeferrableSurface)
    }

    @Test(expected = IllegalStateException::class)
    fun setSourceThenCreateSurfaceRequest_throwsException() {
        fakeSettableSurface.setSource(fakeDeferrableSurface)
        fakeSettableSurface.createSurfaceRequestAsSource(FakeCamera(), false)
    }

    private fun createFakeSettableSurface(): SettableSurface {
        return SettableSurface(
            SurfaceOutput.PREVIEW, Size(640, 480), ImageFormat.PRIVATE,
            Matrix(), true, Rect(), 0, false
        )
    }

    private fun createFakeDeferrableSurface(): DeferrableSurface {
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val deferrableSurface = object : DeferrableSurface() {
            override fun provideSurface(): ListenableFuture<Surface> {
                return Futures.immediateFuture(surface)
            }
        }
        deferrableSurface.terminationFuture.addListener({
            surface.release()
            surfaceTexture.release()
        }, CameraXExecutors.directExecutor())
        return deferrableSurface
    }
}