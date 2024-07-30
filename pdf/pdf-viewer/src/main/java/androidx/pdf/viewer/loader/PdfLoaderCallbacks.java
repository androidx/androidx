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

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.TileBoard.TileInfo;

import java.util.List;

/**
 * Callback interface - should be implemented by a client of PdfLoader, so that
 * they can be notified when PdfLoader has loaded the requested data from the
 * PDF document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PdfLoaderCallbacks {

    /**
     * This is called if the PDF is password protected, to request a password.
     *
     * @param incorrect True if an incorrect password was just provided.
     */
    void requestPassword(boolean incorrect);

    /** This when the PDF is first successfully loaded by PdfLoader. */
    void documentLoaded(int numPages, @NonNull DisplayData data);

    /** Called if the Document can't be loaded. */
    void documentNotLoaded(@NonNull PdfStatus status);

    /** Called if pdfClient chokes on one page of the document. */
    void pageBroken(int page);

    /** This is called in response to a call to loadPageDimensions. */
    void setPageDimensions(int pageNum, @NonNull Dimensions dimensions);

    /** This is called in response to a call to loadPageBitmap. */
    void setPageBitmap(int pageNum, @NonNull Bitmap bitmap);

    /** This is called in response to a call to loadTileBitmap. */
    void setTileBitmap(int pageNum, @NonNull TileInfo tileInfo, @NonNull Bitmap bitmap);

    /** This is called in response to a call to loadPageText. */
    void setPageText(int pageNum, @NonNull String text);

    /** This is called in response to a call to searchPageText. */
    void setSearchResults(@NonNull String query, int pageNum, @NonNull MatchRects matches);

    /** This is called in response to selectPageText. */
    void setSelection(int pageNum, @NonNull PageSelection selection);

    /** This is called in response to getPageUrlLinks. */
    void setPageUrlLinks(int pageNum, @NonNull LinkRects result);

    /** This is called in response to getPageGotoLinks. */
    void setPageGotoLinks(int pageNum, @NonNull List<GotoLink> links);

    /**
     * Called in response to form editing operations in {@link PdfLoader} to inform implementations
     * that portions of the page bitmap that have been invalidated.
     */
    void setInvalidRects(int pageNum, @NonNull List<Rect> invalidRects);
}
