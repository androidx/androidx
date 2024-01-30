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

package androidx.camera.camera2.pipe.integration.compat

import android.graphics.SurfaceTexture
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.ImageFormatConstants

@RequiresApi(21)
internal open class StreamConfigurationMapCompatBaseImpl(
    val streamConfigurationMap: StreamConfigurationMap?
) :
    StreamConfigurationMapCompat.StreamConfigurationMapCompatImpl {
    override fun getOutputSizes(format: Int): Array<Size>? {
        val sizes: Array<Size> =
            if (format == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // This is a little tricky that 0x22 that is internal defined in
                // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is public
                // after Android level 23 but not public in Android L. Use {@link SurfaceTexture}
                // or {@link MediaCodec} will finally mapped to 0x22 in StreamConfigurationMap to
                // retrieve the output sizes information.
                streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()
            } else {
                streamConfigurationMap?.getOutputSizes(format) ?: emptyArray()
            }
        return sizes
    }

    override fun <T> getOutputSizes(klass: Class<T>): Array<Size>? {
        return streamConfigurationMap?.getOutputSizes(klass) ?: emptyArray()
    }

    override fun getHighResolutionOutputSizes(format: Int): Array<Size>? {
        return null
    }

    override fun getOutputMinFrameDuration(format: Int, size: Size?): Long? {
        return streamConfigurationMap?.getOutputMinFrameDuration(format, size)
    }

    override fun unwrap(): StreamConfigurationMap? {
        return streamConfigurationMap
    }
}
