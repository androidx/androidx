/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.proto

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.glance.appwidget.proto.LayoutProto.LayoutConfig
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Serializes the proto for use with DataStore. */
public object LayoutProtoSerializer : Serializer<LayoutConfig> {

    override val defaultValue: LayoutConfig = LayoutConfig.getDefaultInstance()

    @Throws(CorruptionException::class)
    override suspend fun readFrom(input: InputStream): LayoutConfig {
        try {
            return LayoutConfig.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: LayoutConfig, output: OutputStream) = t.writeTo(output)
}
