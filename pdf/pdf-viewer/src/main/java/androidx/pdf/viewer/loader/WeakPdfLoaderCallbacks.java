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
import androidx.annotation.VisibleForTesting;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.TileBoard.TileInfo;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A delegating implementation of PdfLoaderCallbacks that allows the delegate
 * to be garbage collected by holding only a weak reference. Once the delegate
 * is garbage collected, every call is a no-op.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WeakPdfLoaderCallbacks implements PdfLoaderCallbacks {

    private static final String TAG = WeakPdfLoaderCallbacks.class.getSimpleName();

    /** Take some callbacks and hold them with only a weak reference. */
    @NonNull
    public static WeakPdfLoaderCallbacks wrap(@NonNull PdfLoaderCallbacks delegate) {
        if (delegate instanceof WeakPdfLoaderCallbacks) {
            return (WeakPdfLoaderCallbacks) delegate;
        }
        return new WeakPdfLoaderCallbacks(delegate);
    }

    private final WeakReference<PdfLoaderCallbacks> mDelegate;

    @VisibleForTesting
    protected WeakPdfLoaderCallbacks(@NonNull PdfLoaderCallbacks delegate) {
        this.mDelegate = new WeakReference<PdfLoaderCallbacks>(delegate);
    }

    @Override
    public void requestPassword(boolean incorrect) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.requestPassword(incorrect);
        }
    }

    @Override
    public void documentLoaded(int numPages, @NonNull DisplayData data) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.documentLoaded(numPages, data);
        }
    }

    @Override
    public void documentNotLoaded(@NonNull PdfStatus status) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.documentNotLoaded(status);
        }
    }

    @Override
    public void pageBroken(int page) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.pageBroken(page);
        }
    }

    @Override
    public void setPageDimensions(int pageNum, @NonNull Dimensions dimensions) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageDimensions(pageNum, dimensions);
        }
    }

    @Override
    public void setPageBitmap(int pageNum, @NonNull Bitmap bitmap) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageBitmap(pageNum, bitmap);
        }
    }

    @Override
    public void setTileBitmap(int pageNum, @NonNull TileInfo tileInfo, @NonNull Bitmap bitmap) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setTileBitmap(pageNum, tileInfo, bitmap);
        }
    }

    @Override
    public void setPageText(int pageNum, @NonNull String text) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageText(pageNum, text);
        }
    }

    @Override
    public void setSearchResults(@NonNull String query, int pageNum, @NonNull MatchRects matches) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setSearchResults(query, pageNum, matches);
        }
    }

    @Override
    public void setSelection(int pageNum, @NonNull PageSelection selection) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setSelection(pageNum, selection);
        }
    }

    @Override
    public void setPageUrlLinks(int pageNum, @NonNull LinkRects links) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageUrlLinks(pageNum, links);
        }
    }

    @Override
    public void setPageGotoLinks(int pageNum, @NonNull List<GotoLink> links) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageGotoLinks(pageNum, links);
        }
    }

    @Override
    public void setInvalidRects(int pageNum, @NonNull List<Rect> invalidRects) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setInvalidRects(pageNum, invalidRects);
        }
    }

    private PdfLoaderCallbacks getCallbacks() {
        PdfLoaderCallbacks callbacks = mDelegate.get();
        return callbacks;
    }
}
