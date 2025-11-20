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

package androidx.pdf.view

import android.view.View
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import com.google.common.truth.Truth.assertThat

/**
 * Performs a [ViewAssertion] that checks [firstVisiblePage] and [visiblePages] match the
 * corresponding properties of a [PdfView]
 */
internal fun ViewInteraction.checkPagesAreVisible(
    firstVisiblePage: Int,
    visiblePages: Int? = null,
) = this.check(PdfViewPagesAreVisible(firstVisiblePage, visiblePages))

/**
 * [ViewAssertion] which checks that [PdfView] has the expected [PdfView.firstVisiblePage] and
 * [PdfView.visiblePagesCount] values
 */
private class PdfViewPagesAreVisible(val firstVisiblePage: Int, val visiblePages: Int? = null) :
    ViewAssertion {
    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        // ViewAssertion contract requires either view or noViewFoundException to be non-null
        view ?: throw requireNotNull(noViewFoundException)
        assertThat(view).isInstanceOf(PdfView::class.java)
        // We just checked for this, but this makes smartcasts work nicely
        require(view is PdfView)
        assertThat(view.firstVisiblePage).isEqualTo(firstVisiblePage)
        if (visiblePages != null) {
            assertThat(view.visiblePagesCount).isEqualTo(visiblePages)
        }
    }
}
