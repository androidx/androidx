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
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageSource
import androidx.camera.camera2.pipe.media.ImageSourceListener
import kotlin.reflect.KClass

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class FakeImageSource(
    private val streamId: StreamId,
    private val streamFormat: StreamFormat,
    private val outputSizes: Map<OutputId, Size>,
) : ImageSource {
    private var listener: ImageSourceListener? = null
    private val fakeSurface = FakeSurfaces.create(outputSizes.values.first())
    override val surface: Surface
        get() = fakeSurface

    fun simulateImage(
        timestamp: Long,
        outputId: OutputId? = null,
    ): FakeImage {
        val id = outputId ?: outputSizes.keys.single()
        val size = outputSizes[id]!!
        val fakeImage = FakeImage(size.width, size.height, streamFormat.value, timestamp)
        listener?.onImage(streamId, id, timestamp, fakeImage)
        return fakeImage
    }

    fun simulateMissingImage(timestamp: Long, outputId: OutputId? = null) {
        val id = outputId ?: outputSizes.keys.single()
        listener?.onImage(streamId, id, timestamp, null)
    }

    override fun setListener(listener: ImageSourceListener) {
        this.listener = listener
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    override fun close() {
        listener = null
        fakeSurface.release()
    }
}
