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

package androidx.pdf.content

import android.net.Uri

/**
 * Represents an external link on a page of the PDF document. External links typically point to web
 * URLs or other resources that are meant to be handled by external applications (e.g., browsers).
 *
 * @param uri The [Uri] extracted from the PDF document, representing the destination of the
 *   external link.
 */
public class ExternalLink(public val uri: Uri) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExternalLink) return false
        return uri == other.uri
    }

    override fun hashCode(): Int = uri.hashCode()

    override fun toString(): String = "ExternalLink(uri=$uri)"
}
