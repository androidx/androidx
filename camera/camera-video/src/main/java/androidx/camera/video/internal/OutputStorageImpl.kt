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

import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.camera.video.internal.utils.StorageUtil.getAvailableBytes
import androidx.camera.video.internal.utils.StorageUtil.getAvailableBytesForMediaStoreUri

/**
 * An implementation of [OutputStorage] that determines available storage space based on the
 * provided [OutputOptions].
 *
 * @param outputOptions The output options to evaluate for storage space.
 */
public class OutputStorageImpl(private val outputOptions: OutputOptions) : OutputStorage {

    override fun getOutputOptions(): OutputOptions = outputOptions

    /**
     * Gets the available bytes for the storage associated with the [outputOptions].
     *
     * @return The available space in bytes or [Long.MAX_VALUE] if the [outputOptions] is a
     *   [FileDescriptorOutputOptions].
     * @throws AssertionError if the [outputOptions] type is unknown.
     */
    override fun getAvailableBytes(): Long =
        when (outputOptions) {
            is FileOutputOptions -> getAvailableBytes(outputOptions.file.path)
            is MediaStoreOutputOptions ->
                getAvailableBytesForMediaStoreUri(outputOptions.collectionUri)
            // There's no way to get the storage space associated with a FileDescriptor.
            is FileDescriptorOutputOptions -> Long.MAX_VALUE
            else -> throw AssertionError("Unknown OutputOptions: $outputOptions")
        }

    private companion object {
        private const val TAG = "OutputStorageImpl"
    }
}
