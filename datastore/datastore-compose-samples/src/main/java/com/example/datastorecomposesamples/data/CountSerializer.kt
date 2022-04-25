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

package com.example.datastorecomposesamples.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.example.datastorecomposesamples.CountPreferences
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles converting the CountPreferences to and from an OutputStream for storing in protos.
 */
object CountSerializer : Serializer<CountPreferences> {

    override val defaultValue: CountPreferences = CountPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CountPreferences {
        try {
            return CountPreferences.parseFrom(input)
        } catch (ipbe: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", ipbe)
        }
    }

    override suspend fun writeTo(t: CountPreferences, output: OutputStream) = t.writeTo(output)
}