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

package androidx.pdf.viewer;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.select.SelectionModel;
import androidx.pdf.viewer.loader.PdfLoader;

/**
 * Selection model for pdfs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfSelectionModel extends SelectionModel<PageSelection> {

    private final PdfLoader mPdfLoader;

    public PdfSelectionModel(@NonNull PdfLoader pdfLoader) {
        this.mPdfLoader = pdfLoader;
    }

    /** Return the page the selection is on. */
    public int getPage() {
        PageSelection value = mSelection.get();
        return (value != null) ? value.getPage() : -1;
    }

    @NonNull
    @Override
    public String getText() {
        PageSelection value = mSelection.get();
        return (value != null) ? value.getText() : "";
    }

    /**
     * Asynchronous update - the exact selection is not yet known, we need to make an async call to
     * pdfClient to find out the exact selection. This will eventually cause
     * {@link #setSelection} to be called.
     */
    @Override
    public void updateSelectionAsync(@NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop) {
        if (mPdfLoader != null) {
            int page = Math.max(0, getPage());
            mPdfLoader.selectPageText(page, start, stop);
        }
    }
}
