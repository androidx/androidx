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
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
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
    public static WeakPdfLoaderCallbacks wrap(PdfLoaderCallbacks delegate) {
        if (delegate instanceof WeakPdfLoaderCallbacks) {
            return (WeakPdfLoaderCallbacks) delegate;
        }
        return new WeakPdfLoaderCallbacks(delegate);
    }

    private final WeakReference<PdfLoaderCallbacks> mDelegate;

    @VisibleForTesting
    protected WeakPdfLoaderCallbacks(PdfLoaderCallbacks delegate) {
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
    public void documentLoaded(int numPages) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.documentLoaded(numPages);
        }
    }

    @Override
    public void documentNotLoaded(PdfStatus status) {
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
    public void setPageDimensions(int pageNum, Dimensions dimensions) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageDimensions(pageNum, dimensions);
        }
    }

    @Override
    public void setPageBitmap(int pageNum, Bitmap bitmap) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageBitmap(pageNum, bitmap);
        }
    }

    @Override
    public void setTileBitmap(int pageNum, TileInfo tileInfo, Bitmap bitmap) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setTileBitmap(pageNum, tileInfo, bitmap);
        }
    }

    @Override
    public void setPageText(int pageNum, String text) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageText(pageNum, text);
        }
    }

    @Override
    public void setSearchResults(String query, int pageNum, MatchRects matches) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setSearchResults(query, pageNum, matches);
        }
    }

    @Override
    public void setSelection(int pageNum, PageSelection selection) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setSelection(pageNum, selection);
        }
    }

    @Override
    public void setPageUrlLinks(int pageNum, LinkRects links) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageUrlLinks(pageNum, links);
        }
    }

    @Override
    public void setPageGotoLinks(int pageNum, List<GotoLink> links) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setPageGotoLinks(pageNum, links);
        }
    }

    @Override
    public void documentCloned(boolean result) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.documentCloned(result);
        }
    }

    @Override
    public void documentSavedAs(boolean result) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.documentSavedAs(result);
        }
    }

    @Override
    public void setInvalidRects(int pageNum, List<Rect> invalidRects) {
        PdfLoaderCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setInvalidRects(pageNum, invalidRects);
        }
    }

    private PdfLoaderCallbacks getCallbacks() {
        PdfLoaderCallbacks callbacks = mDelegate.get();
        if (callbacks == null) {
            Log.w(TAG, "Callbacks have been garbage collected - nothing to do.");
        }
        return callbacks;
    }
}
