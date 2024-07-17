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

package androidx.pdf

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment

/**
 * A Fragment that renders a PDF document.
 *
 * <p>A [PdfViewerFragment] that can display paginated PDFs. The viewer includes a FAB for
 * annotation support and a search menu. Each page is rendered in its own View. Upon creation, this
 * fragment displays a loading spinner.
 *
 * <p>Rendering is done in 2 passes:
 * <ol>
 * <li>Layout: Request the page data, get the dimensions and set them as measure for the image view.
 * <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * @see documentUri
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class PdfViewerFragment : Fragment() {

    /**
     * The URI of the PDF document to display defaulting to `null`.
     *
     * When this property is set, the fragment begins loading the PDF. A loading spinner is
     * displayed until the document is fully loaded. If an error occurs during loading, an error
     * message is displayed, and the detailed exception can be captured by overriding
     * [onLoadDocumentError].
     */
    var documentUri: Uri? = null

    /**
     * Controls the visibility of the "find in file" menu. Defaults to `false`.
     *
     * Set to `true` to display the menu, or `false` to hide it.
     */
    var isTextSearchActive: Boolean = false

    /**
     * Callback invoked when an error occurs while loading the PDF document.
     *
     * Override this method to handle document loading errors. The default implementation displays a
     * generic error message in the loading view.
     *
     * @param throwable [Throwable] that occurred during document loading.
     */
    @Suppress("UNUSED_PARAMETER") fun onLoadDocumentError(throwable: Throwable) {}
}
