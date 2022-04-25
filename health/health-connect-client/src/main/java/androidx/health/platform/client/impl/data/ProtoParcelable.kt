/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.os.SharedMemory
import android.system.OsConstants
import androidx.annotation.RestrictTo
import com.google.protobuf.MessageLite

/**
 * Base class for parcelables backed by protos.
 *
 * Provided [proto] represents everything important to subclasses, they need not implement [equals]
 * and [hashCode].
 */
@Suppress("ParcelCreator", "ParcelNotFinal")
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class ProtoParcelable<T : MessageLite> : ProtoData<T>(), Parcelable {

    /** Serialized representation of this object. */
    private val bytes: ByteArray by lazy { proto.toByteArray() }

    override fun describeContents(): Int {
        return if (shouldStoreInPlace()) 0 else Parcelable.CONTENTS_FILE_DESCRIPTOR
    }

    /**
     * Flattens the underlying proto into `dest`.
     *
     * @see Parcelable.writeToParcel
     */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (shouldStoreInPlace()) {
            dest.writeInt(STORE_IN_PLACE)
            dest.writeByteArray(bytes)
        } else {
            dest.writeInt(STORE_SHARED_MEMORY)
            writeToParcelUsingSharedMemory(dest, flags)
        }
    }

    /**
     * Flattens the underlying proto into `dest` using [SharedMemory].
     *
     * @see Parcelable.writeToParcel
     */
    @Suppress("NewApi") // API only ever used on SDK 27 and above.
    private fun writeToParcelUsingSharedMemory(dest: Parcel, flags: Int) {
        SharedMemory.create("ProtoParcelable", bytes.size).use { memory ->
            memory.setProtect(OsConstants.PROT_READ or OsConstants.PROT_WRITE)
            memory.mapReadWrite().put(bytes)
            memory.setProtect(OsConstants.PROT_READ)
            memory.writeToParcel(dest, flags)
        }
    }

    /** Returns whether the underlying proto should be stored as an in-place [ByteArray]. */
    private fun shouldStoreInPlace(): Boolean {
        return bytes.size <= MAX_IN_PLACE_SIZE
    }

    companion object {
        /**
         * Constructs and returns a [Creator] based on the provided [parser] accepting a [ByteArray]
         * .
         */
        inline fun <reified U : ProtoParcelable<*>> newCreator(
            crossinline parser: (ByteArray) -> U
        ): Creator<U> {
            return object : Creator<U> {
                @Suppress("NewApi") // API only ever used on SDK 27 and above.
                override fun createFromParcel(source: Parcel): U? {
                    when (val storage = source.readInt()) {
                        STORE_IN_PLACE -> {
                            val payload: ByteArray = source.createByteArray() ?: return null
                            return parser(payload)
                        }
                        STORE_SHARED_MEMORY -> {
                            SharedMemory.CREATOR.createFromParcel(source).use { memory ->
                                val buffer = memory.mapReadOnly()
                                val payload = ByteArray(buffer.remaining())
                                buffer.get(payload)
                                return parser(payload)
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown storage: $storage")
                    }
                }

                override fun newArray(size: Int) = arrayOfNulls<U>(size)
            }
        }
    }
}

/** Flag marking that a proto is stored as an in-place `byte[]` array. */
const val STORE_IN_PLACE = 0

/** Flag marking that a proto is stored in [SharedMemory]. */
const val STORE_SHARED_MEMORY = 1

/** Maximum size of a proto stored as an in-place `byte[]` array (16 KiB). */
private const val MAX_IN_PLACE_SIZE = 16 * 1024
