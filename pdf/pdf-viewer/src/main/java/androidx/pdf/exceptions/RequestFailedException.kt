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

package androidx.pdf.exceptions

import androidx.annotation.RestrictTo

/**
 * An exception thrown when a non-fatal exception occurred while processing user request.
 *
 * This exception provides information about the specific request that failed, along with the page
 * number associated with the failure.
 *
 * @property requestMetadata The metadata of the operation that failed (e.g., "pageInfo",
 *   "pageBitmap").
 * @property throwable original exception occurred.
 * @property showError indicator to show error message. Only display message for failures that
 *   disrupt user experience.
 * @property isFirstPageRendered Indicates whether the first page of the document was rendered.
 *   Should be set from [androidx.pdf.view.PdfView]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RequestFailedException(
    public val requestMetadata: RequestMetadata,
    public val throwable: Throwable,
    public val showError: Boolean = true,
    public val isFirstPageRendered: Boolean? = null,
) : RuntimeException() {

    override fun toString(): String =
        StringBuilder()
            .append("RequestFailedException: ")
            .append("$requestMetadata, ")
            .append("exception =$throwable, ")
            .append("showError =$showError, ")
            .append("isFirstPageRendered=$isFirstPageRendered")
            .toString()

    /** Create a deep-copy of this exception with the specified parameter. */
    public fun copy(
        isFirstPageRendered: Boolean? = this.isFirstPageRendered
    ): RequestFailedException =
        RequestFailedException(requestMetadata, throwable, showError, isFirstPageRendered)
}

/**
 * Metadata associated with a user request, providing context about the operation.
 *
 * @property requestName A descriptive name for the request (e.g., "pageInfo", "pageBitmap").
 * @property pageRange The page number(s) associated with the request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RequestMetadata(public val requestName: String, public val pageRange: IntRange) {
    override fun toString(): String =
        StringBuilder()
            .append("Request metadata: ")
            .append("requestName=$requestName, ")
            .append("pageRange=$pageRange")
            .toString()
}
