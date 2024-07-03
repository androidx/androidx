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

package androidx.pdf.models;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.ListOfList;
import androidx.pdf.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the bounds of links as a {@code List<List<Rect>>}, where
 * the first {@code List<Rect>} is all of the rectangles needed to bound the
 * first link, and so on. Most links will be surrounded with a single Rect.
 * <p>
 * Internally, data is stored as 1-dimensional Lists, to avoid the overhead of
 * a large amount of single-element lists.
 * <p>
 * Also contains the URL index of each link - so {@link #get} returns the
 * rectangles that bound the link, and {@link #getUrl} returns the URL that is
 * linked to.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
@SuppressLint("BanParcelableUsage")
public class LinkRects extends ListOfList<Rect> implements Parcelable {
    public static final LinkRects NO_LINKS = new LinkRects(Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList());

    public static final Creator<LinkRects> CREATOR = new Creator<LinkRects>() {
        @SuppressWarnings("unchecked")
        @Override
        public LinkRects createFromParcel(Parcel parcel) {
            return new LinkRects(parcel.readArrayList(Rect.class.getClassLoader()),
                    parcel.readArrayList(Integer.class.getClassLoader()),
                    parcel.readArrayList(String.class.getClassLoader()));
        }

        @Override
        public LinkRects[] newArray(int size) {
            return new LinkRects[size];
        }
    };

    private final List<Rect> mRects;
    private final List<Integer> mLinkToRect;
    private final List<String> mUrls;

    public LinkRects(@NonNull List<Rect> rects, @NonNull List<Integer> linkToRect,
            @NonNull List<String> urls) {
        super(rects, linkToRect);
        this.mRects = Preconditions.checkNotNull(rects);
        this.mLinkToRect = Preconditions.checkNotNull(linkToRect);
        this.mUrls = Preconditions.checkNotNull(urls);
    }

    /** Return the URL corresponding to the given link. */
    @NonNull
    public String getUrl(int link) {
        return mUrls.get(link);
    }

    /** Return the URL corresponding to the given point. */
    @Nullable
    public String getUrlAtPoint(int x, int y) {
        for (int rect = 0; rect < mRects.size(); rect++) {
            if (mRects.get(rect).contains(x, y)) {
                for (int link = 1; link <= mLinkToRect.size(); link++) {
                    if (indexToFirstValue(link) > rect) {
                        return mUrls.get(link - 1);
                    }
                }
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return size() + " links";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeList(mRects);
        parcel.writeList(mLinkToRect);
        parcel.writeList(mUrls);
    }

    /**
     * Flattens the list of PdfPageLinkContent objects and converts to a LinkRects objects.
     * <p>As an example, in case there are 2 weblinks on the page of the document with the 1st link
     * overflowing to the next line, {@code List<PdfPageLinkContent>} would have the following
     * values -
     * <pre>
     * List(
     *      PdfPageLinkContent(
     *          bounds = [RectF(l1, t1, r1, b1), RectF(l2, t2, r2, b2)],
     *          url = url1
     *      ),
     *      PdfPageLinkContent(
     *          bounds = [RectF(l3, t3, r3, b3)],
     *          url = url2
     *      ),
     * )
     *
     * Using the method below, we can flatten the {@code List<PdfPageLinkContent>} to the following
     * representation -
     * LinkRects(
     *      mRects=[Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2), Rect(l3, t3, r3, b3)],
     *      mLinkToRect=[0,2],
     *      mUrls=[url1, url2]
     * )
     * </pre>
     */
    @NonNull
    public static LinkRects flattenList(@NonNull List<PdfPageLinkContent> pdfPageLinkContentList) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            List<Rect> rects = new ArrayList<>();
            List<Integer> linkToRect = new ArrayList<>();
            List<String> urls = new ArrayList<>();
            int numRects = 0;
            for (PdfPageLinkContent pdfPageLinkContent : pdfPageLinkContentList) {
                List<RectF> rectFBounds = pdfPageLinkContent.getBounds();
                for (RectF rectF : rectFBounds) {
                    rects.add(new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right,
                            (int) rectF.bottom));
                }
                urls.add(pdfPageLinkContent.getUri().toString());
                linkToRect.add(numRects);
                numRects += pdfPageLinkContent.getBounds().size();
            }

            return new LinkRects(rects, linkToRect, urls);
        }
        throw new UnsupportedOperationException("Operation support above S");
    }

    @NonNull
    public List<Rect> getRects() {
        return mRects;
    }

    @NonNull
    public List<Integer> getLinkToRect() {
        return mLinkToRect;
    }

    @NonNull
    public List<String> getUrls() {
        return mUrls;
    }
}
