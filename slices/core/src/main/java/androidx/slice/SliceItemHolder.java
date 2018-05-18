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

import androidx.annotation.RestrictTo;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VersionedParcelize(allowSerialization = true, ignoreParcelables = true)
public class SliceItemHolder implements VersionedParcelable {

    // VersionedParcelable fields for custom serialization.
    @ParcelField(1)
    VersionedParcelable mVersionedParcelable;
    @ParcelField(2)
    Parcelable mParcelable;
    @ParcelField(3)
    String mStr;
    @ParcelField(4)
    int mInt;
    @ParcelField(5)
    long mLong;

    public SliceItemHolder() {
    }

    public SliceItemHolder(String format, Object mObj, boolean isStream) {
        switch (format) {
            case FORMAT_ACTION:
                if (((Pair<Object, Slice>) mObj).first instanceof PendingIntent) {
                    if (isStream) {
                        throw new IllegalArgumentException("Cannot write PendingIntent to stream");
                    } else {
                        mParcelable = (Parcelable) ((Pair<Object, Slice>) mObj).first;
                    }
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
                if (isStream) {
                    throw new IllegalArgumentException("Cannot write RemoteInput to stream");
                }
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
    }

    /**
     * Gets object that should be held by SliceItem.
     */
    public Object getObj(String format) {
        switch (format) {
            case FORMAT_ACTION:
                return new Pair<Object, Slice>(mParcelable, (Slice) mVersionedParcelable);
            case FORMAT_IMAGE:
            case FORMAT_SLICE:
                return mVersionedParcelable;
            case FORMAT_REMOTE_INPUT:
                return mParcelable;
            case FORMAT_TEXT:
                return HtmlCompat.fromHtml(mStr, HtmlCompat.FROM_HTML_MODE_LEGACY);
            case FORMAT_INT:
                return mInt;
            case FORMAT_LONG:
                return mLong;
            default:
                throw new IllegalArgumentException("Unrecognized format " + format);
        }
    }
}
