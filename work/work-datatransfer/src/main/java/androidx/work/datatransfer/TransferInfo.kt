/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work.datatransfer

/**
 * A container class holding byte transfer information related to a [UserInitiatedTaskRequest].
 * Specifically, the estimated number of upload or download bytes which are expected to be
 * transferred.
 */
class TransferInfo(
    /**
     * The estimated number of bytes to be uploaded, if applicable.
     */
    val estimatedUploadBytes: Long = 0L,
    /**
     * The estimated number of bytes to be downloaded, if applicable.
     */
    val estimatedDownloadBytes: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as TransferInfo
        return estimatedUploadBytes == that.estimatedUploadBytes &&
                estimatedDownloadBytes == that.estimatedDownloadBytes
    }

    override fun hashCode(): Int {
        var result = (estimatedUploadBytes xor (estimatedUploadBytes ushr 32)).toInt()
        result = 31 * result + (estimatedDownloadBytes xor (estimatedDownloadBytes ushr 32)).toInt()
        return result
    }
}
