/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.contentpager.content;

import static androidx.core.util.Preconditions.checkArgument;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Encapsulates information related to calling {@link ContentResolver#query},
 * including the logic determining the best query method to call.
 */
public final class Query {

    private static final boolean DEBUG = true;
    private static final String TAG = "Query";

    private final Uri mUri;
    private final @Nullable String[] mProjection;
    private final Bundle mQueryArgs;

    private final int mId;
    private final int mOffset;
    private final int mLimit;

    private final CancellationSignal mCancellationSignal;
    private final ContentPager.ContentCallback mCallback;

    Query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @NonNull Bundle args,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull ContentPager.ContentCallback callback) {

        checkArgument(uri != null);
        checkArgument(args != null);
        checkArgument(callback != null);

        this.mUri = uri;
        this.mProjection = projection;
        this.mQueryArgs = args;
        this.mCancellationSignal = cancellationSignal;
        this.mCallback = callback;

        this.mOffset = args.getInt(ContentPager.QUERY_ARG_OFFSET, -1);
        this.mLimit = args.getInt(ContentPager.QUERY_ARG_LIMIT, -1);

        // NOTE: We omit mProjection and other details from ID. If a client wishes
        // to request a page with a different mProjection or sorting, they should
        // wait for first request to finish. Same goes for mCallback.
        this.mId = uri.hashCode() << 16 | (mOffset | (mLimit << 8));

        checkArgument(mOffset >= 0);  // mOffset must be set, mLimit is optional.
    }

    /**
     * @return the id for this query. Derived from Uri as well as paging arguments.
     */
    public int getId() {
        return mId;
    }

    /**
     * @return the Uri.
     */
    public @NonNull Uri getUri() {
        return mUri;
    }

    /**
     * @return the offset.
     */
    public int getOffset() {
        return mOffset;
    }

    /**
     * @return the limit.
     */
    public int getLimit() {
        return mLimit;
    }

    @NonNull ContentPager.ContentCallback getCallback() {
        return mCallback;
    }

    @Nullable Cursor run(@NonNull ContentResolver resolver) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return resolver.query(
                    mUri,
                    mProjection,
                    mQueryArgs,
                    mCancellationSignal);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (DEBUG) Log.d(TAG, "Falling back to pre-O query method.");
            return resolver.query(
                    mUri,
                    mProjection,
                    null,
                    null,
                    null,
                    mCancellationSignal);
        }

        if (DEBUG) Log.d(TAG, "Falling back to pre-jellybean query method.");
        return resolver.query(
                mUri,
                mProjection,
                null,
                null,
                null);
    }

    void cancel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (mCancellationSignal != null && !mCancellationSignal.isCanceled()) {
                if (DEBUG) {
                    Log.d(TAG, "Attemping to cancel query provider processings: " + this);
                }
                mCancellationSignal.cancel();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Query)) {
            return false;
        }

        Query other = (Query) obj;

        return mId == other.mId
                && mUri.equals(other.mUri)
                && mOffset == other.mOffset
                && mLimit == other.mLimit;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public String toString() {
        return "Query{"
                + "id:" + mId
                + " uri:" + mUri
                + " projection:" + Arrays.toString(mProjection)
                + " offset:" + mOffset
                + " limit:" + mLimit
                + " cancellationSignal:" + mCancellationSignal
                + " callback:" + mCallback
                + "}";
    }
}
