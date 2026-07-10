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

package androidx.pdf

/**
 * Represents an exception that occurred while applying edits on a PDF document.
 *
 * @param failureIndex The index of the operation that caused the failure, relative to the order in
 *   which operations were enqueued in the [EditsDraft].
 * @param appliedEditIds A list of unique identifiers for the annotations that were successfully
 *   edited (inserted, updated, or removed) before the failure occurred.
 * @param cause The underlying cause of the failure.
 */
public class PdfEditApplyException(
    public val failureIndex: Int,
    public val appliedEditIds: List<String>,
    cause: Throwable,
) : Exception(cause)
