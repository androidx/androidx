/*
 * Copyright 2020 The Android Open Source Project
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

package android.support.wearable.watchface

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SharedMemory
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * This class requires API level 27 and is only intended for use in conjunction with
 * wear-watchface-client which also requires API level 27.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SharedMemoryImage {
    @RequiresApi(27)
    public companion object {
        /**
         * WebP compresses a [Bitmap] with the specified quality (100 = lossless) which is
         * stored in shared memory and serialized to a bundle.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("DEPRECATION")
        public fun ashmemCompressedImageBundle(bitmap: Bitmap, quality: Int): Bundle {
            val stream = ByteArrayOutputStream()
            bitmap.compress(CompressFormat.WEBP, quality, stream)
            val bytes = stream.toByteArray()
            val ashmem = SharedMemory.create("WatchFace.Screenshot.Bitmap", bytes.size)
            var byteBuffer: ByteBuffer? = null
            try {
                byteBuffer = ashmem.mapReadWrite()
                byteBuffer.put(bytes)
                return Bundle().apply {
                    this.putParcelable(Constants.KEY_SCREENSHOT, ashmem)
                }
            } finally {
                if (byteBuffer != null) {
                    SharedMemory.unmap(byteBuffer)
                }
            }
        }

        /**
         * Deserializes a [Bundle] containing a [Bitmap] serialized by
         * [ashmemCompressedImageBundle].
         */
        public fun ashmemCompressedImageBundleToBitmap(bundle: Bundle): Bitmap {
            bundle.classLoader = SharedMemory::class.java.classLoader
            val ashmem = bundle.getParcelable<SharedMemory>(Constants.KEY_SCREENSHOT)
                ?: throw IllegalStateException("Bundle did not contain " + Constants.KEY_SCREENSHOT)
            var byteBuffer: ByteBuffer? = null
            try {
                byteBuffer = ashmem.mapReadOnly()
                val bufferBytes = ByteArray(byteBuffer.remaining())
                byteBuffer.get(bufferBytes)
                return BitmapFactory.decodeByteArray(bufferBytes, /* offset= */0, bufferBytes.size)
            } finally {
                if (byteBuffer != null) {
                    SharedMemory.unmap(byteBuffer)
                }
            }
        }
    }
}
