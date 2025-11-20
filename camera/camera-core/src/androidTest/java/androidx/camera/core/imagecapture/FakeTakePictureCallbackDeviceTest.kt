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
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Unit tests for [FakeTakePictureCallbackDeviceTest] */
class FakeTakePictureCallbackDeviceTest {

    private val fakeTakePictureCallback = FakeTakePictureCallback()

    @Test
    fun onDiskResultArrivesBeforeGet_canGetResult() = runBlocking {
        // Arrange.
        val onDiskResult = OutputFileResults(null, ImageFormat.JPEG)
        // Assert.
        fakeTakePictureCallback.onFinalResult(onDiskResult)
        // Act.
        assertThat(fakeTakePictureCallback.getOnDiskResult()).isEqualTo(onDiskResult)
    }

    @Test
    fun inMemoryResultArrivesBeforeGet_canGetResult() = runBlocking {
        // Arrange.
        val inMemoryResult = FakeImageProxy(FakeImageInfo())
        // Assert.
        fakeTakePictureCallback.onFinalResult(inMemoryResult)
        // Act.
        assertThat(fakeTakePictureCallback.getInMemoryResult()).isEqualTo(inMemoryResult)
    }
}
