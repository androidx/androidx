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

package androidx.camera.camera2.pipe.testing

import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import androidx.camera.camera2.pipe.media.ImageWrapper
import androidx.camera.common.ImageDataSpace
import androidx.camera.common.ImagePlane
import androidx.camera.common.MutableImageWrapper
import androidx.camera.common.testing.FakeImage as CommonFakeImage
import java.lang.Class

/** FakeImage that can be used for testing classes that accept [ImageWrapper]. */
public class FakeImage(
    override val width: Int,
    override val height: Int,
    override val format: Int,
    timestamp: Long,
    // TODO(b/516888993): Remove providedHardwareBuffer once Google3 tests are migrated.
    providedHardwareBuffer: HardwareBuffer? = null,
    cropRect: Rect = Rect(0, 0, width, height),
    hardwareBuffer: HardwareBuffer? = null,
) : ImageWrapper, MutableImageWrapper {

    private val delegate =
        CommonFakeImage(
            width = width,
            height = height,
            format = format,
            timestamp = timestamp,
            hardwareBuffer = providedHardwareBuffer ?: hardwareBuffer,
            cropRect = cropRect,
        )

    override val hardwareBuffer: HardwareBuffer?
        get() = delegate.hardwareBuffer

    override var timestamp: Long
        get() = delegate.timestamp
        set(value) {
            delegate.timestamp = value
        }

    override var cropRect: Rect
        get() = delegate.cropRect
        set(value) {
            delegate.cropRect = value
        }

    override var syncFence: SyncFence?
        get() = delegate.syncFence
        set(value) {
            delegate.syncFence = value
        }

    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int
        get() = delegate.dataSpace
        set(value) {
            delegate.dataSpace = value
        }

    public val numberOfTimesClosed: Int
        get() = delegate.closeCount

    public val isClosed: Boolean
        get() = delegate.isClosed

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> delegate.unwrapAs(type)
        }

    @get:Deprecated("Use imagePlanes instead", ReplaceWith("imagePlanes"))
    override val planes: List<ImagePlane>
        get() = imagePlanes

    override val imagePlanes: List<ImagePlane>
        get() = delegate.imagePlanes

    override fun close() {
        delegate.close()
    }

    override fun toString(): String = delegate.toString()
}
