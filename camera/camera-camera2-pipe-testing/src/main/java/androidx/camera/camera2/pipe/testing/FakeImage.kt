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
import androidx.camera.common.ImageWrapper
import androidx.camera.common.MutableImageWrapper
import androidx.camera.common.testing.FakeImage as CommonFakeImage
import java.lang.Class

/** FakeImage that can be used for testing classes that accept [ImageWrapper]. */
public class FakeImage private constructor(private val delegate: CommonFakeImage) :
    MutableImageWrapper by delegate {
    public constructor(
        width: Int,
        height: Int,
        format: Int,
        timestamp: Long,
        cropRect: Rect = Rect(0, 0, width, height),
        hardwareBuffer: HardwareBuffer? = null,
    ) : this(
        CommonFakeImage(
            width = width,
            height = height,
            format = format,
            timestamp = timestamp,
            hardwareBuffer = hardwareBuffer,
            cropRect = cropRect,
        )
    )

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

    override fun toString(): String = delegate.toString()
}
