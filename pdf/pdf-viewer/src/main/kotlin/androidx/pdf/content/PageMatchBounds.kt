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

/**
 * Represents the bounds of a single search match on a page of the PDF document.
 *
 * @param bounds: Bounds of the text match.
 * @param textStartIndex: starting index of the text match.
 */
public class PageMatchBounds(public val bounds: List<RectF>, public val textStartIndex: Int)
