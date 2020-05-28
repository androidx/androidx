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

package androidx.datastore

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The serializer determines the on-disk format and API for accessing it.
 *
 * The type T MUST be immutable. Mutable types will result in broken DataStore functionality.
 *
 * TODO(b/151635324): consider changing InputStream to File.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
interface Serializer<T> {

    /** Unmarshal object from stream. */
    fun readFrom(input: InputStream): T

    /** Marshal object to a stream. */
    fun writeTo(t: T, output: OutputStream)

    /**
     * The initial value of the serialized object. This value be returned if the file does
     * not yet exist on disk.
     */
    val defaultValue: T

    /**
     * The extension required for files that this serializer can act on.
     */
    val fileExtension: String
}

/**
 * A subclass of IOException that indicates that the file could not be de-serialized due
 * to data format corruption. This exception should not be thrown when the IOException is
 * due to a transient IO issue or permissions issue.
 */
class CorruptionException(message: String, cause: Throwable? = null) :
    IOException(message, cause)