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

import androidx.annotation.RestrictTo;
import androidx.pdf.aidl.Dimensions;
import androidx.pdf.aidl.LinkRects;
import androidx.pdf.aidl.MatchRects;
import androidx.pdf.aidl.PageSelection;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.util.TileBoard.TileInfo;

import java.io.FileOutputStream;
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
    void documentLoaded(int numPages);

    /** Called if the Document can't be loaded. */
    void documentNotLoaded(PdfStatus status);

    /** Called if pdfClient chokes on one page of the document. */
    void pageBroken(int page);

    /** This is called in response to a call to loadPageDimensions. */
    void setPageDimensions(int pageNum, Dimensions dimensions);

    /** This is called in response to a call to loadPageBitmap. */
    void setPageBitmap(int pageNum, Bitmap bitmap);

    /** This is called in response to a call to loadTileBitmap. */
    void setTileBitmap(int pageNum, TileInfo tileInfo, Bitmap bitmap);

    /** This is called in response to a call to loadPageText. */
    void setPageText(int pageNum, String text);

    /** This is called in response to a call to searchPageText. */
    void setSearchResults(String query, int pageNum, MatchRects matches);

    /** This is called in response to selectPageText. */
    void setSelection(int pageNum, PageSelection selection);

    /** This is called in response to getPageUrlLinks. */
    void setPageUrlLinks(int pageNum, LinkRects result);

    /**
     * This is called in response to a call to {@link PdfLoader#cloneWithoutSecurity}.
     *
     * @param result is true if the document was successfully cloned.
     */
    void documentCloned(boolean result);

    /**
     * This is called in response to a call to {@link PdfLoader#saveAs(FileOutputStream)}.
     *
     * @param result is true if the document was successfully saved.
     */
    void documentSavedAs(boolean result);

    /**
     * Called in response to form editing operations in {@link PdfLoader} to inform implementations
     * that portions of the page bitmap that have been invalidated.
     */
    void setInvalidRects(int pageNum, List<Rect> invalidRects);
}
