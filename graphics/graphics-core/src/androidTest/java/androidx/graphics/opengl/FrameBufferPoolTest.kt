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

package androidx.graphics.opengl

import android.hardware.HardwareBuffer
import android.os.Build
import androidx.graphics.opengl.FrameBufferPool.Companion.findEntryWith
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@SmallTest
internal class FrameBufferPoolTest {

    @Test
    fun testHardwareBufferMatchesConfig() {
        withEGLSpec { eglSpec ->
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
    fun testCloseReleasesFrameBuffer() {
        withEGLSpec { egl ->
            val pool = createPool()
            val frameBuffer = pool.obtain(egl)
            pool.release(frameBuffer, SyncStrategy.ALWAYS.createSyncFence(egl))
            pool.close()
            assertTrue(frameBuffer.isClosed)
        }
    }

    @Test
    fun testReleaseAlreadyReleasedFrameBuffer() {
        withEGLSpec { egl ->
            val pool = createPool()
            val fb1 = pool.obtain(egl)
            val fb2 = pool.obtain(egl)
            pool.release(fb1)
            pool.release(fb1) // This should be ignored as it is already released
            pool.close()
            pool.release(fb2)
        }
    }

    @Test
    fun testReleaseAlreadyClosedFrameBuffer() {
        withEGLSpec { egl ->
            val pool = createPool()
            val fb = pool.obtain(egl)
            assertEquals(1, pool.allocationCount)
            pool.release(fb)
            pool.close()
            pool.release(fb)
            // releasing already closed buffer should reduce the allocation count
            assertEquals(0, pool.allocationCount)
        }
    }

    @Test
    fun testAllocationAtMaxPoolSizeBlocks() {
        withEGLSpec { egl ->
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
        withEGLSpec { egl ->
            val poolSize = 2
            val latch = CountDownLatch(1)
            val pool = createPool(maxPoolSize = poolSize)
            val b1 = pool.obtain(egl)
            pool.obtain(egl)
            var b3: FrameBuffer? = null
            thread {
                b3 = pool.obtain(egl)
                latch.countDown()
            }
            pool.release(b1, SyncStrategy.ALWAYS.createSyncFence(egl))
            assertTrue(latch.await(3, TimeUnit.SECONDS))
            assertTrue(b1 === b3)
        }
    }

    @Test
    fun testReleaseCloseBufferAtMaxPoolSizeUnblocks() {
        withEGLSpec { egl ->
            val poolSize = 2
            val latch = CountDownLatch(1)
            val pool = createPool(maxPoolSize = poolSize)
            val b1 = pool.obtain(egl)
            pool.obtain(egl)
            var b3: FrameBuffer? = null
            thread {
                b3 = pool.obtain(egl)
                latch.countDown()
            }
            b1.close()
            pool.release(b1, SyncStrategy.ALWAYS.createSyncFence(egl))
            assertTrue(latch.await(3, TimeUnit.SECONDS))
            // Because b1 was closed before releasing, the pool should allocate a new
            // buffer for b3 instead of reusing b1
            assertTrue(b1 !== b3)
        }
    }

    @Test
    fun testBufferReleasedToDifferentFrameBufferPoolThrows() {
        withEGLSpec { egl ->
            val pool1 = createPool()
            val buffer = pool1.obtain(egl)
            val pool2 = createPool()
            try {
                // Attempting to throw
                pool2.release(buffer)
                fail("Releasing a buffer not originally owned by the same FrameBufferPool")
            } catch (exception: IllegalArgumentException) {
                // NO-OP expected to throw
            }
        }
    }

    @Test
    fun testFindQueueEntryWithCondition() {
        data class Entry(val value: Int?, var available: Boolean = true)
        val list = ArrayList<Entry>().apply {
            add(Entry(5))
            add(Entry(4))
            add(Entry(2))
            add(Entry(3))
            add(Entry(null))
            add(Entry(1))
        }
        val primary: (Entry) -> Boolean = { entry -> entry.available }
        val secondary: (Entry) -> Boolean = { entry ->
            // Return the first null or odd entry
            (entry.value == null || entry.value % 2 == 1)
        }

        assertEquals(Entry(5, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(Entry(3, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(Entry(null, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(Entry(1, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(Entry(4, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        // Verify that we return the first entry that satisfies the primary condition while there
        // are no entries that satisfy both.
        // This should return the first even number we find that is available (4) in this case
        list[1].available = true
        assertEquals(Entry(4, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(Entry(2, false),
            list.findEntryWith(primary, secondary)!!.apply { available = false })

        assertEquals(null, list.findEntryWith(primary, secondary))
    }

    private fun createPool(
        width: Int = 2,
        height: Int = 3,
        format: Int = HardwareBuffer.RGB_565,
        usage: Long = HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        maxPoolSize: Int = 2
    ): FrameBufferPool =
        FrameBufferPool(
            width,
            height,
            format,
            usage,
            maxPoolSize
        )

    private fun withEGLSpec(
        block: (egl: EGLSpec) -> Unit = {}
    ) {
        with(EGLManager()) {
            try {
                initialize()
                createContext(loadConfig(EGLConfigAttributes.RGBA_8888)!!)
                if (supportsNativeAndroidFence()) {
                    block(eglSpec)
                }
            } finally {
                release()
            }
        }
    }
}
