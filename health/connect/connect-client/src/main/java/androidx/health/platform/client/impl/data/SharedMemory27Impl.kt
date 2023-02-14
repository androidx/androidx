/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.platform.client.impl.data

import android.os.Build
import android.os.Parcel
import android.os.SharedMemory
import android.system.OsConstants
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * Internal class to ensure calls to shared memory are guarded, so that
 *
 * @suppress
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
object SharedMemory27Impl {
    /** Flattens `bytes` into `dest` using [SharedMemory]. */
    @DoNotInline
    fun writeToParcelUsingSharedMemory(name: String, bytes: ByteArray, dest: Parcel, flags: Int) {
        SharedMemory.create(name, bytes.size).use { memory ->
            memory.setProtect(OsConstants.PROT_READ or OsConstants.PROT_WRITE)
            memory.mapReadWrite().put(bytes)
            memory.setProtect(OsConstants.PROT_READ)
            memory.writeToParcel(dest, flags)
        }
    }

    @DoNotInline
    fun <U : Any> parseParcelUsingSharedMemory(source: Parcel, parser: (ByteArray) -> U): U =
        SharedMemory.CREATOR.createFromParcel(source).use { memory ->
            val buffer = memory.mapReadOnly()
            val payload = ByteArray(buffer.remaining())
            buffer.get(payload)
            return parser(payload)
        }
}
