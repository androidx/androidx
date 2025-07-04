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

package androidx.datastore.core

import java.io.File
import java.io.IOException

/**
 * Utility class that attaches filestat info to crash reports.
 *
 * How to investigate a crash report using this class:
 * 1. If the crash report retained an "attachFileSystemMessage" exception message, look at that.
 * 2. Match the "attachFileStacktrace" line number in the stack trace to that of the code. The
 *    surrounding "if" statements indicate filestat info about the on disk file.
 * 3. Match the "attachParentStacktrace" line number in the stack trace to that of the code. The
 *    surrounding "if" statements indicate filestat info about the on disk file's parent directory.
 *    For example, seeing attachFileDebugInfo line 44 indicates we have file read permission but not
 *    write permission.
 */
internal object FileDiagnostics {
    fun attachFileDebugInfo(file: File, cause: IOException): IOException {
        // Note: these checks are "unrolled" in order to transmit them as part of the stack trace.
        return if (file.exists()) {
            if (file.isFile()) {
                if (file.canRead()) {
                    if (file.canWrite()) {
                        attachParentStacktrace(file, cause)
                    } else {
                        attachParentStacktrace(file, cause)
                    }
                } else {
                    if (file.canWrite()) {
                        attachParentStacktrace(file, cause)
                    } else {
                        attachParentStacktrace(file, cause)
                    }
                }
            } else {
                if (file.canRead()) {
                    if (file.canWrite()) {
                        attachParentStacktrace(file, cause)
                    } else {
                        attachParentStacktrace(file, cause)
                    }
                } else {
                    if (file.canWrite()) {
                        attachParentStacktrace(file, cause)
                    } else {
                        attachParentStacktrace(file, cause)
                    }
                }
            }
        } else {
            // No need to check isFile/canRead/canWrite: they'll all return false.
            attachParentStacktrace(file, cause)
        }
    }

    private fun attachParentStacktrace(file: File, cause: IOException): IOException {
        val parent: File? = file.getParentFile()
        if (parent == null) {
            return attachFileSystemMessage(file, cause)
        }

        return if (parent.exists()) {
            if (parent.isFile()) {
                if (parent.canRead()) {
                    if (parent.canWrite()) {
                        attachFileSystemMessage(file, cause)
                    } else {
                        attachFileSystemMessage(file, cause)
                    }
                } else {
                    if (parent.canWrite()) {
                        attachFileSystemMessage(file, cause)
                    } else {
                        attachFileSystemMessage(file, cause)
                    }
                }
            } else {
                if (parent.canRead()) {
                    if (parent.canWrite()) {
                        attachFileSystemMessage(file, cause)
                    } else {
                        attachFileSystemMessage(file, cause)
                    }
                } else {
                    if (parent.canWrite()) {
                        attachFileSystemMessage(file, cause)
                    } else {
                        attachFileSystemMessage(file, cause)
                    }
                }
            }
        } else {
            // No need to check isFile/canRead/canWrite: they'll all return false.
            attachFileSystemMessage(file, cause)
        }
    }

    private fun attachFileSystemMessage(file: File, origException: IOException): IOException {
        val message = buildString {
            append("Inoperable file:")
            try {
                append(" canonical[${file.getCanonicalPath()}] freeSpace[${file.getFreeSpace()}]")
            } catch (ignored: IOException) {
                append(" failed to attach additional metadata")
            }
        }
        return IOException(message, origException)
    }
}
