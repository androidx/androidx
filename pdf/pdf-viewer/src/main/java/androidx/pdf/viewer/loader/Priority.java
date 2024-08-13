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

package androidx.pdf.viewer.loader;

import androidx.annotation.RestrictTo;

/** Task priorities, from highest to lowest priority. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
enum Priority {
    INITIALIZE,
    // This is above Dimensions and Bitmap because it will never execute unless we have unsaved form
    // filling state to restore. It is more efficient to re-execute these operations on the document
    // before the initial render to avoid immediately re-rendering the same bitmaps.
    RESTORE_FORM_FILLING_STATE,
    DIMENSIONS,
    BITMAP,
    CLONE_PDF,
    BITMAP_TILE,
    SELECT,
    SEARCH,
    TEXT,
    LINKS,
    FEATURES,
    COMMENT_ANCHORS,
    PAGE_CLICK,
    SET_FORM_FIELD_VALUE,
    FORM_WIDGET_INFO,
    RELEASE
}
