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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.ListOfList;
import androidx.pdf.util.Preconditions;

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
}
