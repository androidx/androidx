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

package androidx.graphics.lowlatency

import android.hardware.HardwareBuffer
import android.os.Build
import androidx.graphics.opengl.egl.EglManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@SmallTest
internal class RenderBufferPoolTest {

    @Test
    fun testHardwareBufferMatchesConfig() {
        withEglSpec { eglSpec ->
            val width = 2
            val height = 3
            val format = HardwareBuffer.RGB_565
            val usage = HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            val pool = createPool(
                width,
                height,
                format,
                usage
            )
            try {
                val buffer = pool.obtain(eglSpec).hardwareBuffer
                assertEquals(width, buffer.width)
                assertEquals(height, buffer.height)
                assertEquals(format, buffer.format)
                assertEquals(usage, buffer.usage)
            } finally {
                pool.close()
            }
        }
    }

    @Test
    fun testCloseReleasesRenderBuffer() {
        withEglSpec { egl ->
            val pool = createPool()
            val renderBuffer = pool.obtain(egl)
            pool.release(renderBuffer)
            pool.close()
            assertTrue(renderBuffer.isClosed)
        }
    }

    @Test
    fun testAllocationAtMaxPoolSizeBlocks() {
        withEglSpec { egl ->
            val poolSize = 2
            val latch = CountDownLatch(1)
            thread {
                val pool = createPool(maxPoolSize = poolSize)
                // Attempting to allocate 1 additional buffer than
                // maximum specified pool size should block
                repeat(poolSize + 1) {
                    pool.obtain(egl)
                }
                latch.countDown()
            }
            assertFalse(latch.await(3, TimeUnit.SECONDS))
        }
    }

    @Test
    fun testReleaseAtMaxPoolSizeUnblocks() {
        withEglSpec { egl ->
            val poolSize = 2
            val latch = CountDownLatch(1)
            val pool = createPool(maxPoolSize = poolSize)
            val b1 = pool.obtain(egl)
            pool.obtain(egl)
            var b3: RenderBuffer? = null
            thread {
                b3 = pool.obtain(egl)
                latch.countDown()
            }
            pool.release(b1)
            assertTrue(latch.await(3, TimeUnit.SECONDS))
            assertTrue(b1 === b3)
        }
    }

    fun createPool(
        width: Int = 2,
        height: Int = 3,
        format: Int = HardwareBuffer.RGB_565,
        usage: Long = HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        maxPoolSize: Int = 2
    ): RenderBufferPool =
        RenderBufferPool(
            width,
            height,
            format,
            usage,
            maxPoolSize
        )

    private fun withEglSpec(
        block: (egl: EGLSpec) -> Unit = {}
    ) {
        with(EglManager()) {
            initialize()
            if (supportsNativeAndroidFence()) {
                block(eglSpec)
            }
            release()
        }
    }
}