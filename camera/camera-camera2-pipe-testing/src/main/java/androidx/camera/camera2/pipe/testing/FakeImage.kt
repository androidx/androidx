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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.media.ImagePlane
import androidx.camera.camera2.pipe.media.ImageWrapper
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * FakeImage that can be used for testing classes that accept [ImageWrapper].
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class FakeImage(
    override val width: Int,
    override val height: Int,
    override val format: Int,
    override val timestamp: Long
) : ImageWrapper {
    private val closed = atomic(false)
    val isClosed: Boolean
        get() = closed.value
    override val planes: List<ImagePlane>
        get() = throw UnsupportedOperationException("FakeImage does not support planes.")

    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        // FakeImage cannot be unwrapped
        return null
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            // FakeImage close is a NoOp
        }
    }
}
