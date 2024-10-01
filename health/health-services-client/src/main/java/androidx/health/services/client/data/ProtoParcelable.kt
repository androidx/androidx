/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.MessageLite

/**
 * Base class for parcelables backed by Protocol Buffers.
 *
 * Provided [proto] represents everything important to subclasses, they need not implement [equals]
 * and [hashCode].
 */
@Suppress("ParcelCreator", "ParcelNotFinal")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class ProtoParcelable<T : MessageLite> : Parcelable {

    /** Proto representation of this object. */
    public abstract val proto: T

    /** Serialized representation of this object. */
    protected val bytes: ByteArray
        get() {
            return proto.toByteArray()
        }

    public override fun describeContents(): Int = 0

    public override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(this.bytes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        // It's possible that two objects from different classes have the same serialized form,
        // however
        // they shouldn't be considered equal unless they're of the same class.
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val that = other as ProtoParcelable<*>
        return bytes.contentEquals(that.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    public companion object {
        /**
         * Constructs and returns a [Creator] based on the provided [parser] accepting a [ByteArray]
         * .
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public inline fun <reified U : ProtoParcelable<*>> newCreator(
            crossinline parser: (ByteArray) -> U
        ): Creator<U> {
            return object : Creator<U> {
                override fun createFromParcel(source: Parcel): U? {
                    val payload: ByteArray = source.createByteArray() ?: return null
                    return parser(payload)
                }

                override fun newArray(size: Int) = arrayOfNulls<U>(size)
            }
        }
    }
}
