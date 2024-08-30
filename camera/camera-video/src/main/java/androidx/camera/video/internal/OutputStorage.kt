/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.video.internal

import androidx.camera.video.OutputOptions

/**
 * Represents a storage location associated with [OutputOptions], providing access to its available
 * space.
 */
public interface OutputStorage {

    /**
     * Gets the [OutputOptions] associated with this storage.
     *
     * @return The associated OutputOptions.
     */
    public fun getOutputOptions(): OutputOptions

    /**
     * Gets the available bytes for the storage.
     *
     * @return The available space in bytes.
     */
    public fun getAvailableBytes(): Long

    /** A factory interface for creating [OutputStorage] instances. */
    public interface Factory {

        /**
         * Creates an [OutputStorage] instance based on the provided [OutputOptions].
         *
         * @param outputOptions The output options to configure the storage.
         * @return A new OutputStorage instance.
         */
        public fun create(outputOptions: OutputOptions): OutputStorage
    }
}
