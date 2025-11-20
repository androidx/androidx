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

package androidx.pdf.selection

import androidx.pdf.PdfRect

/**
 * Represents PDF content that has been selected.
 *
 * @property bounds The [PdfRect] bounds of this selection. May contain multiple [PdfRect] if this
 *   selection spans multiple discrete areas within the PDF. Consider for example any selection
 *   spanning multiple pages, or a text selection spanning multiple lines on the same page.
 */
public interface Selection {
    public val bounds: List<PdfRect>
}
