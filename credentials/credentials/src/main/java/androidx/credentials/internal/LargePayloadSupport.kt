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
package androidx.credentials.internal

import android.os.Bundle
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream

@RestrictTo(RestrictTo.Scope.LIBRARY)
/** LargePayloadSupport to support large payload over IPC. */
object LargePayloadSupport {
    /** Key for the large payload stored as a [ParcelFileDescriptor] in a [Bundle]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    const val EXTRA_LARGE_PAYLOAD = "androidx.credentials.provider.extra.LARGE_PAYLOAD"

    /** Key for the size of the large payload stored in a [Bundle]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private const val EXTRA_LARGE_PAYLOAD_SIZE =
        "androidx.credentials.provider.extra.LARGE_PAYLOAD_SIZE"

    internal const val TAG = "LargePayloadSupport"

    /**
     * Encodes a [Bundle] into a [ParcelFileDescriptor] using a temporary file, and returns a new
     * [Bundle] containing the [ParcelFileDescriptor] and the size of the payload.
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun encodeBundleToPfd(bundle: Bundle): Bundle? =
        Parcel.obtain().use { parcel ->
            parcel.writeBundle(bundle)
            val payload = parcel.marshall()
            val tempFile =
                File.createTempFile("bundle-payload-" + System.currentTimeMillis(), ".tmp")
            val fileOutputStream = FileOutputStream(tempFile)
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            tempFile.delete()
            fileOutputStream.use { it.write(payload) }
            val result = Bundle()
            // `Bundle#putParcelable` won't increment the reference count.
            result.putParcelable(EXTRA_LARGE_PAYLOAD, pfd)
            result.putInt(EXTRA_LARGE_PAYLOAD_SIZE, payload.size)
            return result
        }

    /** Decodes a [Bundle] from a [ParcelFileDescriptor] stored in the given [bundle]. */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun decodeBundleFromPfd(bundle: Bundle): Bundle? {
        val pfd =
            BundleCompat.getParcelable(
                bundle,
                EXTRA_LARGE_PAYLOAD,
                ParcelFileDescriptor::class.java,
            ) ?: return null
        val size = bundle.getInt(EXTRA_LARGE_PAYLOAD_SIZE)
        return Parcel.obtain().use { parcel ->
            val payload = ByteArray(size)
            DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(pfd)).use {
                it.readFully(payload)
            }
            parcel.unmarshall(payload, 0, payload.size)
            parcel.setDataPosition(0)
            return@use parcel.readBundle()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal inline fun <R> Parcel.use(block: (Parcel) -> R?) =
        try {
            block(this)
        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred during payload transfer", e)
            null
        } finally {
            recycle()
        }
}
