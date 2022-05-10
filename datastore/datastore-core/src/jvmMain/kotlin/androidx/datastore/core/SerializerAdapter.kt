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

package androidx.datastore.core

import androidx.datastore.io.DatastoreInput
import androidx.datastore.io.DatastoreOutput


internal class SerializerAdapter<T>(val serializer: Serializer<T>)
    : androidx.datastore.io.Serializer<T> {
    override val defaultValue: T
        get() = serializer.defaultValue

    override suspend fun readFrom(input: DatastoreInput): T {
        return serializer.readFrom(input.toInputStream())
    }

    override suspend fun writeTo(value: T, output: DatastoreOutput) {
        serializer.writeTo(value, output.toOutputStream())
    }
}