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

package androidx.camera.common.testing

import android.annotation.SuppressLint
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import android.os.Build
import androidx.camera.common.ImageDataSpace
import androidx.camera.common.ImageFormat
import androidx.camera.common.ImagePlane
import androidx.camera.common.MutableImageWrapper
import java.lang.Class
import kotlinx.atomicfu.atomic

/** FakeImage that can be used for testing classes that accept [MutableImageWrapper]. */
public class FakeImage(
    override val width: Int,
    override val height: Int,
    @ImageFormat override val format: Int,
    override var timestamp: Long,
    override val hardwareBuffer: HardwareBuffer? = null,
    override var cropRect: Rect = Rect(0, 0, width, height),
) : MutableImageWrapper {
    private val debugId = debugIds.incrementAndGet()
    private val _closeCount = atomic(0)
    public val isClosed: Boolean
        get() = _closeCount.value > 0

    public val closeCount: Int
        get() = _closeCount.value

    override var syncFence: SyncFence? = null

    @get:SuppressLint("MethodNameUnits")
    @set:SuppressLint("MethodNameUnits")
    @get:ImageDataSpace
    @setparam:ImageDataSpace
    @SuppressLint("WrongConstant")
    override var dataSpace: Int = android.hardware.DataSpace.DATASPACE_UNKNOWN

    override val imagePlanes: List<ImagePlane>
        get() = throw UnsupportedOperationException("FakeImage does not support imagePlanes.")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            Build.VERSION.SDK_INT >= 28 && type == HardwareBuffer::class.java ->
                hardwareBuffer as T?
            else -> null
        }

    override fun close() {
        _closeCount.incrementAndGet()
    }

    override fun toString(): String = "FakeImage-$debugId-$format-w${width}h$height-t$timestamp"

    private companion object {
        private val debugIds = atomic(0)
    }
}
