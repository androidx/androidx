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

package androidx.camera.camera2.pipe.media

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import kotlin.reflect.KClass

/**
 * An OutputImage is a reference to an [ImageWrapper] that was produced from CameraPipe for a
 * specific [StreamId]/[OutputId] combination.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
interface OutputImage : ImageWrapper {
    val streamId: StreamId
    val outputId: OutputId

    companion object {
        fun from(image: ImageWrapper, streamId: StreamId, outputId: OutputId): OutputImage {
            return OutputImageImpl(image, streamId, outputId)
        }

        private class OutputImageImpl(
            private val image: ImageWrapper,
            override val streamId: StreamId,
            override val outputId: OutputId
        ) : ImageWrapper by image, OutputImage {
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> unwrapAs(type: KClass<T>): T? =
                when (type) {
                    OutputImage::class -> this as T?
                    ImageWrapper::class -> this as T?
                    else -> image.unwrapAs(type)
                }
        }
    }
}
