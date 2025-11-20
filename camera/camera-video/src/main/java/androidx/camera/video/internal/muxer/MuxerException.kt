/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video.internal.muxer

/** Exception thrown when a muxer operation fails. */
public class MuxerException : Exception {
    /** Constructs a new [MuxerException]. */
    public constructor() : super()

    /**
     * Constructs a new [MuxerException] with a message.
     *
     * @param message The detail message.
     */
    public constructor(message: String?) : super(message)

    /**
     * Constructs a new [MuxerException] with a message and a cause.
     *
     * @param message The detail message.
     * @param cause The cause of this exception.
     */
    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * Constructs a new [MuxerException] with a cause.
     *
     * @param cause The cause of this exception.
     */
    public constructor(cause: Throwable?) : super(cause)
}
