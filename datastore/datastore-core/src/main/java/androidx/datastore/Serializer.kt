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

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The serializer determines the on-disk format and API for accessing it.
 *
 * The type T MUST be immutable. Mutable types will result in broken DataStore functionality.
 *
 * TODO(b/151635324): consider changing InputStream to File.
 */
public interface Serializer<T> {

    /**
     * Unmarshal object from stream. This must be able to handle an empty input stream - which is
     * passed in as a default value.
     */
    public fun readFrom(input: InputStream): T

    /** Marshal object to a stream. */
    public fun writeTo(t: T, output: OutputStream)
}

/**
 * A subclass of IOException that indicates that the file could not be de-serialized due
 * to data format corruption. This exception should not be thrown when the IOException is
 * due to a transient IO issue or permissions issue.
 */
public class CorruptionException(message: String, cause: Throwable? = null) :
    IOException(message, cause)