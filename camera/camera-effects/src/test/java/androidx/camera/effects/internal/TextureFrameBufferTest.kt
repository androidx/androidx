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

package androidx.camera.effects.internal

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Build
import android.view.Surface
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [TextureFrameBuffer].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TextureFrameBufferTest {

    companion object {
        private const val TIMESTAMP_1 = 11L
        private const val TIMESTAMP_2 = 22L
    }

    private lateinit var surfaceTexture1: SurfaceTexture
    private lateinit var surface1: Surface
    private val transform1 = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    private lateinit var surfaceTexture2: SurfaceTexture
    private lateinit var surface2: Surface
    private val transform2 = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.rotateM(this, 0, 90f, 0f, 0f, 1f)
    }

    @Before
    fun setUp() {
        surfaceTexture1 = SurfaceTexture(1)
        surface1 = Surface(surfaceTexture1)
        surfaceTexture2 = SurfaceTexture(2)
        surface2 = Surface(surfaceTexture2)
    }

    @After
    fun tearDown() {
        surface1.release()
        surfaceTexture1.release()
        surface2.release()
        surfaceTexture2.release()
    }

    @Test
    fun getLength_returnsCorrectLength() {
        // Arrange: create 3 textures.
        val textures = intArrayOf(1, 2, 3)
        // Act: create the buffer.
        val buffer = TextureFrameBuffer(textures)
        // Assert: the length is 3.
        assertThat(buffer.length).isEqualTo(3)
    }

    @Test
    fun getFrameToRender_returnsFilledFrame() {
        // Arrange: create a buffer with 1 texture and mark them filled.
        val buffer = TextureFrameBuffer(intArrayOf(1))
        buffer.frameToFill.markFilled(TIMESTAMP_1, transform1, surface1)

        // Act: get frame with the same timestamp.
        val frame = buffer.getFrameToRender(TIMESTAMP_1)!!

        // Assert: the frame has the correct values.
        assertThat(frame.textureId).isEqualTo(1)
        assertThat(frame.timestampNs).isEqualTo(TIMESTAMP_1)
        assertThat(frame.transform.contentEquals(transform1)).isTrue()
        assertThat(frame.transform).isNotSameInstanceAs(transform1)
        assertThat(frame.surface).isSameInstanceAs(surface1)
    }

    @Test
    fun getFrameToRender_returnsNullIfNotFound() {
        // Arrange: create a buffer with 1 texture and mark them filled.
        val buffer = TextureFrameBuffer(intArrayOf(1))
        buffer.frameToFill.markFilled(TIMESTAMP_1, transform1, surface1)
        // Act and assert: get frame with a different timestamp and it should be null.
        assertThat(buffer.getFrameToRender(TIMESTAMP_2)).isNull()
    }

    @Test
    fun getFrameToRender_markOlderFramesEmpty() {
        // Arrange: create a buffer with two textures and mark them filled.
        val buffer = TextureFrameBuffer(intArrayOf(1, 2))
        val frames = fillBufferWithTwoFrames(buffer)

        // Act: get frame2 for rendering.
        assertThat(buffer.getFrameToRender(TIMESTAMP_2)).isSameInstanceAs(frames.second)

        // Assert: frame1 is empty now.
        assertThat(frames.second.isEmpty).isFalse()
        assertThat(frames.first.isEmpty).isTrue()
    }

    @Test
    fun getFrameToFill_returnsOldestFrameWhenFull() {
        // Arrange: create a full buffer.
        val buffer = TextureFrameBuffer(intArrayOf(1, 2))
        val frames = fillBufferWithTwoFrames(buffer)
        // Act and assert: get frame to fill and it should be the first frame.
        assertThat(buffer.frameToFill).isSameInstanceAs(frames.first)
    }

    @Test
    fun getFrameToFill_returnsEmptyFrameWhenNotFull() {
        // Arrange: create a full buffer.
        val buffer = TextureFrameBuffer(intArrayOf(1, 2))
        val frames = fillBufferWithTwoFrames(buffer)
        // Mark the second frame empty.
        frames.second.markEmpty()
        // Act and assert: get frame to fill and it should be the second frame.
        assertThat(buffer.frameToFill).isSameInstanceAs(frames.second)
    }

    private fun fillBufferWithTwoFrames(
        buffer: TextureFrameBuffer
    ): Pair<TextureFrame, TextureFrame> {
        assertThat(buffer.length).isEqualTo(2)
        // Fill frame 1.
        val frame1 = buffer.frameToFill
        assertThat(frame1.textureId).isEqualTo(1)
        frame1.markFilled(TIMESTAMP_1, transform1, surface1)
        // Fill frame 2.
        val frame2 = buffer.frameToFill
        assertThat(frame2.textureId).isEqualTo(2)
        frame2.markFilled(TIMESTAMP_2, transform2, surface2)
        return Pair(frame1, frame2)
    }
}
