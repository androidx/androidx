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

package androidx.camera.core.imagecapture

import android.graphics.ImageFormat
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ProcessingNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ProcessingNodeTest {

    private lateinit var processingNodeIn: ProcessingNode.In

    private val node = ProcessingNode(mainThreadExecutor())

    @Before
    fun setUp() {
        processingNodeIn = ProcessingNode.In.of(ImageFormat.JPEG)
        node.transform(processingNodeIn)
    }

    @Test
    fun inMemoryInputPacket_callbackInvoked() {
        // Arrange.
        val callback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(createCaptureBundle(intArrayOf()), callback)
        val image = FakeImageProxy(FakeImageInfo())
        // Act.
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(callback.inMemoryResult).isEqualTo(image)
    }
}
