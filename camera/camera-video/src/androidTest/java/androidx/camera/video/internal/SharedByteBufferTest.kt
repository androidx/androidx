/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal

import android.os.Build
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SharedByteBufferTest {

    @Test
    fun canRetrieveByteBuffer_fromOriginal() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }
            .use { sharedBuf -> assertThat(sharedBuf.get()).isEqualTo(buf) }
    }

    @Test
    fun canRetrieveByteBuffer_fromShared() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }
            .use { origBuf ->
                origBuf.share().use { sharedBuf -> assertThat(sharedBuf.get()).isEqualTo(buf) }
            }
    }

    @Test
    fun closeActionRuns_afterAllSharedBufsClosed() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        // Arrange
        val buf = ByteBuffer.allocate(0)
        var closeActionRan = false
        val origBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                closeActionRan = true
            }
        val sharedBufs = listOf(origBuf.share(), origBuf.share(), origBuf.share())

        // Act
        origBuf.close()
        val closeActionRanAfterOrigBufClosed = closeActionRan
        sharedBufs.forEach { it.close() }

        // Assert
        assertThat(closeActionRanAfterOrigBufClosed).isFalse()
        assertThat(closeActionRan).isTrue()
    }

    @Test
    fun closedSharedBuf_throwsOnGet() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        val sharedBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }

        sharedBuf.close()

        assertThrows<IllegalStateException> { sharedBuf.get() }
    }

    @Test
    fun closedSharedBuf_throwsOnShare() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        val sharedBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }

        sharedBuf.close()

        assertThrows<IllegalStateException> { sharedBuf.share() }
    }

    @Test
    fun canGetFromSharedBuf_afterOrigClosed() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        val origBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }

        origBuf.share().use { sharedBuf ->
            origBuf.close()
            assertThat(sharedBuf.get()).isEqualTo(buf)
        }
    }

    @Test
    fun closeAction_onlyRunsOnce_afterLastBufferClosed() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        var numFinalCloseInvocations = 0
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                numFinalCloseInvocations++
            }
            .use { sharedBuf -> repeat(10) { sharedBuf.share().close() } }

        assertThat(numFinalCloseInvocations).isEqualTo(1)
    }

    @Test
    fun closeAction_onlyRunsOnce_whenCloseCalledMultipleTimes() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(0)
        var numFinalCloseInvocations = 0
        val sharedBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                numFinalCloseInvocations++
            }

        repeat(10) {
            // Close same buffer. close() is idempotent.
            sharedBuf.close()
        }

        assertThat(numFinalCloseInvocations).isEqualTo(1)
    }

    @Test
    fun limitIsTransferred_toChildSharedInstances() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(16)
        var origLimit: Int
        var sharedInstanceLimit0: Int
        var sharedInstanceLimit1: Int
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }
            .use { origBuf ->
                origBuf.share().use { sharedBuf0 ->
                    sharedBuf0.get().limit(8)
                    sharedInstanceLimit0 = sharedBuf0.get().limit()

                    sharedBuf0.share().use { sharedBuf1 ->
                        sharedInstanceLimit1 = sharedBuf1.get().limit()
                    }
                }

                // Check origBuf limit last to ensure it hasn't been modified
                origLimit = origBuf.get().limit()
            }

        assertThat(origLimit).isEqualTo(16)
        assertThat(sharedInstanceLimit0).isEqualTo(8)
        assertThat(sharedInstanceLimit1).isEqualTo(sharedInstanceLimit0)
    }

    @Test
    fun positionIsTransferred_toChildSharedInstances() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(16)
        var origPos: Int
        var sharedInstancePos0: Int
        var sharedInstancePos1: Int
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }
            .use { origBuf ->
                origBuf.share().use { sharedBuf0 ->
                    sharedBuf0.get().position(8)
                    sharedInstancePos0 = sharedBuf0.get().position()

                    sharedBuf0.share().use { sharedBuf1 ->
                        sharedInstancePos1 = sharedBuf1.get().position()
                    }
                }

                // Check origBuf position last to ensure it hasn't been modified
                origPos = origBuf.get().position()
            }

        assertThat(origPos).isEqualTo(0)
        assertThat(sharedInstancePos0).isEqualTo(8)
        assertThat(sharedInstancePos1).isEqualTo(sharedInstancePos0)
    }

    @Test
    fun markIsTransferred_toChildSharedInstances() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val buf = ByteBuffer.allocate(16)
        var origMark: Int
        var sharedInstanceMark0: Int
        var sharedInstanceMark1: Int
        SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                // no-op close action
            }
            .use { origBuf ->
                origBuf.get().mark()
                origBuf.share().use { sharedBuf0 ->
                    sharedBuf0.get().apply {
                        position(8)
                        mark()
                        position(10)
                    }

                    sharedBuf0.share().use { sharedBuf1 ->
                        sharedInstanceMark1 =
                            with(sharedBuf1.get()) {
                                reset()
                                position()
                            }
                    }

                    sharedInstanceMark0 =
                        with(sharedBuf0.get()) {
                            reset()
                            position()
                        }
                }

                // Check origBuf mark last to ensure it hasn't been modified
                origMark =
                    with(origBuf.get()) {
                        reset()
                        position()
                    }
            }

        assertThat(origMark).isEqualTo(0)
        assertThat(sharedInstanceMark0).isEqualTo(8)
        assertThat(sharedInstanceMark1).isEqualTo(sharedInstanceMark0)
    }

    @Test
    @MediumTest
    fun closeAction_runsOnBackgroundThread_whenFinalInstanceClosedOnBackgroundThread(): Unit =
        runBlocking {
            // Skip for b/264902324
            assumeFalse(
                "Emulator API 30 crashes running this test.",
                Build.VERSION.SDK_INT == 30 && isEmulator(),
            )
            val buf = ByteBuffer.allocate(0)
            val closeActionThreadNameDeferred = CompletableDeferred<String>()
            val origBuf =
                SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                    closeActionThreadNameDeferred.complete(Thread.currentThread().name)
                }

            val origClosedDeferred = CompletableDeferred<Unit>()

            repeat(25) {
                val sharedBuf = origBuf.share()
                launch(Dispatchers.IO) {
                    // Wait for original to be closed on test runner instrumentation thread
                    origClosedDeferred.await()
                    sharedBuf.use {
                        // Should be able to retrieve buffer as long as sharedBuf is not closed
                        it.get()
                    }
                }
            }
            origBuf.close()
            origClosedDeferred.complete(Unit) // Allow background threads to close

            val closingThreadName = closeActionThreadNameDeferred.await()
            assertThat(closingThreadName).isNotEqualTo(Thread.currentThread().name)
        }

    @Test
    @LargeTest
    fun finalizeClosesUnclosedInstances() = runBlocking {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        assumeFalse(
            "Ignore devices that get flaky result. See b/278842333",
            isModel("moto c") || isModel("rne-l23"),
        )

        val buf = ByteBuffer.allocate(0)
        val closeActionDeferred = CompletableDeferred<Unit>()
        val origBuf =
            SharedByteBuffer.newSharedInstance(buf, CameraXExecutors.directExecutor()) {
                closeActionDeferred.complete(Unit)
            }

        val finalizeAwaitQueue = ReferenceQueue<SharedByteBuffer>()
        // Create 5 phantom reachable SharedByteBuffers
        val phantomReferences = List(5) { PhantomReference(origBuf.share(), finalizeAwaitQueue) }
        try {
            // Close original buffer. Only phantomly reachable instances will now exist.
            origBuf.close()
            // Run gc until all finalizers have fun or timeout occurs
            withTimeout(timeMillis = 1000) {
                withContext(Dispatchers.IO) {
                    var numFinalized = 0
                    while (isActive && numFinalized < phantomReferences.size) {
                        Runtime.getRuntime().gc()
                        Runtime.getRuntime().runFinalization()
                        delay(timeMillis = 50) // Give some time for finalizers to complete
                        while (finalizeAwaitQueue.poll() != null) {
                            numFinalized++
                        }
                    }
                }
            }

            // Finalizers will have caused close action to run
            assertThat(closeActionDeferred.isCompleted).isTrue()
        } finally {
            // Clean up. Shouldn't be strictly necessary but using the `phantomReferences`
            // variable here will guarantee references are phantom reachable until test is
            // complete, just in case the compiler decides to do some optimizing transformations
            // with stack references.
            phantomReferences.forEach { it.clear() }
        }
    }

    private fun isModel(model: String) = model.equals(Build.MODEL, true)
}
