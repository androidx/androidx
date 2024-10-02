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

import android.os.Bundle
import androidx.health.services.client.proto.ByteString
import androidx.health.services.client.proto.DataProto

/** Utility methods for working with Bundles. */
internal object BundlesUtil {

    @JvmStatic
    internal fun toProto(bundle: Bundle): DataProto.Bundle {
        val builder = DataProto.Bundle.newBuilder()

        for (key in bundle.keySet()) {
            @Suppress("DEPRECATION")
            when (val value = bundle.get(key)) {
                is Boolean -> builder.putBools(key, value)
                is String -> builder.putStrings(key, value)
                is Int -> builder.putInts(key, value)
                is Long -> builder.putLongs(key, value)
                is Float -> builder.putFloats(key, value)
                is Double -> builder.putDoubles(key, value)
                is Byte -> builder.putBytes(key, value.toInt())
                is ByteArray -> builder.putByteArrays(key, ByteString.copyFrom(value))
                is Bundle -> if (value != bundle) builder.putBundles(key, toProto(value))
            }
        }

        return builder.build()
    }

    @JvmStatic
    internal fun fromProto(proto: DataProto.Bundle): Bundle {
        val bundle = Bundle()

        proto.boolsMap.forEach { bundle.putBoolean(it.key, it.value) }
        proto.stringsMap.forEach { bundle.putString(it.key, it.value) }
        proto.intsMap.forEach { bundle.putInt(it.key, it.value) }
        proto.longsMap.forEach { bundle.putLong(it.key, it.value) }
        proto.floatsMap.forEach { bundle.putFloat(it.key, it.value) }
        proto.doublesMap.forEach { bundle.putDouble(it.key, it.value) }
        proto.bytesMap.forEach { bundle.putByte(it.key, it.value.toByte()) }
        proto.byteArraysMap.forEach { bundle.putByteArray(it.key, it.value.toByteArray()) }
        proto.bundlesMap.forEach { bundle.putBundle(it.key, fromProto(it.value)) }

        return bundle
    }
}
