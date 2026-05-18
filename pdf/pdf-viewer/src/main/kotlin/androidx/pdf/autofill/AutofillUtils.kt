/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.autofill

/** Returns a packed virtual ID for a form widget. */
internal fun getVirtualFormWidgetId(pageNumber: Int, widgetIndex: Int): Int {
    return (pageNumber shl 16) or (widgetIndex and 0xFFFF)
}

/** Returns the page number from the packed [virtualId]. */
internal fun getPageNumber(virtualId: Int): Int {
    return virtualId ushr 16
}

/** Returns the widget index from the packed [virtualId]. */
internal fun getWidgetIndex(virtualId: Int): Int {
    return virtualId and 0xFFFF
}
