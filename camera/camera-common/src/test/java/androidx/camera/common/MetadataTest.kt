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

package androidx.camera.common

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public class MetadataTest {

    @Test
    public fun keysWithSameNameAreSameInstance() {
        val key1 = Metadata.Key<String>("metadata.test.key")
        val key2 = Metadata.Key<String>("metadata.test.key")
        assertThat(key1).isSameInstanceAs(key2)
    }

    @Test
    public fun keysWithDifferentNamesAreNotSameInstance() {
        val key1 = Metadata.Key<String>("metadata.test.key1")
        val key2 = Metadata.Key<String>("metadata.test.key2")
        assertThat(key1).isNotSameInstanceAs(key2)
    }

    @Test
    public fun keysWithSameNameAndDifferentTypesThrowsExceptions() {
        Metadata.Key<String>("metadata.test.key.diff")
        assertThrows(IllegalStateException::class.java) {
            Metadata.Key<Int>("metadata.test.key.diff")
        }
    }

    @Test
    public fun cameraIdBehavior() {
        val cameraId = CameraId("0")
        assertThat(cameraId.value).isEqualTo("0")
        assertThat(cameraId.toString()).isEqualTo("CameraId-0")
    }

    @Test
    public fun cameraFrameNumberBehavior() {
        val frameNumber = CameraFrameNumber(42L)
        assertThat(frameNumber.value).isEqualTo(42L)
        assertThat(frameNumber.toString()).isEqualTo("Frame-42")
    }

    @Test
    public fun cameraFrameNumberValidation() {
        assertThrows(IllegalArgumentException::class.java) { CameraFrameNumber(-1L) }
    }
}
