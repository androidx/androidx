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

package androidx.pdf.content

import android.graphics.RectF
import android.net.Uri

/**
 * Represents the bounds and link on a page of the PDF document. Weblinks are those links implicitly
 * embedded in PDF pages. Note: Only weblinks that are embedded will be supported. Links encoded as
 * plain text will be returned as part of [PdfPageTextContent].
 *
 * @param bounds: Bounds which envelop the URI.
 * @param uri: Uri embedded in the PDF document.
 */
public class PdfPageLinkContent(bounds: List<RectF>, public val uri: Uri) : PdfPageContent(bounds)
