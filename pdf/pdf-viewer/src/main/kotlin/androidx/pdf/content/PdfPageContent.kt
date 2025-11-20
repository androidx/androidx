/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.content

import android.graphics.RectF

/**
 * Represents the content on a PDF page, such as text, images, uris, or other selectable elements.
 *
 * This is an abstract class that serves as a base for different types of page content.
 *
 * @param bounds: A list of rectangles defining the content's bounding boxes in PDF coordinates.
 */
public abstract class PdfPageContent internal constructor(public val bounds: List<RectF>)
