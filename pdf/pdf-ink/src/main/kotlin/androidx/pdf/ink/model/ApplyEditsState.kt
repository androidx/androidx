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

package androidx.pdf.ink.model

import androidx.pdf.PdfWriteHandle

/** Represents the state of the "apply edits" operation. */
internal sealed interface ApplyEditsState {
    /** Ready to trigger the apply operation. */
    object Ready : ApplyEditsState

    /** The operation is currently in progress. */
    object InProgress : ApplyEditsState

    /**
     * The operation completed successfully.
     *
     * @param handle A [PdfWriteHandle] to the editable document.
     */
    class Success(val handle: PdfWriteHandle) : ApplyEditsState

    /**
     * The operation failed.
     *
     * @param error The error that occurred.
     */
    class Failure(val error: Throwable) : ApplyEditsState
}
