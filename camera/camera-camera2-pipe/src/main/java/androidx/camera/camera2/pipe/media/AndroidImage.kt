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

import android.media.Image
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.common.AndroidImage as CommonAndroidImage
import androidx.camera.common.ImageWrapper
import androidx.camera.common.MutableImageWrapper
import java.lang.Class

/**
 * An [ImageWrapper] backed by an [Image].
 *
 * Note: [Image] is not thread-safe, so all interactions with the underlying properties must be
 * copied into local fields or guarded by a lock.
 */
public class AndroidImage private constructor(private val delegate: CommonAndroidImage) :
    MutableImageWrapper by delegate {
    public constructor(image: Image) : this(CommonAndroidImage(image))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> delegate.unwrapAs(type)
        }

    override fun toString(): String {
        // Image will be written as "Image-YUV_444_888w640h480-t1234567890" with format, width,
        // height, and timestamp
        return "Image-${StreamFormat(format).name}-w${width}h$height-t$timestamp"
    }
}
