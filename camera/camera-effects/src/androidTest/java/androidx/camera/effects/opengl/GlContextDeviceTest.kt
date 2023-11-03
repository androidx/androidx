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

package androidx.camera.effects.opengl

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [GlContext].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class GlContextDeviceTest {

    companion object {
        private const val TIMESTAMP_NS = 0L
    }

    private val glContext = GlContext()

    private lateinit var surface: Surface
    private lateinit var surfaceTexture: SurfaceTexture

    @Before
    fun setUp() {
        surfaceTexture = SurfaceTexture(0)
        surface = Surface(surfaceTexture)
        glContext.init()
    }

    @After
    fun tearDown() {
        glContext.release()
        surfaceTexture.release()
        surface.release()
    }

    @Test(expected = IllegalStateException::class)
    fun drawUnregisteredSurface_throwsException() {
        glContext.drawAndSwap(surface, TIMESTAMP_NS)
    }

    @Test(expected = IllegalStateException::class)
    fun unregisterSurfaceAndDraw_throwsException() {
        glContext.registerSurface(surface)
        glContext.unregisterSurface(surface)
        glContext.drawAndSwap(surface, TIMESTAMP_NS)
    }

    @Test
    fun drawRegisteredSurface_noException() {
        glContext.registerSurface(surface)
        glContext.drawAndSwap(surface, TIMESTAMP_NS)
    }

    @Test
    fun registerSurfaceWithoutDrawingOrReleasing_noException() {
        glContext.registerSurface(surface)
    }
}
