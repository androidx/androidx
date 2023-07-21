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
import android.os.Bundle
import android.os.SharedMemory
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.watchface.utility.TraceEvent
import java.nio.ByteBuffer

/**
 * This class requires API level 27 and is only intended for use in conjunction with
 * wear-watchface-client which also requires API level 27.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SharedMemoryImage {
    @RequiresApi(27)
    public companion object {
        /** Stores a [Bitmap] in shared memory and serializes it as a bundle. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("DEPRECATION")
        public fun ashmemWriteImageBundle(bitmap: Bitmap): Bundle =
            TraceEvent("SharedMemoryImage.ashmemWriteImageBundle").use {
                val ashmem =
                    SharedMemory.create("WatchFace.Screenshot.Bitmap", bitmap.allocationByteCount)
                var byteBuffer: ByteBuffer? = null
                try {
                    byteBuffer = ashmem.mapReadWrite()
                    bitmap.copyPixelsToBuffer(byteBuffer)
                    return Bundle().apply {
                        this.putInt(Constants.KEY_BITMAP_WIDTH_PX, bitmap.width)
                        this.putInt(Constants.KEY_BITMAP_HEIGHT_PX, bitmap.height)
                        this.putInt(Constants.KEY_BITMAP_CONFIG_ORDINAL, bitmap.config.ordinal)
                        this.putParcelable(Constants.KEY_SCREENSHOT, ashmem)
                    }
                } finally {
                    if (byteBuffer != null) {
                        SharedMemory.unmap(byteBuffer)
                    }
                }
            }

        /** Deserializes a [Bundle] containing a [Bitmap] serialized by [ashmemWriteImageBundle]. */
        @Suppress("DEPRECATION")
        public fun ashmemReadImageBundle(bundle: Bundle): Bitmap =
            TraceEvent("SharedMemoryImage.ashmemReadImageBundle").use {
                bundle.classLoader = SharedMemory::class.java.classLoader
                val ashmem =
                    bundle.getParcelable<SharedMemory>(Constants.KEY_SCREENSHOT)
                        ?: throw IllegalStateException(
                            "Bundle did not contain " + Constants.KEY_SCREENSHOT
                        )
                val width = bundle.getInt(Constants.KEY_BITMAP_WIDTH_PX)
                val height = bundle.getInt(Constants.KEY_BITMAP_HEIGHT_PX)
                val configOrdinal = bundle.getInt(Constants.KEY_BITMAP_CONFIG_ORDINAL)
                var byteBuffer: ByteBuffer? = null
                try {
                    val bitmap =
                        Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.values().find { it.ordinal == configOrdinal }!!
                        )
                    byteBuffer = ashmem.mapReadOnly()
                    bitmap.copyPixelsFromBuffer(byteBuffer)
                    return bitmap
                } finally {
                    if (byteBuffer != null) {
                        SharedMemory.unmap(byteBuffer)
                    }
                }
            }
    }
}
