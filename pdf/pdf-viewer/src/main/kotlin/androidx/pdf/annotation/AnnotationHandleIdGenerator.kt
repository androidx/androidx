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

package androidx.pdf.annotation

import androidx.annotation.RestrictTo
import java.util.UUID

/** Responsible for generating unique string identifiers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AnnotationHandleIdGenerator {
    private const val ANNOTATION_ID_PREFIX = "AnnotationId_"

    private const val ANNOTATION_ID_DELIMITER = "::"

    /**
     * Generates a strong pseudo-random unique identifier (UUID).
     *
     * @return A string representation of a random UUID
     */
    public fun generateId(): String = UUID.randomUUID().toString()

    /** Returns an annotation id in the format AnnotationId_<pageNum>::<string>. */
    public fun composeAnnotationId(pageNum: Int, id: String): String =
        "${ANNOTATION_ID_PREFIX}${pageNum}${ANNOTATION_ID_DELIMITER}${id}"

    public fun decomposeAnnotationId(key: String): Pair<Int, String> {
        require(key.startsWith(ANNOTATION_ID_PREFIX)) {
            "Invalid ID format: '$this' must start with prefix '$ANNOTATION_ID_PREFIX'"
        }

        val prefixLength = ANNOTATION_ID_PREFIX.length
        val delimiterIndex = key.indexOf(ANNOTATION_ID_DELIMITER, startIndex = prefixLength)

        require(delimiterIndex != -1) {
            "Invalid ID format: '$this' missing delimiter '$ANNOTATION_ID_DELIMITER'"
        }

        val pageNumStr = key.substring(prefixLength, delimiterIndex)
        val pageNum =
            pageNumStr.toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Invalid ID format: Page segment '$pageNumStr' is not a valid integer"
                )

        val originalString = key.substring(delimiterIndex + ANNOTATION_ID_DELIMITER.length)

        return pageNum to originalString
    }
}
