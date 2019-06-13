/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.app.PendingIntent;
import android.os.Parcelable;
import android.text.Spanned;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VersionedParcelize(allowSerialization = true, ignoreParcelables = true,
        factory = SliceItemHolder.SliceItemPool.class)
@RequiresApi(19)
public class SliceItemHolder implements VersionedParcelable {

    public static final Object sSerializeLock = new Object();
    public static HolderHandler sHandler;

    // VersionedParcelable fields for custom serialization.
    @ParcelField(value = 1, defaultValue = "null")
    public VersionedParcelable mVersionedParcelable = null;
    @ParcelField(value = 2, defaultValue = "null")
    Parcelable mParcelable = null;
    @NonParcelField
    Object mCallback;
    @ParcelField(value = 3, defaultValue = "null")
    String mStr = null;
    @ParcelField(value = 4, defaultValue = "0")
    int mInt = 0;
    @ParcelField(value = 5, defaultValue = "0")
    long mLong = 0;

    @NonParcelField
    private SliceItemPool mPool;

    SliceItemHolder(SliceItemPool pool) {
        mPool = pool;
    }

    /**
     * Send this back to the pool it came from (if it came from one).
     */
    public void release() {
        if (mPool != null) {
            mPool.release(this);
        }
    }

    @SuppressWarnings("unchecked")
    public SliceItemHolder(String format, Object mObj, boolean isStream) {
        switch (format) {
            case FORMAT_ACTION:
                if (((Pair<Object, Slice>) mObj).first instanceof PendingIntent) {
                    mParcelable = (Parcelable) ((Pair<Object, Slice>) mObj).first;
                } else if (!isStream) {
                    throw new IllegalArgumentException("Cannot write callback to parcel");
                }
                mVersionedParcelable = ((Pair<Object, Slice>) mObj).second;
                break;
            case FORMAT_IMAGE:
            case FORMAT_SLICE:
                mVersionedParcelable = (VersionedParcelable) mObj;
                break;
            case FORMAT_REMOTE_INPUT:
                mParcelable = (Parcelable) mObj;
                break;
            case FORMAT_TEXT:
                mStr = mObj instanceof Spanned ? HtmlCompat.toHtml((Spanned) mObj,
                        HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) : (String) mObj;
                break;
            case FORMAT_INT:
                mInt = (Integer) mObj;
                break;
            case FORMAT_LONG:
                mLong = (Long) mObj;
                break;
        }
        if (SliceItemHolder.sHandler != null) {
            SliceItemHolder.sHandler.handle(this, format);
        }
    }

    /**
     * Gets object that should be held by SliceItem.
     */
    public Object getObj(String format) {
        if (SliceItemHolder.sHandler != null) {
            SliceItemHolder.sHandler.handle(this, format);
        }
        switch (format) {
            case FORMAT_ACTION:
                if (mParcelable == null && mVersionedParcelable == null) return null;
                return new Pair<>(mParcelable != null ? mParcelable : mCallback,
                        (Slice) mVersionedParcelable);
            case FORMAT_IMAGE:
            case FORMAT_SLICE:
                return mVersionedParcelable;
            case FORMAT_REMOTE_INPUT:
                return mParcelable;
            case FORMAT_TEXT:
                if (mStr == null || mStr.length() == 0) {
                    return "";
                }
                return HtmlCompat.fromHtml(mStr, HtmlCompat.FROM_HTML_MODE_LEGACY);
            case FORMAT_INT:
                return mInt;
            case FORMAT_LONG:
                return mLong;
            default:
                throw new IllegalArgumentException("Unrecognized format " + format);
        }
    }

    /**
     * Callback that gets to participate in the serialization process for SliceItems.
     */
    public interface HolderHandler {
        void handle(SliceItemHolder holder, String format);
    }

    /**
     * Simple object pool for slice items.
     */
    public static class SliceItemPool {

        private final ArrayList<SliceItemHolder> mCached = new ArrayList<>();

        /**
         * Acquire an item from the pool.
         */
        public SliceItemHolder get() {
            if (mCached.size() > 0) {
                return mCached.remove(mCached.size() - 1);
            }
            return new SliceItemHolder(this);
        }

        /**
         * Send an object back to the pool.
         */
        public void release(SliceItemHolder sliceItemHolder) {
            sliceItemHolder.mParcelable = null;
            sliceItemHolder.mCallback = null;
            sliceItemHolder.mVersionedParcelable = null;
            sliceItemHolder.mInt = 0;
            sliceItemHolder.mLong = 0;
            sliceItemHolder.mStr = null;
            mCached.add(sliceItemHolder);
        }
    }
}
